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

package burp.controller.agentSession;

import burp.controller.LogController;
import burp.serialization.dto.ConversationRequestDto;
import burp.serialization.dto.ConversationResponseDto;
import burp.serialization.dto.MessageResponseDto;
import burp.serialization.dto.MyEndpointResponseDto;
import burp.serialization.util.JsonMapperUtil;
import burp.utils.SessionManager;
import burp.utils.SharedHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AgentSessionController implements AutoCloseable {
    private static final String PLANNER_API_ENDPOINT = "/planner";
    private static final String MY_API_ENDPOINT_MESSAGE = "/my?label=message&key=%23message%23";

    private final LogController logger;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Map<Integer, String> conversationUuids = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lastMessageTimestamp = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> pollingTasks = new ConcurrentHashMap<>();

    private volatile SessionManager sessionManager;
    private volatile ScheduledExecutorService pollingExecutor;

    public AgentSessionController(LogController logger) {
        this(logger, SharedHttpClient.get(), newPollingExecutor());
    }

    AgentSessionController(LogController logger, HttpClient httpClient, ScheduledExecutorService pollingExecutor) {
        this.logger = logger;
        this.objectMapper = JsonMapperUtil.getConfiguredMapper();
        this.httpClient = httpClient;
        this.pollingExecutor = pollingExecutor;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        if (sessionManager == null) {
            stopPolling();
            conversationUuids.clear();
            lastMessageTimestamp.clear();
        }
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public CompletableFuture<ConversationResponseDto> sendConversationRequest(String message, int tabIndex,
            String mode) {
        SessionManager currentSession = sessionManager;
        if (currentSession == null || !currentSession.isEnabled()) {
            return CompletableFuture.failedFuture(new IllegalStateException("SessionManager not configured"));
        }

        return CompletableFuture.supplyAsync(() -> {
            String token = currentSession.getChariotToken(logger);
            if (token == null || token.isEmpty()) {
                throw new IllegalStateException("Failed to obtain Guard token");
            }
            try {
                ConversationRequestDto requestDto = new ConversationRequestDto();
                requestDto.setMessage(message);
                requestDto.setMode(mode == null || mode.isEmpty() ? "query" : mode);
                String conversationUuid = conversationUuids.get(tabIndex);
                if (conversationUuid != null && !conversationUuid.isEmpty()) {
                    requestDto.setConversationId(conversationUuid);
                }

                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(currentSession.getChariotApiEndpoint() + PLANNER_API_ENDPOINT))
                        .POST(HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(requestDto), StandardCharsets.UTF_8))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + token)
                        .timeout(Duration.ofSeconds(60));
                addAccountHeader(builder, currentSession);
                return builder.build();
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to build planner request", exception);
            }
        }).thenCompose(request -> httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .thenApply(response -> parseConversationResponse(response, tabIndex))
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        logger.logError("Error sending conversation request: " + throwable.getMessage());
                    }
                });
    }

    public synchronized void startPollingForUpdates(int tabIndex, ConversationUpdateCallback callback) {
        SessionManager currentSession = sessionManager;
        if (currentSession == null || !currentSession.isEnabled()) {
            logger.logInfo("Polling not started - SessionManager not configured");
            return;
        }
        ensurePollingExecutor();
        pollingTasks.compute(tabIndex, (ignored, existingTask) -> {
            if (existingTask != null && !existingTask.isCancelled() && !existingTask.isDone()) {
                return existingTask;
            }
            logger.logDebug("Starting polling for tab " + tabIndex);
            return pollingExecutor.scheduleWithFixedDelay(
                    () -> pollForConversationUpdates(tabIndex, callback), 2, 2, TimeUnit.SECONDS);
        });
    }

    private ConversationResponseDto parseConversationResponse(HttpResponse<String> response, int tabIndex) {
        logger.logInfo("Guard API response status: " + response.statusCode());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("API request failed with status: " + response.statusCode());
        }
        try {
            ConversationResponseDto responseDto = objectMapper.readValue(response.body(), ConversationResponseDto.class);
            if (responseDto.getConversation() != null && responseDto.getConversation().getUuid() != null) {
                conversationUuids.put(tabIndex, responseDto.getConversation().getUuid());
            }
            lastMessageTimestamp.put(tabIndex, System.currentTimeMillis());
            return responseDto;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse planner response", exception);
        }
    }

    private void pollForConversationUpdates(int tabIndex, ConversationUpdateCallback callback) {
        SessionManager currentSession = sessionManager;
        if (currentSession == null || !currentSession.isEnabled()) {
            cancelPolling(tabIndex);
            return;
        }

        String conversationUuid = conversationUuids.get(tabIndex);
        if (conversationUuid == null || conversationUuid.isEmpty()) {
            return;
        }

        try {
            String token = currentSession.getChariotToken(logger);
            if (token == null || token.isEmpty()) {
                logger.logError("Cannot poll - no valid token");
                return;
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(currentSession.getChariotApiEndpoint() + MY_API_ENDPOINT_MESSAGE
                            + conversationUuid + "&user=true"))
                    .GET()
                    .header("Authorization", "Bearer " + token)
                    .timeout(Duration.ofSeconds(30));
            addAccountHeader(builder, currentSession);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                processMyEndpointResponse(response.body(), tabIndex, conversationUuid, callback);
            } else {
                logger.logError("Polling failed with status: " + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            logger.logError("Error polling for updates: " + exception.getMessage());
        }
    }

    void processMyEndpointResponse(String responseBody, int tabIndex, String conversationUuid,
            ConversationUpdateCallback callback) {
        try {
            MyEndpointResponseDto response = objectMapper.readValue(responseBody, MyEndpointResponseDto.class);
            if (response.getMessages() == null) {
                return;
            }

            long latestTimestamp = lastMessageTimestamp.getOrDefault(tabIndex, 0L);
            for (MessageResponseDto message : response.getMessages()) {
                try {
                    if (message == null || !conversationUuid.equals(message.getConversationId())) {
                        continue;
                    }
                    String timestamp = message.getTimestamp();
                    if (timestamp == null || timestamp.isBlank()) {
                        logger.logError("Skipping Guard message with missing timestamp");
                        continue;
                    }
                    long messageTimestamp = Instant.parse(timestamp).toEpochMilli();
                    if (messageTimestamp <= latestTimestamp) {
                        continue;
                    }
                    latestTimestamp = messageTimestamp;
                    lastMessageTimestamp.put(tabIndex, latestTimestamp);
                    if (callback != null) {
                        callback.onNewMessage(message, tabIndex);
                    }
                } catch (Exception exception) {
                    logger.logError("Skipping invalid Guard message: " + exception.getMessage());
                }
            }
        } catch (Exception exception) {
            logger.logError("Error processing /my endpoint response: " + exception.getMessage());
        }
    }

    public String getConversationUuid(int tabIndex) {
        return conversationUuids.get(tabIndex);
    }

    public void setConversationUuid(int tabIndex, String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            conversationUuids.remove(tabIndex);
        } else {
            conversationUuids.put(tabIndex, uuid);
        }
    }

    public void initializeTimestampTracking(int tabIndex) {
        lastMessageTimestamp.putIfAbsent(tabIndex, 0L);
    }

    public synchronized void stopPolling() {
        pollingTasks.values().forEach(task -> task.cancel(true));
        pollingTasks.clear();
    }

    int activePollingCount() {
        return (int) pollingTasks.values().stream()
                .filter(task -> !task.isCancelled() && !task.isDone())
                .count();
    }

    public void handleConversationResponse(ConversationResponseDto responseDto, int tabIndex,
            ConversationResponseCallback callback) {
        if (responseDto == null) {
            notifyError(callback, "Empty response from planner");
            return;
        }
        if (responseDto.getError() != null && !responseDto.getError().isEmpty()) {
            notifyError(callback, "API Error: " + responseDto.getError());
            return;
        }

        if (responseDto.getConversation() != null && responseDto.getConversation().getUuid() != null) {
            String uuid = responseDto.getConversation().getUuid();
            setConversationUuid(tabIndex, uuid);
            if (callback != null) {
                callback.onConversationCreated(uuid, responseDto.getConversation().getTopic());
            }
        }

        ConversationResponseDto.PlannerResponseDto plannerResponse = responseDto.getResponse();
        if (plannerResponse == null) {
            notifyError(callback, "Planner response did not contain a result");
            return;
        }
        if (plannerResponse.getResponse() != null && !plannerResponse.getResponse().isEmpty() && callback != null) {
            callback.onResponseReceived(plannerResponse.getResponse(), plannerResponse.getConversationId(),
                    plannerResponse.isSuccess());
        }
        if (plannerResponse.isSuccess() && callback != null) {
            startPollingForUpdates(tabIndex, callback.getUpdateCallback());
        }
    }

    private void cancelPolling(int tabIndex) {
        ScheduledFuture<?> task = pollingTasks.remove(tabIndex);
        if (task != null) {
            task.cancel(true);
        }
    }

    private synchronized void ensurePollingExecutor() {
        if (pollingExecutor == null || pollingExecutor.isShutdown()) {
            pollingExecutor = newPollingExecutor();
        }
    }

    private static ScheduledExecutorService newPollingExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Courier-Agent-Poller");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static void addAccountHeader(HttpRequest.Builder builder, SessionManager sessionManager) {
        String account = sessionManager.getSelectedAccount();
        if (account != null && !account.isEmpty()) {
            builder.header("account", account);
        }
    }

    private static void notifyError(ConversationResponseCallback callback, String error) {
        if (callback != null) {
            callback.onError(error);
        }
    }

    @Override
    public synchronized void close() {
        stopPolling();
        ScheduledExecutorService executor = pollingExecutor;
        pollingExecutor = null;
        if (executor != null) {
            executor.shutdownNow();
        }
        sessionManager = null;
        conversationUuids.clear();
        lastMessageTimestamp.clear();
    }

    public interface ConversationUpdateCallback {
        void onNewMessage(MessageResponseDto message, int tabIndex);
    }

    public interface ConversationResponseCallback {
        void onError(String error);
        void onConversationCreated(String conversationUuid, String topic);
        void onResponseReceived(String responseText, String conversationId, boolean success);
        ConversationUpdateCallback getUpdateCallback();
    }
}
