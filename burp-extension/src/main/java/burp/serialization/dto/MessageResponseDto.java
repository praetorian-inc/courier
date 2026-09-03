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

package burp.serialization.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for Message objects returned from the /my endpoint
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageResponseDto {
    private String username;
    private String key;
    private String conversationId;
    private String role;
    private String content;
    private String timestamp;
    private String messageId;
    private long ttl;
    private String toolUseId;
    private String toolUseContent;

    public MessageResponseDto() {}

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

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public String getToolUseId() {
        return toolUseId;
    }

    public void setToolUseId(String toolUseId) {
        this.toolUseId = toolUseId;
    }

    public String getToolUseContent() {
        return toolUseContent;
    }

    public void setToolUseContent(String toolUseContent) {
        this.toolUseContent = toolUseContent;
    }

    @Override
    public String toString() {
        return "MessageResponseDto{" +
                "role='" + role + '\'' +
                ", contentLength=" + (content == null ? 0 : content.length()) +
                ", timestamp='" + timestamp + '\'' +
                ", ttl=" + ttl +
                ", hasToolUseContent=" + (toolUseContent != null && !toolUseContent.isEmpty()) +
                '}';
    }
}
