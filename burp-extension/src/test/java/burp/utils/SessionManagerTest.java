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

package burp.utils;

import burp.api.montoya.MontoyaApi;
import burp.model.NetworkInterface;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionManagerTest {
    @Test
    void validatesJwtExpirationWithSafetyWindow() {
        assertTrue(SessionManager.isJwtValid(jwt(Instant.now().plusSeconds(3600).getEpochSecond())));
        assertFalse(SessionManager.isJwtValid(jwt(Instant.now().minusSeconds(1).getEpochSecond())));
        assertFalse(SessionManager.isJwtValid("not-a-jwt"));
        assertFalse(SessionManager.isJwtValid(null));
    }

    @Test
    void bracketsSpecificIpv6ProxyListenerAddresses() throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, RETURNS_DEEP_STUBS);
        when(api.burpSuite().exportProjectOptionsAsJson("proxy.request_listeners"))
                .thenReturn("""
                        {"proxy":{"request_listeners":[{
                          "running":true,
                          "listener_port":8080,
                          "listen_mode":"specific_address",
                          "listen_specific_address":"2001:db8::1"
                        }]}}
                        """);
        SessionManager manager = new SessionManager(
                "https://guard.example.test", "id", "secret", "project", "target", api,
                mock(HttpClient.class));

        Map<NetworkInterface, String> listeners = manager.getProxyServerUrls();

        assertEquals("http://[2001:db8::1]:8080",
                listeners.get(NetworkInterface.SPECIFIC_ADDRESS));
    }

    @Test
    void usesGuardTokenContractOverRequiredTls() throws Exception {
        HttpClient client = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"access_token\":\"token-value\"}");
        when(client.send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(response);
        SessionManager manager = new SessionManager(
                "https://guard.example.test/", "key-id", "secret-value",
                "project", "target", null, client);

        assertEquals("token-value", manager.getChariotToken(mock(burp.controller.LogController.class)));
        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertEquals("POST", request.getValue().method());
        assertEquals("https://guard.example.test/token", request.getValue().uri().toString());
        assertNull(request.getValue().uri().getQuery());
        assertEquals("application/x-www-form-urlencoded",
                request.getValue().headers().firstValue("Content-Type").orElseThrow());
        assertTrue(request.getValue().bodyPublisher().isPresent());
        assertThrows(IllegalArgumentException.class, () -> new SessionManager(
                "http://guard.example.test", "id", "secret", "project", "target", null, client));
    }

    private static String jwt(long expiration) {
        String header = encode("{\"alg\":\"none\"}");
        String payload = encode("{\"exp\":" + expiration + "}");
        return header + "." + payload + ".signature";
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
