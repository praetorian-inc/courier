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
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.internal.MontoyaObjectFactory;
import burp.api.montoya.internal.ObjectFactoryLocator;
import burp.api.montoya.proxy.MessageReceivedAction;
import burp.api.montoya.proxy.MessageToBeSentAction;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.InterceptedResponse;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;
import burp.api.montoya.proxy.http.ProxyResponseReceivedAction;
import burp.api.montoya.proxy.http.ProxyResponseToBeSentAction;
import burp.controller.ConfigurationController;
import burp.controller.ConnectionController;
import burp.controller.LogController;
import burp.model.HttpRequestResponsePair;
import burp.utils.MonitoredHashMap;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProxyHandlerTest {
    @Test
    void publishesProxyRequestAndResponseTogether() {
        ConfigurationController configuration = mock(ConfigurationController.class);
        ConnectionController connection = mock(ConnectionController.class);
        when(connection.isEnabled()).thenReturn(true);
        MontoyaApi api = mock(MontoyaApi.class, RETURNS_DEEP_STUBS);
        ProxyHandler handler = new ProxyHandler(
                configuration, api, mock(LogController.class), connection);
        AtomicReference<HttpRequestResponsePair> published = new AtomicReference<>();
        MonitoredHashMap<Integer, HttpRequestResponsePair> destination = capturingMap(published);
        handler.setRequestResponses(destination);

        ByteArray bytes = mock(ByteArray.class);
        when(bytes.getBytes()).thenReturn(new byte[] {1});
        Annotations annotations = mock(Annotations.class);
        InterceptedRequest request = mock(InterceptedRequest.class);
        when(request.messageId()).thenReturn(7);
        when(request.url()).thenReturn("https://example.test");
        when(request.fileExtension()).thenReturn("");
        when(request.headerValue("Content-Type")).thenReturn("text/plain");
        when(request.annotations()).thenReturn(annotations);
        when(request.toByteArray()).thenReturn(bytes);
        when(annotations.notes()).thenReturn("");
        when(annotations.hasHighlightColor()).thenReturn(false);
        InterceptedResponse response = mock(InterceptedResponse.class);
        when(response.messageId()).thenReturn(7);
        when(response.initiatingRequest()).thenReturn(request);
        when(response.annotations()).thenReturn(annotations);
        when(response.headerValue("Content-Type")).thenReturn("text/plain");
        when(response.toByteArray()).thenReturn(bytes);
        when(api.proxy().isInterceptEnabled()).thenReturn(false);

        MontoyaObjectFactory previousFactory = ObjectFactoryLocator.FACTORY;
        MontoyaObjectFactory factory = mock(MontoyaObjectFactory.class);
        ProxyRequestReceivedAction requestReceivedAction = mock(ProxyRequestReceivedAction.class);
        ProxyRequestToBeSentAction requestToBeSentAction = mock(ProxyRequestToBeSentAction.class);
        ProxyResponseReceivedAction responseReceivedAction = mock(ProxyResponseReceivedAction.class);
        ProxyResponseToBeSentAction responseToBeSentAction = mock(ProxyResponseToBeSentAction.class);
        when(requestReceivedAction.action()).thenReturn(MessageReceivedAction.CONTINUE);
        when(requestToBeSentAction.action()).thenReturn(MessageToBeSentAction.CONTINUE);
        when(responseReceivedAction.action()).thenReturn(MessageReceivedAction.CONTINUE);
        when(responseToBeSentAction.action()).thenReturn(MessageToBeSentAction.CONTINUE);
        when(factory.requestInitialInterceptResultFollowUserRules(request))
                .thenReturn(requestReceivedAction);
        when(factory.requestFinalInterceptResultContinueWith(request))
                .thenReturn(requestToBeSentAction);
        when(factory.responseInitialInterceptResultFollowUserRules(response))
                .thenReturn(responseReceivedAction);
        when(factory.responseFinalInterceptResultContinueWith(response))
                .thenReturn(responseToBeSentAction);
        ObjectFactoryLocator.FACTORY = factory;
        try {
            assertEquals(MessageReceivedAction.CONTINUE,
                    handler.handleRequestReceived(request).action());
            assertEquals(MessageToBeSentAction.CONTINUE,
                    handler.handleRequestToBeSent(request).action());
            assertNull(published.get());
            assertEquals(MessageReceivedAction.CONTINUE,
                    handler.handleResponseReceived(response).action());
            assertEquals(MessageToBeSentAction.CONTINUE,
                    handler.handleResponseToBeSent(response).action());
        } finally {
            ObjectFactoryLocator.FACTORY = previousFactory;
        }

        assertNotNull(published.get());
        assertSame(request, published.get().getOriginalRequest());
        assertSame(response, published.get().getOriginalResponse());
        destination.close();
    }

    private static MonitoredHashMap<Integer, HttpRequestResponsePair> capturingMap(
            AtomicReference<HttpRequestResponsePair> published) {
        return new MonitoredHashMap<>(1, 1, TimeUnit.DAYS,
                mock(burp.utils.SessionManager.class), mock(LogController.class), "test",
                mock(ConfigurationController.class)) {
            @Override
            public HttpRequestResponsePair put(Integer key, HttpRequestResponsePair value) {
                return published.getAndSet(value);
            }
        };
    }
}
