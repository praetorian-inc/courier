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

package burp.handlers;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BinaryMessageSupportTest {
    @Test
    void comparesMessageBytesByContent() {
        HttpRequest first = requestBytes("same");
        HttpRequest second = requestBytes("same");
        HttpRequest different = requestBytes("different");

        assertTrue(BinaryMessageSupport.requestsEqual(first, second));
        assertFalse(BinaryMessageSupport.requestsEqual(first, different));
    }

    @Test
    void base64EncodesBinaryRequestBodyOnce() {
        HttpRequest request = mock(HttpRequest.class);
        HttpRequest encoded = mock(HttpRequest.class);
        ByteArray body = mock(ByteArray.class);
        when(body.getBytes()).thenReturn(new byte[] {0, 1, 2});
        when(request.body()).thenReturn(body);
        String expected = Base64.getEncoder().encodeToString(new byte[] {0, 1, 2});
        when(request.withBody(expected)).thenReturn(encoded);

        assertSame(encoded, BinaryMessageSupport.encodeBody(request));
        verify(request).withBody(expected);
    }

    @Test
    void preservesEveryIssuePairWhenOnlyOneContainsBinaryData() {
        HttpRequest textRequest = mock(HttpRequest.class);
        HttpResponse textResponse = mock(HttpResponse.class);
        when(textRequest.headerValue("Content-Type")).thenReturn("text/plain");
        when(textRequest.fileExtension()).thenReturn("txt");
        when(textResponse.headerValue("Content-Type")).thenReturn("text/plain");

        HttpRequest binaryRequest = mock(HttpRequest.class);
        HttpResponse binaryResponse = mock(HttpResponse.class);
        ByteArray binaryBody = mock(ByteArray.class);
        when(binaryRequest.headerValue("Content-Type")).thenReturn("text/plain");
        when(binaryRequest.fileExtension()).thenReturn("bin");
        when(binaryRequest.body()).thenReturn(binaryBody);
        when(binaryBody.getBytes()).thenReturn(new byte[] {0, 1});
        when(binaryRequest.withBody(anyString())).thenReturn(binaryRequest);
        ByteArray binaryResponseBody = mock(ByteArray.class);
        when(binaryResponse.headerValue("Content-Type")).thenReturn("text/plain");
        when(binaryResponse.body()).thenReturn(binaryResponseBody);
        when(binaryResponseBody.getBytes()).thenReturn(new byte[] {2, 3});
        when(binaryResponse.withBody(anyString())).thenReturn(binaryResponse);

        HttpRequestResponse first = mock(HttpRequestResponse.class);
        HttpRequestResponse second = mock(HttpRequestResponse.class);
        when(first.request()).thenReturn(textRequest);
        when(first.response()).thenReturn(textResponse);
        when(second.request()).thenReturn(binaryRequest);
        when(second.response()).thenReturn(binaryResponse);

        BinaryMessageSupport.EncodedPairs result = BinaryMessageSupport.encodePairs(
                List.of(first, second), (request, response) -> mock(HttpRequestResponse.class));

        assertTrue(result.encoded());
        assertEquals(2, result.pairs().size());
    }

    private static HttpRequest requestBytes(String content) {
        HttpRequest request = mock(HttpRequest.class);
        ByteArray bytes = mock(ByteArray.class);
        when(bytes.getBytes()).thenReturn(content.getBytes(StandardCharsets.UTF_8));
        when(request.toByteArray()).thenReturn(bytes);
        return request;
    }
}
