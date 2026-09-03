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

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.model.HttpRequestResponsePair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlannerRequestPayloadFactoryTest {
    @Test
    void truncatesBase64BodiesAtAValidEncodingBoundary() throws Exception {
        List<HttpRequestResponsePair> requests = new java.util.ArrayList<>();
        requests.add(new HttpRequestResponsePair()
                .setOriginalRequest(requestWithBody(1, 1), 1L));
        for (int index = 0; index < 3; index++) {
            HttpRequestResponsePair pair = new HttpRequestResponsePair()
                    .setOriginalRequest(requestWithBody(
                            PlannerRequestPayloadFactory.MAX_BODY_BYTES_PER_MESSAGE + 1,
                            PlannerRequestPayloadFactory.MAX_BODY_BYTES_PER_MESSAGE), 1L);
            pair.wasRequestBodyBase64Encoded = true;
            requests.add(pair);
        }
        HttpRequestResponsePair finalPair = new HttpRequestResponsePair()
                .setOriginalRequest(requestWithBody(
                        PlannerRequestPayloadFactory.MAX_BODY_BYTES_PER_MESSAGE + 1,
                        PlannerRequestPayloadFactory.MAX_BODY_BYTES_PER_MESSAGE - 4), 1L);
        finalPair.wasRequestBodyBase64Encoded = true;
        requests.add(finalPair);

        JsonNode payload = new ObjectMapper().valueToTree(
                new PlannerRequestPayloadFactory().create(requests));
        String encodedBody = payload.get(4).get("originalRequest").get("body").asText();

        assertEquals(0, encodedBody.length() % 4);
        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(encodedBody));
    }

    @Test
    void preservesHeadersWithoutSerializingAnOversizedResponseBody() throws Exception {
        HttpHeader referer = mock(HttpHeader.class);
        when(referer.name()).thenReturn("Referer");
        when(referer.value()).thenReturn("https://workspace.example.test/");
        ByteArray emptyRequestBody = mock(ByteArray.class);
        when(emptyRequestBody.length()).thenReturn(0);
        HttpRequest request = mock(HttpRequest.class);
        when(request.body()).thenReturn(emptyRequestBody);
        when(request.headers()).thenReturn(List.of(referer));
        when(request.method()).thenReturn("GET");
        when(request.path()).thenReturn("/calendar");
        when(request.url()).thenReturn("https://calendar.example.test/calendar");

        ByteArray fullResponseBody = mock(ByteArray.class);
        ByteArray responseSample = mock(ByteArray.class);
        when(fullResponseBody.length()).thenReturn(5 * 1024 * 1024);
        when(fullResponseBody.subArray(0,
                PlannerRequestPayloadFactory.MAX_BODY_BYTES_PER_MESSAGE)).thenReturn(responseSample);
        byte[] responseBytes = new byte[PlannerRequestPayloadFactory.MAX_BODY_BYTES_PER_MESSAGE];
        java.util.Arrays.fill(responseBytes, (byte) 'A');
        when(responseSample.getBytes()).thenReturn(responseBytes);
        HttpResponse response = mock(HttpResponse.class);
        when(response.body()).thenReturn(fullResponseBody);
        when(response.headerValue("Content-Type")).thenReturn("text/html");
        when(response.headers()).thenReturn(List.of());
        when(response.statusCode()).thenReturn((short) 200);

        Object payload = new PlannerRequestPayloadFactory().create(List.of(
                new HttpRequestResponsePair()
                        .setOriginalRequest(request, 1L)
                        .setOriginalResponse(response, 2L)));
        String json = new ObjectMapper().writeValueAsString(payload);
        JsonNode root = new ObjectMapper().readTree(json);

        assertEquals("https://workspace.example.test/",
                root.get("originalRequest").get("headers").get(0).get("Referer").asText());
        assertTrue(root.get("originalResponse").get("bodyTruncated").asBoolean());
        assertEquals(5 * 1024 * 1024,
                root.get("originalResponse").get("bodyLength").asInt());
        assertTrue(json.length() < 100_000);
        verify(fullResponseBody, never()).getBytes();
    }

    private static HttpRequest requestWithBody(int bodyLength, int sampleLength) {
        ByteArray body = mock(ByteArray.class);
        when(body.length()).thenReturn(bodyLength);
        byte[] bytes = new byte[sampleLength];
        java.util.Arrays.fill(bytes, (byte) 'A');
        if (sampleLength == bodyLength) {
            when(body.getBytes()).thenReturn(bytes);
        } else {
            ByteArray sample = mock(ByteArray.class);
            when(sample.getBytes()).thenReturn(bytes);
            when(body.subArray(0, sampleLength)).thenReturn(sample);
        }
        HttpRequest request = mock(HttpRequest.class);
        when(request.body()).thenReturn(body);
        when(request.headerValue("Content-Type")).thenReturn("text/plain");
        when(request.headers()).thenReturn(List.of());
        return request;
    }
}
