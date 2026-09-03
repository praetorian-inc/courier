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

import burp.model.ChatMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConversationManagerTest {
    @Test
    void keepsConversationAndDisplayStateIsolatedByTab() {
        ConversationManager manager = new ConversationManager();
        int secondTab = manager.createConversation();

        manager.addMessage(0, new ChatMessage("first", ChatMessage.MessageType.USER));
        manager.addMessage(secondTab, new ChatMessage("second", ChatMessage.MessageType.USER));
        manager.recordSentMessage(0, "conversation-1", "first", "query");
        manager.recordReceivedMessage(secondTab, "conversation-2", "response", true);

        assertEquals("first", manager.getMessages(0).get(0).getContent());
        assertEquals("second", manager.getMessages(secondTab).get(0).getContent());
        assertEquals(1, manager.getConversation(0).getMessagesSent().size());
        assertEquals(0, manager.getConversation(0).getMessagesReceived().size());
        assertEquals(1, manager.getConversation(secondTab).getMessagesReceived().size());
        assertEquals(0, manager.getConversation(secondTab).getMessagesSent().size());

        manager.clear(0);
        assertTrue(manager.getMessages(0).isEmpty());
        assertEquals(1, manager.getMessages(secondTab).size());
    }
}
