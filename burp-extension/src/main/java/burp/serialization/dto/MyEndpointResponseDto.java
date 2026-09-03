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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for the response from the /my endpoint
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MyEndpointResponseDto {
    private int count;
    private List<MessageResponseDto> messages;

    public MyEndpointResponseDto() {}

    // Getters and Setters
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<MessageResponseDto> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageResponseDto> messages) {
        this.messages = messages;
    }

    @Override
    public String toString() {
        return "MyEndpointResponseDto{" +
                "count=" + count +
                ", messages=" + (messages != null ? messages.size() : 0) + " messages" +
                '}';
    }
}
