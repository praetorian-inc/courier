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
import burp.view.WebflowRecorderView;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebflowRecorderControllerTest {
    @Test
    void listModelIsTheSingleWebflowSourceOfTruth() {
        LogController logger = mock(LogController.class);
        WebflowRecorderView view = new WebflowRecorderView(logger);
        WebflowRecorderController controller = new WebflowRecorderController(view, logger);
        view.setWebflowRecorderController(controller);
        assertTrue(controller.getWebflows().isEmpty());

        Webflow webflow = new Webflow("name", "description", "project", "https://example.test");
        controller.getWebflowListModel().addElement(webflow);
        assertEquals(java.util.List.of(webflow), controller.getWebflows());

        controller.clearAllWebflows();
        assertTrue(controller.getWebflows().isEmpty());
        controller.close();
    }

    @Test
    void reportsRecordingCompletedWhenRecorderStops() {
        WebflowRecorderView view = mock(WebflowRecorderView.class);
        WebflowRecorderController controller = new WebflowRecorderController(
                view, mock(LogController.class));
        AtomicReference<String> status = new AtomicReference<>();
        controller.setStatusUpdateCallback(status::set);

        controller.onRecordingCompleted();

        assertEquals("Recording completed", status.get());
        controller.close();
    }

    @Test
    void rendersJsonOffTheUiThreadAndExposesLoadingState() throws Exception {
        LogController logger = mock(LogController.class);
        WebflowRecorderView view = mock(WebflowRecorderView.class);
        WebflowRecorderController controller = new WebflowRecorderController(view, logger);
        SlowWebflow webflow = new SlowWebflow();
        controller.getWebflowListModel().addElement(webflow);
        controller.handleWebflowSelectionChange(new int[] {0});
        clearInvocations(view);
        AtomicBoolean displayUpdatedOnEdt = new AtomicBoolean();
        doAnswer(invocation -> {
            displayUpdatedOnEdt.set(SwingUtilities.isEventDispatchThread());
            return null;
        }).when(view).showJsonText(anyString());
        webflow.slowNextStepsRead.set(true);

        CompletableFuture<Void> dispatch = CompletableFuture.runAsync(
                () -> controller.handleViewModeChange("JSON"));
        assertTrue(webflow.jsonReadStarted.await(3, TimeUnit.SECONDS));
        try {
            dispatch.get(3, TimeUnit.SECONDS);
        } finally {
            webflow.releaseJsonRead.countDown();
        }

        verify(view).setJsonLoading(true);
        verify(view, timeout(3000)).showJsonText(anyString());
        verify(view, timeout(3000)).setJsonLoading(false);
        assertFalse(webflow.jsonReadOnEventDispatchThread.get());
        assertTrue(webflow.jsonThreadName.get().startsWith("Courier-Webflow-JSON-"));
        assertTrue(displayUpdatedOnEdt.get());
        controller.close();
    }

    private static final class SlowWebflow extends Webflow {
        private final AtomicBoolean slowNextStepsRead = new AtomicBoolean();
        private final AtomicBoolean jsonReadOnEventDispatchThread = new AtomicBoolean(true);
        private final AtomicReference<String> jsonThreadName = new AtomicReference<>("");
        private final CountDownLatch jsonReadStarted = new CountDownLatch(1);
        private final CountDownLatch releaseJsonRead = new CountDownLatch(1);

        private SlowWebflow() {
            super("name", "description", "project", "https://example.test");
        }

        @Override
        public java.util.List<WebflowStep> getSteps() {
            if (slowNextStepsRead.compareAndSet(true, false)) {
                jsonReadOnEventDispatchThread.set(SwingUtilities.isEventDispatchThread());
                jsonThreadName.set(Thread.currentThread().getName());
                jsonReadStarted.countDown();
                try {
                    releaseJsonRead.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
            return super.getSteps();
        }
    }
}
