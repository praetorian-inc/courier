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

package burp.controller.sessionRecorder;

import burp.controller.LogController;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.Sizes;
import com.microsoft.playwright.options.Timing;
import burp.model.Webflow;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlaywrightNetworkCaptureTest {
    @Test
    void usesCompleteHeadersAndBinaryRequestBuffer() {
        Request request = mock(Request.class);
        when(request.method()).thenReturn("POST");
        when(request.url()).thenReturn("https://example.test/upload");
        when(request.allHeaders()).thenReturn(Map.of(
                "content-type", "application/octet-stream",
                "content-length", "3",
                "cookie", "session=value"));
        byte[] body = {(byte) 0xff, 0, 1};
        when(request.postDataBuffer()).thenReturn(body);

        var dto = Utils.convertPlaywrightRequestToDto(request, mock(LogController.class));

        assertEquals(Base64.getEncoder().encodeToString(body), dto.getBody());
        assertTrue(dto.getHeaders().stream().anyMatch(header -> header.containsKey("cookie")));
        verify(request, never()).headers();
        verify(request, never()).postData();
    }

    @Test
    void retainsFailedRequestsWithoutInventingResponses() {
        Request request = mock(Request.class);
        when(request.method()).thenReturn("GET");
        when(request.url()).thenReturn("https://example.test/failure");
        when(request.allHeaders()).thenReturn(Map.of());
        Timing timing = new Timing();
        timing.startTime = 100;
        timing.requestStart = 10;
        timing.responseStart = -1;
        when(request.timing()).thenReturn(timing);
        when(request.response()).thenReturn(null);
        Webflow webflow = new Webflow("name", "description", "project",
                "https://example.test");

        Utils.correlateRequestResponseWithSteps(request, mock(LogController.class), webflow);

        assertEquals(1, webflow.getUncorrelatedRequests().size());
        assertTrue(webflow.getUncorrelatedResponses().isEmpty());
    }

    @Test
    void treatsMissingTimingAsUncorrelatedInsteadOfFailing() {
        Request request = mock(Request.class);
        when(request.method()).thenReturn("GET");
        when(request.url()).thenReturn("https://example.test/no-timing");
        when(request.allHeaders()).thenReturn(Map.of());
        when(request.timing()).thenReturn(null);
        Webflow webflow = new Webflow("name", "description", "project",
                "https://example.test");
        webflow.addStep(new Webflow.WebflowStep(
                Webflow.WebflowStep.StepType.CLICK, "#button", "", "Click", 1));

        assertDoesNotThrow(() -> Utils.correlateRequestResponseWithSteps(
                request, mock(LogController.class), webflow));

        assertEquals(1, webflow.getUncorrelatedRequests().size());
        assertTrue(webflow.getSteps().get(0).getCorrelatedRequests().isEmpty());
    }

    @Test
    void preservesResponseRequestMethodAndSkipsKnownOversizedBody() {
        Request request = mock(Request.class);
        Sizes sizes = new Sizes();
        sizes.responseBodySize = 2 * 1024 * 1024;
        when(request.method()).thenReturn("PATCH");
        when(request.sizes()).thenReturn(sizes);
        Response response = mock(Response.class);
        when(response.request()).thenReturn(request);
        when(response.url()).thenReturn("https://example.test/result");
        when(response.allHeaders()).thenReturn(Map.of(
                "content-type", "text/plain",
                "content-length", Integer.toString(2 * 1024 * 1024)));
        when(response.status()).thenReturn(200);

        var dto = Utils.convertPlaywrightResponseToDto(response, mock(LogController.class));

        assertEquals("PATCH", dto.getMethod());
        assertEquals("", dto.getBody());
        verify(response, never()).body();
        verify(response, never()).headers();
    }
}
