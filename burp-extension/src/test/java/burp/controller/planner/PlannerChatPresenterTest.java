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
import burp.serialization.dto.MessageResponseDto;
import burp.view.PlannerChatView;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PlannerChatPresenterTest {
    @Test
    void displaysChariotApiRoleAsGuard() throws Exception {
        PlannerChatView chatView = new PlannerChatView();
        PlannerChatPresenter presenter = new PlannerChatPresenter(
                new ConversationManager(), Map.of(0, chatView), mock(LogController.class));
        MessageResponseDto message = new MessageResponseDto();
        message.setRole("chariot");
        message.setContent("response");

        presenter.addApiMessage(message, 0);
        SwingUtilities.invokeAndWait(() -> { });

        assertTrue(chatView.getTranscriptText().contains("<Guard> response"));
    }

    @Test
    void ignoresEchoedUserAndQueuedSystemMessages() throws Exception {
        PlannerChatView chatView = new PlannerChatView();
        PlannerChatPresenter presenter = new PlannerChatPresenter(
                new ConversationManager(), Map.of(0, chatView), mock(LogController.class));
        presenter.add(new burp.model.ChatMessage(
                "hello", burp.model.ChatMessage.MessageType.USER, "You"), 0);
        MessageResponseDto echoedUser = new MessageResponseDto();
        echoedUser.setRole("user");
        echoedUser.setContent("hello");
        MessageResponseDto queued = new MessageResponseDto();
        queued.setRole("system");
        queued.setContent("Your request has been queued for processing.");

        presenter.addApiMessage(echoedUser, 0);
        presenter.addApiMessage(queued, 0);
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(1, chatView.getTranscriptText().lines().count());
        assertFalse(chatView.getTranscriptText().contains("queued for processing"));
    }
}
