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

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpPreviewFormatterTest {
    @Test
    void formatsRawRequestWithoutDuplicatingFormattingLogic() {
        HttpRequest request = mock(HttpRequest.class);
        HttpHeader header = mock(HttpHeader.class);
        when(request.method()).thenReturn("POST");
        when(request.path()).thenReturn("/items");
        when(request.httpVersion()).thenReturn("HTTP/1.1");
        when(request.headers()).thenReturn(List.of(header));
        when(header.name()).thenReturn("Content-Type");
        when(header.value()).thenReturn("application/json");
        when(request.bodyToString()).thenReturn("{}");

        String raw = HttpPreviewFormatter.raw(request);
        assertTrue(raw.startsWith("POST /items HTTP/1.1"));
        assertTrue(raw.contains("Content-Type: application/json"));
        assertTrue(raw.endsWith("{}"));
    }

    @Test
    void rendersDeterministicHexDump() {
        String dump = HttpPreviewFormatter.hex("A\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(dump.startsWith("00000000  41 0a"));
        assertTrue(dump.endsWith("|A.|\n"));
    }
}
