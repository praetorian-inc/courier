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

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlannerRequestTableModelTest {
    @Test
    void usesBodyLengthWithoutConvertingBodyToText() {
        HttpRequest request = mock(HttpRequest.class);
        ByteArray body = mock(ByteArray.class);
        when(request.method()).thenReturn("POST");
        when(request.url()).thenReturn("https://example.test");
        when(request.headers()).thenReturn(List.of());
        when(request.body()).thenReturn(body);
        when(body.length()).thenReturn(4096);
        HttpRequestResponsePair pair = new HttpRequestResponsePair().setOriginalRequest(request, 123L);

        PlannerRequestTableModel model = new PlannerRequestTableModel();
        model.addRequest(pair);

        assertEquals(8, model.getColumnCount());
        assertEquals("", model.getValueAt(0, 0));
        assertTrue(model.getValueAt(0, 1).toString().matches("\\d{2}:\\d{2}:\\d{2}"));
        assertEquals(4096, model.getValueAt(0, 7));
        verify(request, never()).bodyToString();
        assertEquals(List.of(pair), model.getAllRequests());
    }

    @Test
    void doesNotReplaceUnknownCaptureTimeWithPlannerInsertionTime() {
        HttpRequestResponsePair pair = new HttpRequestResponsePair();
        PlannerRequestTableModel model = new PlannerRequestTableModel();

        model.addRequest(pair);

        assertEquals("—", model.getValueAt(0, 1));
        assertEquals(0, pair.originalRequestTime);
    }
}
