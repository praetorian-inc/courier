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

/**
 * Represents a conversation response from the planner.
 * Based on the Go ConversationResponse structure.
 */
public class ConversationResponse {
    private String conversationId;
    private String response;
    private boolean success;

    public ConversationResponse() {}

    public ConversationResponse(String conversationId, String response, boolean success) {
        this.conversationId = conversationId;
        this.response = response;
        this.success = success;
    }

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

    @Override
    public String toString() {
        return "ConversationResponse{" +
                "hasConversationId=" + (conversationId != null && !conversationId.isEmpty()) +
                ", responseLength=" + (response == null ? 0 : response.length()) +
                ", success=" + success +
                '}';
    }
}
