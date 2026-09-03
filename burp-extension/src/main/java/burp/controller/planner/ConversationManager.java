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

import burp.model.ChatMessage;
import burp.model.Conversation;
import burp.model.ConversationRequest;
import burp.model.ConversationResponse;
import burp.model.HttpRequestResponsePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConversationManager {
    private final Map<Integer, Conversation> conversations = new ConcurrentHashMap<>();
    private final Map<Integer, List<ChatMessage>> messages = new ConcurrentHashMap<>();
    private final AtomicInteger nextTabIndex = new AtomicInteger();
    private final Map<Integer, java.util.Set<HttpRequestResponsePair>> attachedRequests =
            new ConcurrentHashMap<>();
    private final Map<Integer, java.util.Set<HttpRequestResponsePair>> reservedRequests =
            new ConcurrentHashMap<>();

    public ConversationManager() {
        createConversation();
    }

    public int createConversation() {
        int tabIndex = nextTabIndex.getAndIncrement();
        conversations.put(tabIndex, new Conversation());
        messages.put(tabIndex, new CopyOnWriteArrayList<>());
        attachedRequests.put(tabIndex, ConcurrentHashMap.newKeySet());
        reservedRequests.put(tabIndex, ConcurrentHashMap.newKeySet());
        return tabIndex;
    }

    public Conversation getConversation(int tabIndex) {
        return conversations.get(tabIndex);
    }

    public Map<Integer, Conversation> getConversations() {
        return Collections.unmodifiableMap(Map.copyOf(conversations));
    }

    public List<ChatMessage> getMessages(int tabIndex) {
        List<ChatMessage> tabMessages = messages.get(tabIndex);
        return tabMessages == null ? List.of() : List.copyOf(tabMessages);
    }

    public List<ChatMessage> getAllMessages() {
        List<ChatMessage> allMessages = new ArrayList<>();
        messages.keySet().stream().sorted().forEach(tab -> allMessages.addAll(messages.get(tab)));
        return allMessages;
    }

    public void addMessage(int tabIndex, ChatMessage message) {
        messages.computeIfAbsent(tabIndex, ignored -> new CopyOnWriteArrayList<>()).add(message);
    }

    public List<HttpRequestResponsePair> reserveUnattachedRequests(int tabIndex,
            List<HttpRequestResponsePair> selectedRequests) {
        if (selectedRequests == null || selectedRequests.isEmpty()) {
            return List.of();
        }
        java.util.Set<HttpRequestResponsePair> attached = attachedRequests.computeIfAbsent(
                tabIndex, ignored -> ConcurrentHashMap.newKeySet());
        java.util.Set<HttpRequestResponsePair> reserved = reservedRequests.computeIfAbsent(
                tabIndex, ignored -> ConcurrentHashMap.newKeySet());
        List<HttpRequestResponsePair> claimed = new ArrayList<>();
        for (HttpRequestResponsePair request : selectedRequests) {
            if (request != null && !attached.contains(request) && reserved.add(request)) {
                claimed.add(request);
            }
        }
        return List.copyOf(claimed);
    }

    public void markRequestsAttached(int tabIndex, List<HttpRequestResponsePair> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        attachedRequests.computeIfAbsent(tabIndex, ignored -> ConcurrentHashMap.newKeySet())
                .addAll(requests);
        reservedRequests.computeIfAbsent(tabIndex, ignored -> ConcurrentHashMap.newKeySet())
                .removeAll(requests);
    }

    public void releaseReservedRequests(int tabIndex, List<HttpRequestResponsePair> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        reservedRequests.computeIfAbsent(tabIndex, ignored -> ConcurrentHashMap.newKeySet())
                .removeAll(requests);
    }

    public void recordSentMessage(int tabIndex, String conversationId, String message, String mode) {
        Conversation conversation = conversations.get(tabIndex);
        if (conversation != null) {
            conversation.addSentMessage(new ConversationRequest(conversationId, message, mode));
        }
    }

    public void recordReceivedMessage(int tabIndex, String conversationId, String message, boolean success) {
        Conversation conversation = conversations.get(tabIndex);
        if (conversation != null) {
            conversation.addReceivedMessage(new ConversationResponse(conversationId, message, success));
        }
    }

    public void clear(int tabIndex) {
        List<ChatMessage> tabMessages = messages.get(tabIndex);
        if (tabMessages != null) {
            tabMessages.clear();
        }
        Conversation conversation = conversations.get(tabIndex);
        if (conversation != null) {
            conversation.getMessagesSent().clear();
            conversation.getMessagesReceived().clear();
        }
    }
}
