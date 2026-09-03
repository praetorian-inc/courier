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

package burp.controller;

import burp.controller.agentSession.AgentSessionController;
import burp.model.Conversation;
import burp.view.PlannerView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlannerControllerTest {
    @Test
    void routesAsyncResponseToOriginatingTabRatherThanSelectedTab() {
        PlannerView view = mock(PlannerView.class);
        when(view.getCurrentTabIndex()).thenReturn(1);
        PlannerController controller = new PlannerController(view, mock(LogController.class));
        AgentSessionController.ConversationResponseCallback callback =
                controller.createConversationResponseCallback(0);

        callback.onConversationCreated("originating-conversation", "topic");

        Conversation origin = controller.getConversation(0);
        assertEquals("originating-conversation", origin.getUuid());
        assertEquals("topic", origin.getTopic());
        controller.close();
    }
}
