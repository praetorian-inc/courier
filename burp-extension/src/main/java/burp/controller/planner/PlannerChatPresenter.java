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

package burp.controller.planner;

import burp.controller.LogController;
import burp.model.ChatMessage;
import burp.serialization.dto.MessageResponseDto;
import burp.utils.SafeLogFormatter;
import burp.view.PlannerChatView;

import java.util.Map;

public final class PlannerChatPresenter {
    private final ConversationManager conversations;
    private final Map<Integer, PlannerChatView> chatViews;
    private final LogController logger;

    public PlannerChatPresenter(ConversationManager conversations,
            Map<Integer, PlannerChatView> chatViews, LogController logger) {
        this.conversations = conversations;
        this.chatViews = chatViews;
        this.logger = logger;
    }

    public void add(ChatMessage message, int tabIndex) {
        conversations.addMessage(tabIndex, message);
        if (message.isVisible()) {
            append(tabIndex, message.getSender(), message.getContent(),
                    message.getType(), message.getFormattedTimestamp());
        }
        logger.logDebug("Added chat message to tab " + tabIndex
                + " (" + SafeLogFormatter.length(message.getContent()) + " characters)");
    }

    public void addSystem(String content, int tabIndex) {
        add(new ChatMessage(content, ChatMessage.MessageType.SYSTEM, "System"), tabIndex);
    }

    public void addError(String content, int tabIndex) {
        add(new ChatMessage(content, ChatMessage.MessageType.ERROR, "Error"), tabIndex);
    }

    public void showGuardPending(int tabIndex) {
        PlannerChatView chatView = chatViews.get(tabIndex);
        if (chatView != null) {
            chatView.showGuardPending();
        }
    }

    public void failPending(int tabIndex, String error) {
        PlannerChatView chatView = chatViews.get(tabIndex);
        if (chatView != null) {
            chatView.resolveGuardPending();
        }
        addError(error, tabIndex);
    }

    public void handleInitialResponse(String responseText, String conversationId,
            boolean success, int tabIndex) {
        if (isQueueAcknowledgement(responseText)) {
            return;
        }
        PlannerChatView chatView = chatViews.get(tabIndex);
        if (chatView != null) {
            chatView.resolveGuardPending();
        }
        conversations.recordReceivedMessage(tabIndex, conversationId, responseText, success);
        add(new ChatMessage(responseText, success
                ? ChatMessage.MessageType.CHARIOT : ChatMessage.MessageType.ERROR,
                success ? "Guard" : "Error"), tabIndex);
    }

    public void clear(int tabIndex) {
        conversations.clear(tabIndex);
        PlannerChatView chatView = chatViews.get(tabIndex);
        if (chatView != null) {
            chatView.clear();
        }
        logger.logInfo("Chat cleared for tab " + tabIndex);
    }

    public void addApiMessage(MessageResponseDto message, int tabIndex) {
        PlannerChatView chatView = chatViews.get(tabIndex);
        if (chatView == null) {
            logger.logError("No chat view found for tab: " + tabIndex);
            return;
        }
        String role = message.getRole();
        if ("user".equalsIgnoreCase(role) || isQueueAcknowledgement(message.getContent())) {
            return;
        }
        String content = message.getContent();
        ChatMessage.MessageType type = messageType(role);
        ChatMessage chatMessage = new ChatMessage(content, type, displayRole(role));
        conversations.addMessage(tabIndex, chatMessage);
        if ("user".equals(role)) {
            conversations.recordSentMessage(tabIndex, message.getConversationId(), content, null);
        } else {
            conversations.recordReceivedMessage(tabIndex, message.getConversationId(), content, true);
        }
        if (logger.getCurrentLogLevel() < LogController.LOG_LEVEL_DEBUG
                && type == ChatMessage.MessageType.TOOL) {
            return;
        }
        chatMessage.setVisibility(true);
        if (type == ChatMessage.MessageType.CHARIOT || "planner-output".equalsIgnoreCase(role)) {
            chatView.resolveGuardPending();
        }
        append(tabIndex, displayRole(role), content, type, message.getTimestamp());
    }

    private void append(int tabIndex, String sender, String content,
            ChatMessage.MessageType type, String timestamp) {
        PlannerChatView chatView = chatViews.get(tabIndex);
        if (chatView == null) {
            logger.logError("No chat view found for tab " + tabIndex);
            return;
        }
        chatView.append(sender, content, type, timestamp);
    }

    private static boolean isQueueAcknowledgement(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("queued for processing")
                || normalized.contains("queued for execution");
    }

    private static ChatMessage.MessageType messageType(String role) {
        if (role == null) {
            return ChatMessage.MessageType.SYSTEM;
        }
        return switch (role.toLowerCase()) {
            case "user" -> ChatMessage.MessageType.USER;
            case "chariot" -> ChatMessage.MessageType.CHARIOT;
            case "tool call", "tool response", "tool" -> ChatMessage.MessageType.TOOL;
            default -> ChatMessage.MessageType.SYSTEM;
        };
    }

    private static String displayRole(String role) {
        if (role == null) {
            return "Unknown";
        }
        return switch (role.toLowerCase()) {
            case "user" -> "You";
            case "chariot" -> "Guard";
            case "system" -> "System";
            case "tool", "tool call" -> "Tool";
            case "tool response" -> "Tool Response";
            case "planner-output" -> "Planner";
            default -> role;
        };
    }
}
