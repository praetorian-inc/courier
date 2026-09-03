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

import burp.model.HttpRequestResponsePair;
import burp.model.ChatMessage;
import burp.model.Conversation;
import burp.view.PlannerChatView;
import burp.view.PlannerView;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import burp.controller.agentSession.AgentSessionController;
import burp.controller.planner.ConversationManager;
import burp.controller.planner.PlannerChatPresenter;
import burp.controller.planner.PlannerContextMenuProvider;
import burp.controller.planner.PlannerMessageComposer;
import burp.controller.planner.PlannerPreviewPresenter;
import burp.controller.planner.PlannerRequestCoordinator;
import burp.utils.SafeLogFormatter;
import burp.utils.SessionManager;

/**
 * Controller for handling planner-related UI actions and business logic.
 * This class manages planner requests and chat functionality.
 */
public class PlannerController {

    private final LogController logger;
    private final PlannerView plannerView;
    private final ConversationManager conversations;
    private final Map<Integer, PlannerChatView> conversationChatViews;
    private final AgentSessionController agentSessionController;
    private final PlannerPreviewPresenter previewPresenter;
    private final PlannerRequestCoordinator requestCoordinator;
    private final PlannerChatPresenter chatPresenter;
    private final PlannerMessageComposer messageComposer;
    
    public PlannerController(PlannerView plannerView, LogController logger) {
        this.logger = logger;
        this.plannerView = plannerView;
        this.conversations = new ConversationManager();
        this.conversationChatViews = new ConcurrentHashMap<>();
        this.agentSessionController = new AgentSessionController(logger);
        this.previewPresenter = new PlannerPreviewPresenter(plannerView, logger);
        this.chatPresenter = new PlannerChatPresenter(conversations, conversationChatViews, logger);
        this.messageComposer = new PlannerMessageComposer(conversations, logger);
        this.requestCoordinator = new PlannerRequestCoordinator(
                plannerView, logger, previewPresenter);
        initializeChat();
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.agentSessionController.setSessionManager(sessionManager);
    }
    
    /**
     * Initialize chat with welcome message
     */
    private void initializeChat() {
        logger.logInfo("Planner chat initialized");
    }
    
    /**
     * Register a chat area for a conversation tab
     */
    public void registerChatView(int tabIndex, PlannerChatView chatView) {
        conversationChatViews.put(tabIndex, chatView);
        
        // Initialize timestamp tracking for this tab in AgentSessionController
        agentSessionController.initializeTimestampTracking(tabIndex);
        
        logger.logDebug("Registered chat area for tab " + tabIndex);
    }
    
    /**
     * Get the current tab index from the view
     */
    public int getCurrentTabIndex() {
        return plannerView.getCurrentTabIndex();
    }
    
    /**
     * Create a new conversation tab
     */
    public void createNewConversationTab() {
        int tabIndex = conversations.createConversation();
        PlannerChatView newChatView = plannerView.createNewConversationTab("Chat " + (tabIndex + 1));
        conversationChatViews.put(tabIndex, newChatView);
        agentSessionController.initializeTimestampTracking(tabIndex);
        plannerView.setCurrentTabIndex(tabIndex);
        logger.logInfo("New conversation created");
    }
    
    /**
     * Handle creating a new conversation
     */
    public void handleNewConversation() {
        createNewConversationTab();
    }

    /**
     * Handles sending a chat message
     */
    public void handleSendMessage() {
        if (plannerView.getMessageInputField() == null) {
            logger.logError("Message input field is null");
            return;
        }

        String messageContent = plannerView.getMessageInputField().getText().trim();
        if (messageContent.isEmpty()) {
            logger.logDebug("Empty message content, not sending");
            return;
        }

        // Get current tab index
        int currentTabIndex = getCurrentTabIndex();
        
        chatPresenter.add(
                new ChatMessage(messageContent, ChatMessage.MessageType.USER, "You"), currentTabIndex);

        SwingUtilities.invokeLater(() -> plannerView.getMessageInputField().setText(""));

        String mode = getSelectedMode();
        List<HttpRequestResponsePair> selectedRequests = plannerView.getSelectedRequests();
        processUserMessage(messageContent, currentTabIndex, mode, selectedRequests);

        logger.logInfo("Chat message sent to tab " + currentTabIndex
                + " (" + SafeLogFormatter.length(messageContent) + " characters)");
    }
    
    /**
     * Get the currently selected conversation mode from the view
     */
    public String getSelectedMode() {
        return plannerView.getSelectedMode();
    }

