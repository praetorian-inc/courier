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

package burp.controller;

import burp.model.Webflow;
import burp.serialization.dto.HttpResponseDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WebflowJsonFormatterTest {
    @Test
    void returnsPrettyPrintedWebflowJson() {
        Webflow webflow = new Webflow("name", "description", "project", "https://example.test");
        webflow.addUncorrelatedResponse(new HttpResponseDto(
                "response-body", 1, true, "GET", "/", List.of(),
                "https://example.test", 200));

        String json = new WebflowJsonFormatter().format(webflow);

        String lineSeparator = System.lineSeparator();
        assertTrue(json.startsWith("{" + lineSeparator));
        assertTrue(json.contains(lineSeparator + "  \"name\" : \"name\""));
        assertTrue(json.contains("\"body\" : \"response-body\""));
    }

    @Test
    void boundsJsonPreviewMemoryForLargeCapturedBodies() {
        Webflow webflow = new Webflow("name", "description", "project", "https://example.test");
        webflow.addUncorrelatedResponse(new HttpResponseDto(
                "A".repeat(5_000_000), 1, true, "GET", "/", List.of(),
                "https://example.test", 200));

        String json = new WebflowJsonFormatter().format(webflow);

        assertTrue(json.length() < 50_000);
        assertTrue(json.contains("\"bodyLength\" : 5000000"));
        assertTrue(json.contains("\"bodyTruncated\" : true"));
        assertTrue(json.contains("Display preview only"));
    }
}
