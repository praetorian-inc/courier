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

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.ToolSource;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.controller.ConfigurationController;
import burp.controller.ConnectionController;
import burp.controller.LogController;
import burp.model.HttpRequestResponsePair;
import burp.utils.MonitoredHashMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpRequestsAndResponsesHandlerTest {
    @Test
    void publishesRequestAndResponseAsOneBatchEntry() {
        ConfigurationController configuration = mock(ConfigurationController.class);
        ConnectionController connection = mock(ConnectionController.class);
        when(connection.isEnabled()).thenReturn(true);
        List<Map<String, Object>> payloads = new ArrayList<>();
        // Use a test destination so no network operation occurs.
        MonitoredHashMap<Integer, HttpRequestResponsePair> map = testMap(payloads);
        HttpRequestsAndResponsesHandler handler = new HttpRequestsAndResponsesHandler(
                configuration, mock(MontoyaApi.class), connection);
        handler.setRequestResponses(map);
        ToolSource source = mock(ToolSource.class);
        when(source.isFromTool(ToolType.PROXY)).thenReturn(false);
        when(source.toolType()).thenReturn(ToolType.REPEATER);
        Annotations annotations = mock(Annotations.class);
        HttpRequestToBeSent request = mock(HttpRequestToBeSent.class);
        when(request.messageId()).thenReturn(42);
        when(request.url()).thenReturn("https://example.test");
        when(request.toolSource()).thenReturn(source);
        when(request.annotations()).thenReturn(annotations);
        HttpResponseReceived response = mock(HttpResponseReceived.class);
        when(response.messageId()).thenReturn(42);
        when(response.initiatingRequest()).thenReturn(request);
        when(response.toolSource()).thenReturn(source);
        when(response.annotations()).thenReturn(annotations);

        assertThrows(NullPointerException.class,
                () -> handler.handleHttpRequestToBeSent(request));
        assertTrue(payloads.isEmpty());
        assertThrows(NullPointerException.class,
                () -> handler.handleHttpResponseReceived(response));

        assertEquals(1, payloads.size());
        @SuppressWarnings("unchecked")
        Map<Integer, HttpRequestResponsePair> data =
                (Map<Integer, HttpRequestResponsePair>) payloads.get(0).get("data");
        assertSame(request, data.get(42).getOriginalRequest());
        assertSame(response, data.get(42).getOriginalResponse());
        map.close();
    }

    private static MonitoredHashMap<Integer, HttpRequestResponsePair> testMap(
            List<Map<String, Object>> payloads) {
        return new MonitoredHashMap<>(1, 1, TimeUnit.DAYS,
                mock(burp.utils.SessionManager.class), mock(LogController.class), "test",
                mock(ConfigurationController.class)) {
            @Override
            public HttpRequestResponsePair put(Integer key, HttpRequestResponsePair value) {
                payloads.add(Map.of("data", Map.of(key, value)));
                return null;
            }
        };
    }
}
