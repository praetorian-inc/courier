/*
 * Copyright Praetorian Security Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package burp.controller;

import burp.api.montoya.MontoyaApi;
import burp.model.HttpRequestResponsePair;
import burp.serialization.dto.AuditIssueDto;
import burp.utils.MonitoredHashMap;
import burp.utils.PendingUploadStore;
import burp.utils.SessionManager;
import burp.view.ConnectionsView;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConnectionController implements AutoCloseable {
    public interface ConnectionLifecycle {
        void connected(SessionManager sessionManager,
                MonitoredHashMap<Integer, HttpRequestResponsePair> proxyResponses,
                MonitoredHashMap<Integer, HttpRequestResponsePair> httpResponses,
                MonitoredHashMap<String, AuditIssueDto> issues);

        void disconnected();
    }

    private final LogController logger;
    private final ConnectionsView view;
    private final MontoyaApi api;
    private final ConfigurationController configuration;
    private final ConnectionLifecycle lifecycle;

    private volatile boolean enabled;
    private volatile SessionManager sessionManager;
    private volatile SwingWorker<SessionManager, Void> connectWorker;
    private MonitoredHashMap<Integer, HttpRequestResponsePair> proxyResponses;
    private MonitoredHashMap<Integer, HttpRequestResponsePair> httpResponses;
    private MonitoredHashMap<String, AuditIssueDto> issues;
    private volatile long lastSuccessfulSyncAt;
    private boolean uploadDisclosureAccepted;

    public ConnectionController(ConnectionsView view, LogController logger, MontoyaApi api,
            ConfigurationController configuration, ConnectionLifecycle lifecycle) {
        this.view = view;
        this.logger = logger;
        this.api = api;
        this.configuration = configuration;
        this.lifecycle = lifecycle;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void handleConnectButtonClick() {
        if (enabled || connectWorker != null) {
            return;
        }

        ConnectionInput input = connectionInput();
        if (input.endpoint().isBlank() || input.keyId().isBlank() || input.keySecret().isBlank()) {
            JOptionPane.showMessageDialog(view.getConnectionPanel(),
                    "Please enter the Guard API endpoint, API key ID, and API key secret.",
                    "Missing Connection Details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!uploadDisclosureAccepted && !view.confirmDataUploadDisclosure()) {
            return;
        }
        uploadDisclosureAccepted = true;
        view.showConnectingState();
        connectWorker = new SwingWorker<>() {
            @Override
            protected SessionManager doInBackground() {
                SessionManager session = new SessionManager(
                        input.endpoint(), input.keyId(), input.keySecret(),
                        encode(input.projectName()), input.targetApplication(), api);
                session.setSelectedAccount(input.account());
                String token = session.getChariotToken(logger);
                if (token == null || token.isEmpty()) {
                    throw new IllegalStateException("Unable to retrieve a Guard token");
                }
                return session;
            }

            @Override
            protected void done() {
                try {
                    completeConnection(get());
                } catch (Exception exception) {
                    logger.logError("Error connecting to Guard: " + rootMessage(exception));
                    disconnect();
                } finally {
                    connectWorker = null;
                }
            }
        };
        connectWorker.execute();
    }

    public void handleDisableButtonClick() {
        disconnect();
    }

    private void completeConnection(SessionManager session) {
        sessionManager = session;
        session.setEnabled(true);
        proxyResponses = new MonitoredHashMap<>(1024, 10, TimeUnit.SECONDS,
                session, logger, "proxy", configuration);
        httpResponses = new MonitoredHashMap<>(1024, 10, TimeUnit.SECONDS,
                session, logger, "http", configuration);
        issues = new MonitoredHashMap<>(1024, 10, TimeUnit.SECONDS,
                session, logger, "issues", configuration);
        proxyResponses.setFailureMerger(HttpRequestResponsePair::mergeFailedBatch);
        httpResponses.setFailureMerger(HttpRequestResponsePair::mergeFailedBatch);
        java.util.function.Consumer<MonitoredHashMap.SyncReason> syncListener =
                ignored -> lastSuccessfulSyncAt = System.currentTimeMillis();
        proxyResponses.setSyncListener(syncListener);
        httpResponses.setSyncListener(syncListener);
        issues.setSyncListener(syncListener);
        lifecycle.connected(session, proxyResponses, httpResponses, issues);
        enabled = true;
        view.showConnectedState();
        logger.logInfo("Connected to Guard");
        PendingUploadStore.retryAll(session, logger);
    }

    private void disconnect() {
        SwingWorker<SessionManager, Void> worker = connectWorker;
        if (worker != null) {
            worker.cancel(true);
            connectWorker = null;
        }

        enabled = false;
        lifecycle.disconnected();
        closeMap(proxyResponses);
        closeMap(httpResponses);
        closeMap(issues);
        proxyResponses = null;
        httpResponses = null;
        issues = null;

        SessionManager session = sessionManager;
        sessionManager = null;
        if (session != null) {
            session.setEnabled(false);
        }
        resetConnectionState();
        logger.logInfo("Connection disabled");
    }

    public void handleFetchAccountsButtonClick() {
        ConnectionsView.Credentials credentials = view.readCredentials();
        if (credentials.endpoint().isEmpty()
                || credentials.keyId().isEmpty() || credentials.keySecret().isEmpty()) {
            JOptionPane.showMessageDialog(view.getConnectionPanel(),
                    "Please enter the Guard API endpoint, API key ID, and API key secret first.",
                    "Missing Credentials", JOptionPane.WARNING_MESSAGE);
            return;
        }

        view.showFetchingAccounts(true);
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                SessionManager temporarySession = new SessionManager(
                        credentials.endpoint(), credentials.keyId(), credentials.keySecret(),
                        "", "", api);
                return temporarySession.fetchAccounts(logger);
            }

            @Override
            protected void done() {
                try {
                    populateAccounts(get());
                } catch (Exception exception) {
                    logger.logError("Error fetching accounts: " + rootMessage(exception));
                    JOptionPane.showMessageDialog(view.getConnectionPanel(),
                            "Failed to fetch accounts: " + rootMessage(exception),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    view.showFetchingAccounts(false);
                }
            }
        }.execute();
    }

    private void populateAccounts(List<String> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            view.setAccounts(List.of());
            JOptionPane.showMessageDialog(view.getConnectionPanel(),
                    "No accessible accounts found. Please verify your API credentials.",
                    "No Accounts", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        view.setAccounts(accounts);
        logger.logInfo("Account selector populated with " + accounts.size() + " accounts");
    }

    public int getPendingRecordCount() {
        return sizeOf(proxyResponses) + sizeOf(httpResponses) + sizeOf(issues);
    }

    public String getLastSyncDescription() {
        if (lastSuccessfulSyncAt == 0) {
            return "Never";
        }
        long seconds = Math.max(0, (System.currentTimeMillis() - lastSuccessfulSyncAt) / 1000);
        if (seconds < 60) {
            return seconds + "s ago";
        }
        long minutes = seconds / 60;
        return minutes < 60 ? minutes + "m ago" : (minutes / 60) + "h ago";
    }

    private static int sizeOf(java.util.Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }

    public void configureBurpScope() {
        JOptionPane.showMessageDialog(view.getConnectionPanel(),
                "To configure Burp's built-in scope filtering:\n\n"
                        + "1. Go to Logger tab\n2. Click on 'Filter' button\n"
                        + "3. Check/uncheck 'Show only in-scope items'",
                "Burp Logger Scope Configuration", JOptionPane.INFORMATION_MESSAGE);
    }

    private ConnectionInput connectionInput() {
        ConnectionsView.Credentials credentials = view.readCredentials();
        return new ConnectionInput(credentials.endpoint(), credentials.keyId(), credentials.keySecret(),
                credentials.projectName(), credentials.targetApplication(), credentials.account());
    }

    private void resetConnectionState() {
        if (SwingUtilities.isEventDispatchThread()) {
            view.showDisconnectedState();
        } else {
            SwingUtilities.invokeLater(view::showDisconnectedState);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String rootMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    private static void closeMap(AutoCloseable map) {
        if (map == null) {
            return;
        }
        try {
            map.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void close() {
        disconnect();
    }

    private record ConnectionInput(String endpoint, String keyId, String keySecret,
            String projectName, String targetApplication, String account) {
    }
}
