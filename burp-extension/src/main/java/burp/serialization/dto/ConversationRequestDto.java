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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for sending conversation requests to the Guard API
 */
public class ConversationRequestDto {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String conversationId; // uuid from previous response
    private String message;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String mode;

    public ConversationRequestDto() {}

    public ConversationRequestDto(String message, String mode) {
        this.message = message;
        this.mode = mode;
    }

    public ConversationRequestDto(String conversationId, String message, String mode) {
        this.conversationId = conversationId;
        this.message = message;
        this.mode = mode;
    }

    // Getters and Setters
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "ConversationRequestDto{" +
                "hasConversationId=" + (conversationId != null && !conversationId.isEmpty()) +
                ", messageLength=" + (message == null ? 0 : message.length()) +
                ", mode='" + mode + '\'' +
                '}';
    }
}
