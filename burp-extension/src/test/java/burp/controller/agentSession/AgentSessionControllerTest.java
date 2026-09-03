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

package burp.controller.agentSession;

import burp.controller.LogController;
import burp.utils.SessionManager;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgentSessionControllerTest {
    @Test
    void skipsInvalidMessageTimestampsWithoutDroppingLaterMessages() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        AgentSessionController controller = new AgentSessionController(
                mock(LogController.class), mock(HttpClient.class), executor);
        AtomicInteger delivered = new AtomicInteger();
        String response = """
                {"messages":[
                  {"conversationId":"conversation-id","timestamp":null},
                  {"conversationId":"conversation-id","timestamp":"invalid"},
                  {"conversationId":"conversation-id","timestamp":"2026-01-01T00:00:00Z"}
                ]}
                """;

        controller.processMyEndpointResponse(
                response, 1, "conversation-id", (message, tab) -> delivered.incrementAndGet());

        assertEquals(1, delivered.get());
        controller.close();
    }

    @Test
    void createsOnlyOnePollingTaskPerConversationTab() {
        SessionManager session = mock(SessionManager.class);
        when(session.isEnabled()).thenReturn(true);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        AgentSessionController controller = new AgentSessionController(
                mock(LogController.class), mock(HttpClient.class), executor);
        controller.setSessionManager(session);
        controller.setConversationUuid(3, "conversation-id");

        controller.startPollingForUpdates(3, (message, tab) -> { });
        controller.startPollingForUpdates(3, (message, tab) -> { });

        assertEquals(1, controller.activePollingCount());
        controller.stopPolling();
        assertEquals(0, controller.activePollingCount());
        assertFalse(executor.isShutdown());

        controller.startPollingForUpdates(3, (message, tab) -> { });
        assertEquals(1, controller.activePollingCount());
        controller.close();
        assertEquals(0, controller.activePollingCount());
        assertTrue(executor.isShutdown());
    }
}
