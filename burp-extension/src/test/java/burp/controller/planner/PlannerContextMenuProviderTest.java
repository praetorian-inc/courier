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

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PlannerContextMenuProviderTest {
    @Test
    void fallsBackToResponseDateWhenBurpTimingDataIsUnavailable() {
        HttpRequestResponse pair = mock(HttpRequestResponse.class);
        HttpResponse response = mock(HttpResponse.class);
        when(pair.timingData()).thenReturn(Optional.empty());
        when(pair.response()).thenReturn(response);
        when(response.headerValue("Date")).thenReturn("Tue, 1 Sep 2026 07:22:25 GMT");

        long timestamp = PlannerContextMenuProvider.captureTimestamp(pair);

        assertEquals(ZonedDateTime.parse("Tue, 1 Sep 2026 07:22:25 GMT",
                DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli(), timestamp);
    }
}
