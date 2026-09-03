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
 * DTO for receiving conversation responses from the Guard API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConversationResponseDto {
    private ConversationInfoDto conversation;
    private PlannerResponseDto response;
    private String error;

    public ConversationResponseDto() {}

    // Getters and Setters
    public ConversationInfoDto getConversation() {
        return conversation;
    }

    public void setConversation(ConversationInfoDto conversation) {
        this.conversation = conversation;
    }

    public PlannerResponseDto getResponse() {
        return response;
    }

    public void setResponse(PlannerResponseDto response) {
        this.response = response;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    /**
     * Nested DTO for conversation information
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConversationInfoDto {
        private String username;
        private String key;
        private String uuid;
        private String user;
        private String created;
        private String topic;

        public ConversationInfoDto() {}

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
    }

    /**
     * Nested DTO for planner response
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlannerResponseDto {
        private String conversationId;
        private String response;
        private boolean success;

        public PlannerResponseDto() {}

        // Getters and Setters
        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
    }

    @Override
    public String toString() {
        return "ConversationResponseDto{" +
                "hasConversation=" + (conversation != null) +
                ", hasResponse=" + (response != null) +
                ", hasError=" + (error != null && !error.isEmpty()) +
                '}';
    }
}