    /**
     * Process user message and send to Guard API
     */
    private void processUserMessage(String userMessage, int tabIndex, String mode,
            List<HttpRequestResponsePair> selectedRequests) {
        SessionManager sessionManager = agentSessionController.getSessionManager();
        if (sessionManager == null || !sessionManager.isEnabled()) {
            addSystemMessage("SessionManager not configured. Message: " + userMessage, tabIndex);
            return;
        }

        chatPresenter.showGuardPending(tabIndex);
        PlannerMessageComposer.ComposedMessage composedMessage =
                messageComposer.compose(userMessage, tabIndex, mode, selectedRequests);
        sendConversationRequest(composedMessage, tabIndex, mode);
    }

    /**
     * Send conversation request to Guard API
     */
    private void sendConversationRequest(PlannerMessageComposer.ComposedMessage message,
            int tabIndex, String mode) {
        agentSessionController.sendConversationRequest(message.outboundMessage(), tabIndex, mode)
                .thenAccept(responseDto -> {
                    conversations.markRequestsAttached(tabIndex, message.attachedRequests());
                    SwingUtilities.invokeLater(() -> agentSessionController.handleConversationResponse(
                            responseDto, tabIndex, createConversationResponseCallback(tabIndex)));
                })
                .exceptionally(throwable -> {
                    conversations.releaseReservedRequests(tabIndex, message.attachedRequests());
                    SwingUtilities.invokeLater(() -> chatPresenter.failPending(
                            tabIndex, "Error: " + throwable.getMessage()));
                    return null;
                });
    }

    /**
     * Create callback for conversation responses from AgentSessionController
     */
    AgentSessionController.ConversationResponseCallback createConversationResponseCallback(int tabIndex) {
        return new AgentSessionController.ConversationResponseCallback() {
            @Override
            public void onError(String error) {
                chatPresenter.failPending(tabIndex, error);
            }

            @Override
            public void onConversationCreated(String conversationUuid, String topic) {
                Conversation conversation = conversations.getConversation(tabIndex);
                if (conversation != null) {
                    conversation.setUuid(conversationUuid);
                    conversation.setKey("#conversation#" + conversationUuid);
                    if (topic != null) {
                        conversation.setTopic(topic);
                    }
                }
            }

            @Override
            public void onResponseReceived(String responseText, String conversationId, boolean success) {
                chatPresenter.handleInitialResponse(responseText, conversationId, success, tabIndex);
            }

            @Override
            public AgentSessionController.ConversationUpdateCallback getUpdateCallback() {
                return createConversationUpdateCallback();
            }
        };
    }

    public void addSystemMessage(String content) {
        chatPresenter.addSystem(content, getCurrentTabIndex());
    }

    private void addSystemMessage(String content, int tabIndex) {
        chatPresenter.addSystem(content, tabIndex);
    }

    public void addErrorMessage(String content) {
        chatPresenter.addError(content, getCurrentTabIndex());
    }

    public void addPlannerRequest(HttpRequestResponsePair request) {
        requestCoordinator.add(request);
    }

    public void addHttpRequest(HttpRequest request, long timestamp) {
        requestCoordinator.add(request, timestamp);
    }

    public void addHttpRequestResponse(HttpRequest request, HttpResponse response, long timestamp) {
        requestCoordinator.add(request, response, timestamp);
    }

    public void handleRequestSelection(HttpRequestResponsePair selectedRequest) {
        requestCoordinator.select(selectedRequest);
    }

    public void handleRequestDeselection() {
        requestCoordinator.deselect();
    }


    /**
     * Clear all chat messages
     */
    public void clearChat() {
        chatPresenter.clear(getCurrentTabIndex());
    }

    public void removeRequest(int index) {
        requestCoordinator.remove(index);
    }

    public void clearRequests() {
        requestCoordinator.clear();
    }

    /**
     * Get conversation by tab index
     */
    public Conversation getConversation(int tabIndex) {
        return conversations.getConversation(tabIndex);
    }
    
    /**
     * Create callback for conversation updates from AgentSessionController
     */
    private AgentSessionController.ConversationUpdateCallback createConversationUpdateCallback() {
        return chatPresenter::addApiMessage;
    }


    /**
     * Handle clear requests button click
     */
    public void handleClearRequests() {
        clearRequests();
    }

    /**
     * Handle clear chat button click
     */
    public void handleClearChat() {
        clearChat();
    }

    /**
     * Handle view mode change (Pretty, Raw, Hex)
     */
    public void handleViewModeChange(String newMode) {
        previewPresenter.setViewMode(newMode);
    }

    /**
     * Create and return a context menu provider for sending requests to planner
     */
    public ContextMenuItemsProvider createContextMenuProvider() {
        return new PlannerContextMenuProvider(requestCoordinator, logger);
    }

    public void stopPolling() {
        agentSessionController.stopPolling();
    }

    public void close() {
        agentSessionController.close();
    }
}
