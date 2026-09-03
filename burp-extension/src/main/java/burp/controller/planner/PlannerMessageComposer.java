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
import burp.model.Conversation;
import burp.model.HttpRequestResponsePair;
import burp.serialization.util.JsonMapperUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public final class PlannerMessageComposer {
    static final String REQUEST_DATA_PREFIX = " [with B64 encoded request data: ";

    private final ConversationManager conversations;
    private final LogController logger;
    private final ObjectMapper objectMapper;
    private final PlannerRequestPayloadFactory payloadFactory;

    public PlannerMessageComposer(ConversationManager conversations, LogController logger) {
        this(conversations, logger, JsonMapperUtil.getConfiguredMapper());
    }

    PlannerMessageComposer(ConversationManager conversations,
            LogController logger, ObjectMapper objectMapper) {
        this.conversations = conversations;
        this.logger = logger;
        this.objectMapper = objectMapper;
        this.payloadFactory = new PlannerRequestPayloadFactory();
    }

    public ComposedMessage compose(String message, int tabIndex, String mode,
            List<HttpRequestResponsePair> selectedRequests) {
        Conversation conversation = conversations.getConversation(tabIndex);
        if (conversation == null) {
            return new ComposedMessage(message, List.of());
        }

        List<HttpRequestResponsePair> requestsToAttach =
                conversations.reserveUnattachedRequests(tabIndex, selectedRequests);
        String outboundMessage = message;
        int attachedRequestCount = 0;
        if (!requestsToAttach.isEmpty()) {
            try {
                Object requestPayload = payloadFactory.create(requestsToAttach);
                String requestJson = objectMapper.writeValueAsString(requestPayload);
                String encodedRequest = Base64.getEncoder().encodeToString(
                        requestJson.getBytes(StandardCharsets.UTF_8));
                outboundMessage = message + REQUEST_DATA_PREFIX + encodedRequest + "]";
                attachedRequestCount = requestsToAttach.size();
                logger.logInfo("Attached " + attachedRequestCount
                        + " newly selected request(s) to planner message in tab " + tabIndex);
            } catch (Exception exception) {
                conversations.releaseReservedRequests(tabIndex, requestsToAttach);
                requestsToAttach = List.of();
                logger.logError("Error serializing selected request(s): " + exception.getMessage());
            }
        }

        conversations.recordSentMessage(tabIndex, conversation.getUuid(), outboundMessage, mode);
        return new ComposedMessage(outboundMessage, List.copyOf(requestsToAttach));
    }

    public record ComposedMessage(String outboundMessage,
            List<HttpRequestResponsePair> attachedRequests) {
        public int attachedRequestCount() {
            return attachedRequests.size();
        }

        public boolean requestAttached() {
            return !attachedRequests.isEmpty();
        }
    }
}
