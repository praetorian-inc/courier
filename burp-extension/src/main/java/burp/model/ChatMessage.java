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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class ChatMessage {
    public enum MessageType {
        USER,
        TOOL,
        CHARIOT,
        SYSTEM,
        ERROR
    }
    
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private String messageId;
    private String sender;
    private boolean visible = false;
    
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.messageId = generateMessageId();
        this.type = MessageType.USER;
        this.sender = "User";
    }
    
    public ChatMessage(String content, MessageType type) {
        this();
        this.content = content;
        this.type = type;
        this.sender = type == MessageType.USER ? "User" : type == MessageType.CHARIOT ? "Guard" : "System";
        if (this.type == MessageType.USER) {
            this.visible = true;
        }
    }
    
    public ChatMessage(String content, MessageType type, String sender) {
        this();
        this.content = content;
        this.type = type;
        this.sender = sender;
        this.visible = type != MessageType.TOOL;
    }
    
    private String generateMessageId() {
        return "MSG-" + UUID.randomUUID();
    }
    
    public String getContent() {
        return content;
    }
    
    public ChatMessage setContent(String content) {
        this.content = content;
        return this;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public ChatMessage setType(MessageType type) {
        this.type = type;
        return this;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public ChatMessage setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public ChatMessage setMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }
    
    public String getSender() {
        return sender;
    }
    
    public ChatMessage setSender(String sender) {
        this.sender = sender;
        return this;
    }
    
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    public String getFormattedMessage() {
        return String.format("[%s] %s: %s", 
                getFormattedTimestamp(), sender, content);
    }

    public boolean isVisible() {
        return visible;
    }
    
    public ChatMessage setVisibility(boolean visible) {
        this.visible = visible;
        return this;
    }
    
    @Override
    public String toString() {
        return "ChatMessage{" +
                "type=" + type +
                ", sender='" + sender + '\'' +
                ", contentLength=" + (content == null ? 0 : content.length()) +
                '}';
    }
}
