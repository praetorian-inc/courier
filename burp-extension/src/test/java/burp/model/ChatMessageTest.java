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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageTest {
    @Test
    void displaysUserAndSystemMessagesButHidesToolsByDefault() {
        assertTrue(new ChatMessage("user", ChatMessage.MessageType.USER).isVisible());
        assertTrue(new ChatMessage("system", ChatMessage.MessageType.SYSTEM, "System").isVisible());
        assertFalse(new ChatMessage("tool", ChatMessage.MessageType.TOOL, "Tool").isVisible());
        assertEquals("Guard", new ChatMessage("reply", ChatMessage.MessageType.CHARIOT).getSender());
    }

    @Test
    void generatesCollisionResistantIdentifiers() {
        ChatMessage first = new ChatMessage();
        ChatMessage second = new ChatMessage();
        assertNotEquals(first.getMessageId(), second.getMessageId());
    }
}
