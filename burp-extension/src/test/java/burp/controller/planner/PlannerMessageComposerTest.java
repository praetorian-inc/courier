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
import burp.model.HttpRequestResponsePair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlannerMessageComposerTest {
    @Test
    void attachesEachSelectedRequestOnlyOncePerConversation() throws Exception {
        ConversationManager conversations = new ConversationManager();
        PlannerMessageComposer composer = new PlannerMessageComposer(
                conversations, mock(LogController.class));
        HttpRequestResponsePair selectedRequest = new HttpRequestResponsePair().setToolSource("Proxy");

        PlannerMessageComposer.ComposedMessage first =
                composer.compose("first", 0, "query", List.of(selectedRequest));
        PlannerMessageComposer.ComposedMessage second =
                composer.compose("second", 0, "query", List.of(selectedRequest));

        assertEquals(1, first.attachedRequestCount());
        assertEquals("second", second.outboundMessage());
        assertFalse(second.requestAttached());
        assertEquals("Proxy", decodeRequestPayload(first).get("toolSource").asText());
        assertEquals(2, conversations.getConversation(0).getMessagesSent().size());

        conversations.clear(0);
        PlannerMessageComposer.ComposedMessage afterClear =
                composer.compose("after clear", 0, "query", List.of(selectedRequest));
        assertEquals("after clear", afterClear.outboundMessage());
        assertFalse(afterClear.requestAttached());
    }

    @Test
    void attachesRequestWhenSelectedAfterConversationHasStarted() throws Exception {
        ConversationManager conversations = new ConversationManager();
        PlannerMessageComposer composer = new PlannerMessageComposer(
                conversations, mock(LogController.class));

        PlannerMessageComposer.ComposedMessage first =
                composer.compose("first", 0, "query", List.of());
        PlannerMessageComposer.ComposedMessage second = composer.compose(
                "second", 0, "query", List.of(new HttpRequestResponsePair()));

        assertEquals("first", first.outboundMessage());
        assertTrue(second.requestAttached());
        assertEquals(1, second.attachedRequestCount());
        assertTrue(decodeRequestPayload(second).isObject());
    }

    @Test
    void attachesOnlyNewRequestsWhenOldAndNewSelectionsAreMixed() throws Exception {
        ConversationManager conversations = new ConversationManager();
        PlannerMessageComposer composer = new PlannerMessageComposer(
                conversations, mock(LogController.class));
        HttpRequestResponsePair firstRequest =
                new HttpRequestResponsePair().setToolSource("Proxy");
        HttpRequestResponsePair secondRequest =
                new HttpRequestResponsePair().setToolSource("Repeater");

        composer.compose("first", 0, "query", List.of(firstRequest));
        PlannerMessageComposer.ComposedMessage second = composer.compose(
                "second", 0, "query", List.of(firstRequest, secondRequest));
        PlannerMessageComposer.ComposedMessage third = composer.compose(
                "third", 0, "query", List.of(firstRequest, secondRequest));

        assertEquals(1, second.attachedRequestCount());
        assertEquals("Repeater", decodeRequestPayload(second).get("toolSource").asText());
        assertEquals("third", third.outboundMessage());
        assertFalse(third.requestAttached());
    }

    @Test
    void releasedEvidenceCanBeRetriedAfterSendFailure() {
        ConversationManager conversations = new ConversationManager();
        PlannerMessageComposer composer = new PlannerMessageComposer(
                conversations, mock(LogController.class));
        HttpRequestResponsePair request = new HttpRequestResponsePair();

        PlannerMessageComposer.ComposedMessage failed =
                composer.compose("first attempt", 0, "query", List.of(request));
        conversations.releaseReservedRequests(0, failed.attachedRequests());
        PlannerMessageComposer.ComposedMessage retry =
                composer.compose("retry", 0, "query", List.of(request));

        assertTrue(failed.requestAttached());
        assertTrue(retry.requestAttached());
    }

    @Test
    void excludesReleasedRequestsWhenSerializationFails() throws Exception {
        ConversationManager conversations = new ConversationManager();
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("test failure") { });
        PlannerMessageComposer composer = new PlannerMessageComposer(
                conversations, mock(LogController.class), failingMapper);
        HttpRequestResponsePair request = new HttpRequestResponsePair();

        PlannerMessageComposer.ComposedMessage failed = composer.compose(
                "message", 0, "query", List.of(request));

        assertEquals("message", failed.outboundMessage());
        assertFalse(failed.requestAttached());
        PlannerMessageComposer.ComposedMessage retry = new PlannerMessageComposer(
                conversations, mock(LogController.class)).compose(
                        "retry", 0, "query", List.of(request));
        assertTrue(retry.requestAttached());
    }

    @Test
    void attachesAllSelectedRequestsAsJsonArray() throws Exception {
        ConversationManager conversations = new ConversationManager();
        PlannerMessageComposer composer = new PlannerMessageComposer(
                conversations, mock(LogController.class));

        PlannerMessageComposer.ComposedMessage message = composer.compose(
                "inspect", 0, "query", List.of(
                        new HttpRequestResponsePair().setToolSource("Proxy"),
                        new HttpRequestResponsePair().setToolSource("Repeater")));

        JsonNode payload = decodeRequestPayload(message);
        assertEquals(2, message.attachedRequestCount());
        assertTrue(payload.isArray());
        assertEquals("Proxy", payload.get(0).get("toolSource").asText());
        assertEquals("Repeater", payload.get(1).get("toolSource").asText());
    }

    @Test
    void routesEachTabsSelectionToThatTabsFirstMessage() {
        ConversationManager conversations = new ConversationManager();
        int secondTab = conversations.createConversation();
        PlannerMessageComposer composer = new PlannerMessageComposer(
                conversations, mock(LogController.class));

        PlannerMessageComposer.ComposedMessage firstTab = composer.compose(
                "tab one", 0, "query", List.of(new HttpRequestResponsePair()));
        PlannerMessageComposer.ComposedMessage secondTabMessage = composer.compose(
                "tab two", secondTab, "query", List.of(new HttpRequestResponsePair()));

        assertTrue(firstTab.requestAttached());
        assertTrue(secondTabMessage.requestAttached());
        assertEquals(1, conversations.getConversation(0).getMessagesSent().size());
        assertEquals(1, conversations.getConversation(secondTab).getMessagesSent().size());
    }

    private static JsonNode decodeRequestPayload(
            PlannerMessageComposer.ComposedMessage message) throws Exception {
        String encodedRequest = message.outboundMessage().substring(
                message.outboundMessage().indexOf(PlannerMessageComposer.REQUEST_DATA_PREFIX)
                        + PlannerMessageComposer.REQUEST_DATA_PREFIX.length(),
                message.outboundMessage().length() - 1);
        return new ObjectMapper().readTree(new String(
                Base64.getDecoder().decode(encodedRequest), StandardCharsets.UTF_8));
    }
}
