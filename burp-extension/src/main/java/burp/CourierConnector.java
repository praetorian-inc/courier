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

package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.controller.ConfigurationController;
import burp.controller.ConnectionController;
import burp.controller.LogController;
import burp.controller.PlannerController;
import burp.controller.WebflowRecorderController;
import burp.handlers.HttpRequestsAndResponsesHandler;
import burp.handlers.IssuesHandler;
import burp.handlers.OrganizerHandler;
import burp.handlers.ProxyHandler;
import burp.model.HttpRequestResponsePair;
import burp.serialization.dto.AuditIssueDto;
import burp.utils.BuildInfo;
import burp.utils.MonitoredHashMap;
import burp.utils.SessionManager;
import burp.view.ConnectionsView;
import burp.view.CourierIcons;
import burp.view.GeneralView;
import burp.view.LoggerView;
import burp.view.OptionsView;
import burp.view.PlannerView;
import burp.view.WebflowRecorderView;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class CourierConnector implements BurpExtension, ExtensionUnloadingHandler {
    private MontoyaApi api;
    private JPanel panel;
    private LogController logger;
    private ConfigurationController configurationController;
    private ConnectionController connectionController;
    private HttpRequestsAndResponsesHandler httpHandler;
    private ProxyHandler proxyHandler;
    private IssuesHandler issuesHandler;
    private OrganizerHandler organizerHandler;
    private WebflowRecorderController webflowRecorderController;
    private PlannerController plannerController;
    private GeneralView.Shell shell;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().registerUnloadingHandler(this);
        api.extension().setName("Courier");

        createUi();
        registerHandlers();
        api.userInterface().registerSuiteTab("Courier", panel);
    }

    private void createUi() {
        shell = GeneralView.createShell(BuildInfo.timestamp());
        panel = shell.root();
        JTabbedPane tabs = shell.tabs();

        LoggerView loggerView = new LoggerView(api, BuildInfo.timestamp());
        logger = loggerView.getLogController();
        JPanel loggerPanel = loggerView.createLoggerPanel();

        OptionsView optionsView = new OptionsView(logger);
        optionsView.setScopeListener(shell.dashboard()::setCapturePolicy);
        configurationController = optionsView.getConfigurationController();

        PlannerView plannerView = new PlannerView(logger);
        plannerController = new PlannerController(plannerView, logger);
        plannerView.setPlannerController(plannerController);

        WebflowRecorderView recorderView = new WebflowRecorderView(logger);
        webflowRecorderController = new WebflowRecorderController(recorderView, logger);
        recorderView.setWebflowRecorderController(webflowRecorderController);

        organizerHandler = new OrganizerHandler(api, configurationController, logger);
        ConnectionsView connectionsView = new ConnectionsView();
        connectionsView.setConnectionStatusListener(shell::setConnectionStatus);
        connectionController = new ConnectionController(
                connectionsView, logger, api, configurationController, new RuntimeConnectionLifecycle());

        shell.dashboard().bindMetrics(
                connectionController::getPendingRecordCount,
                connectionController::getLastSyncDescription);
        JPanel controlPanel = GeneralView.createControlPanel(shell,
                connectionsView.createConnectionsPanel(connectionController),
                optionsView.createOptionsPanel(), loggerPanel);
        tabs.addTab("Control", CourierIcons.control(), controlPanel);
        tabs.addTab("Planner", CourierIcons.planner(), plannerView.createPlannerPanel());
        tabs.addTab("Webflows", CourierIcons.webflows(), recorderView.createWebflowRecorderPanel());

        api.userInterface().registerContextMenuItemsProvider(plannerController.createContextMenuProvider());
        logger.logInfo("Courier extension loaded successfully");
        logger.logInfo("Build timestamp: " + BuildInfo.timestamp());
    }

    private void registerHandlers() {
        httpHandler = new HttpRequestsAndResponsesHandler(configurationController, api, connectionController);
        api.http().registerHttpHandler(httpHandler);

        proxyHandler = new ProxyHandler(configurationController, api, logger, connectionController);
        api.proxy().registerRequestHandler(proxyHandler);
        api.proxy().registerResponseHandler(proxyHandler);

        issuesHandler = new IssuesHandler(configurationController, api, connectionController);
        api.scanner().registerAuditIssueHandler(issuesHandler);
    }

    @Override
    public void extensionUnloaded() {
        if (connectionController != null) {
            connectionController.close();
        }
        if (organizerHandler != null) {
            organizerHandler.close();
        }
        if (plannerController != null) {
            plannerController.close();
        }
        if (webflowRecorderController != null) {
            webflowRecorderController.close();
        }
        if (shell != null) {
            shell.close();
        }
        if (logger != null) {
            logger.logInfo("Courier extension unloaded");
            logger.close();
        }
    }

    private final class RuntimeConnectionLifecycle implements ConnectionController.ConnectionLifecycle {
        @Override
        public void connected(SessionManager sessionManager,
                MonitoredHashMap<Integer, HttpRequestResponsePair> proxyResponses,
                MonitoredHashMap<Integer, HttpRequestResponsePair> httpResponses,
                MonitoredHashMap<String, AuditIssueDto> issues) {
            httpHandler.setRequestResponses(httpResponses);
            proxyHandler.setRequestResponses(proxyResponses);
            issuesHandler.setIssues(issues);
            plannerController.setSessionManager(sessionManager);
            webflowRecorderController.setSessionManager(sessionManager);
            organizerHandler.start(sessionManager);
        }

        @Override
        public void disconnected() {
            if (httpHandler != null) {
                httpHandler.setRequestResponses(null);
            }
            if (proxyHandler != null) {
                proxyHandler.setRequestResponses(null);
            }
            if (issuesHandler != null) {
                issuesHandler.setIssues(null);
            }
            organizerHandler.close();
            plannerController.setSessionManager(null);
            plannerController.stopPolling();
            webflowRecorderController.setSessionManager(null);
            webflowRecorderController.stopRecording();
        }
    }
}
