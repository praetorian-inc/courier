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

package burp.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.Instant;

/**
 * Represents a conversation between a user and AI assistant.
 * Based on the Go Conversation model but adapted for Java.
 */
public class Conversation {
    private String username;
    private String key;
    private String uuid;
    private String user;
    private String created;
    private String topic;
    private List<ConversationRequest> messagesSent;
    private List<ConversationResponse> messagesReceived;

    public Conversation() {
        this.messagesSent = new CopyOnWriteArrayList<>();
        this.messagesReceived = new CopyOnWriteArrayList<>();
        this.created = Instant.now().toString();
    }

    public Conversation(String topic) {
        this();
        this.topic = topic;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public List<ConversationRequest> getMessagesSent() {
        return messagesSent;
    }

    public void setMessagesSent(List<ConversationRequest> messagesSent) {
        this.messagesSent = new CopyOnWriteArrayList<>(messagesSent == null ? List.of() : messagesSent);
    }

    public List<ConversationResponse> getMessagesReceived() {
        return messagesReceived;
    }

    public void setMessagesReceived(List<ConversationResponse> messagesReceived) {
        this.messagesReceived = new CopyOnWriteArrayList<>(
                messagesReceived == null ? List.of() : messagesReceived);
    }

    public void addSentMessage(ConversationRequest request) {
        this.messagesSent.add(request);
    }

    public void addSentMessage(String conversationId, String message, String mode) {
        this.messagesSent.add(new ConversationRequest(conversationId, message, mode));
    }

    public void addSentMessage(String conversationId, String message) {
        this.messagesSent.add(new ConversationRequest(conversationId, message));
    }

    public void addReceivedMessage(ConversationResponse response) {
        this.messagesReceived.add(response);
    }

    public void addReceivedMessage(String conversationId, String message, boolean success) {
        this.messagesReceived.add(new ConversationResponse(conversationId, message, success));
    }

    public boolean isValid() {
        return key != null && !key.isEmpty();
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "hasKey=" + (key != null && !key.isEmpty()) +
                ", hasUuid=" + (uuid != null && !uuid.isEmpty()) +
                ", created='" + created + '\'' +
                ", messagesSent=" + messagesSent.size() +
                ", messagesReceived=" + messagesReceived.size() +
                '}';
    }
}
