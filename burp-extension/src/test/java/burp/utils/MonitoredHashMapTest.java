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

import burp.controller.LogController;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.model.HttpRequestResponsePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MonitoredHashMapTest {
    private final List<MonitoredHashMap<?, ?>> maps = new ArrayList<>();

    @AfterEach
    void closeMaps() {
        maps.forEach(MonitoredHashMap::close);
    }

    @Test
    void uploadsAtomicBatchesWithoutClearingNewEntries() {
        List<Map<String, Object>> payloads = new ArrayList<>();
        CompletableFuture<Boolean> firstUpload = new CompletableFuture<>();
        CompletableFuture<Boolean> secondUpload = new CompletableFuture<>();
        MonitoredHashMap.BatchUploader uploader = (payload, reason) -> {
            payloads.add(payload);
            return payloads.size() == 1 ? firstUpload : secondUpload;
        };
        MonitoredHashMap<Integer, String> map = createMap(uploader);

        map.put(1, "first");
        map.put(2, "second");
        assertEquals(1, payloads.size());

        firstUpload.complete(true);
        await(() -> payloads.size() == 2);

        assertEquals(Map.of(1, "first"), payloads.get(0).get("data"));
        assertEquals(Map.of(2, "second"), payloads.get(1).get("data"));
        secondUpload.complete(true);
    }

    @Test
    void tracksNestedSyncWhenFirstUploadCompletesSynchronously() {
        AtomicReference<MonitoredHashMap<Integer, String>> mapReference = new AtomicReference<>();
        AtomicInteger uploads = new AtomicInteger();
        CompletableFuture<Boolean> secondUpload = new CompletableFuture<>();
        MonitoredHashMap.BatchUploader uploader = (payload, reason) -> {
            if (uploads.incrementAndGet() == 1) {
                mapReference.get().put(2, "second");
                return CompletableFuture.completedFuture(true);
            }
            return secondUpload;
        };
        MonitoredHashMap<Integer, String> map = createMap(uploader);
        mapReference.set(map);

        map.put(1, "first");

        assertEquals(2, uploads.get());
        assertFalse(map.currentSync().isDone());
        secondUpload.complete(true);
    }

    @Test
    void restoresFailedBatchForScheduledRetry() {
        MonitoredHashMap<Integer, String> map = createMap(
                (payload, reason) -> CompletableFuture.completedFuture(false));

        map.put(7, "retry-me");
        await(() -> map.containsKey(7));

        assertEquals("retry-me", map.get(7));
    }

    @Test
    void readsMetadataAtUploadTime() {
        AtomicBoolean trainingEnabled = new AtomicBoolean(true);
        List<Map<String, Object>> payloads = new ArrayList<>();
        MonitoredHashMap<Integer, String> map = new MonitoredHashMap<>(
                1, 1, TimeUnit.DAYS,
                () -> Map.of("ai_enabled", trainingEnabled.get()), () -> true,
                (payload, reason) -> {
                    payloads.add(payload);
                    return CompletableFuture.completedFuture(true);
                }, mock(LogController.class));
        maps.add(map);

        map.put(1, "first");
        trainingEnabled.set(false);
        map.put(2, "second");

        assertEquals(true, ((Map<?, ?>) payloads.get(0).get("metadata")).get("ai_enabled"));
        assertEquals(false, ((Map<?, ?>) payloads.get(1).get("metadata")).get("ai_enabled"));
    }

    @Test
    void mergesFailedRequestHalfWithResponseThatArrivedDuringUpload() {
        CompletableFuture<Boolean> upload = new CompletableFuture<>();
        MonitoredHashMap<Integer, HttpRequestResponsePair> map = new MonitoredHashMap<>(
                1, 1, TimeUnit.DAYS, Map.of("mapType", "test"), () -> true,
                (payload, reason) -> upload, mock(LogController.class));
        map.setFailureMerger(HttpRequestResponsePair::mergeFailedBatch);
        maps.add(map);
        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        map.put(9, new HttpRequestResponsePair().setOriginalRequest(request, 1));
        HttpRequestResponsePair current =
                new HttpRequestResponsePair().setOriginalResponse(response, 2);
        map.put(9, current);
        upload.complete(false);
        await(() -> map.get(9) != null && map.get(9).getOriginalRequest() != null);

        assertNotSame(current, map.get(9));
        assertNull(current.getOriginalRequest());
        assertSame(request, map.get(9).getOriginalRequest());
        assertSame(response, map.get(9).getOriginalResponse());
    }

    @Test
    void retriesFailedShutdownBatch() {
        AtomicInteger attempts = new AtomicInteger();
        MonitoredHashMap<Integer, String> map = new MonitoredHashMap<>(
                10, 1, TimeUnit.DAYS, Map.of("mapType", "test"), () -> true,
                (payload, reason) -> {
                    attempts.incrementAndGet();
                    return CompletableFuture.completedFuture(false);
                }, mock(LogController.class));
        maps.add(map);
        map.put(1, "retry");

        map.close();

        assertEquals(3, attempts.get());
        assertEquals("retry", map.get(1));
    }

    @Test
    void shutdownSnapshotIncludesTheBatchCurrentlyBeingUploaded() {
        CompletableFuture<Boolean> upload = new CompletableFuture<>();
        MonitoredHashMap<Integer, String> map = createMap((payload, reason) -> upload);

        map.put(1, "uploading");

        assertEquals(Map.of(1, "uploading"), map.shutdownSnapshot());
        upload.complete(true);
    }

    @Test
    void closeWaitsForFinalUpload() throws Exception {
        CompletableFuture<Boolean> upload = new CompletableFuture<>();
        MonitoredHashMap<Integer, String> map = new MonitoredHashMap<>(
                10, 1, TimeUnit.DAYS, Map.of("mapType", "test"), () -> true,
                (payload, reason) -> upload, mock(LogController.class));
        maps.add(map);
        map.put(1, "final");

        CompletableFuture<Void> close = CompletableFuture.runAsync(map::close);
        await(() -> !map.currentSync().isDone());
        assertFalse(close.isDone());
        upload.complete(true);
        close.get(2, TimeUnit.SECONDS);

        assertTrue(map.isEmpty());
    }

    private MonitoredHashMap<Integer, String> createMap(MonitoredHashMap.BatchUploader uploader) {
        MonitoredHashMap<Integer, String> map = new MonitoredHashMap<>(
                1, 1, TimeUnit.DAYS, Map.of("mapType", "test"), () -> true,
                uploader, mock(LogController.class));
        maps.add(map);
        return map;
    }

    private static void await(java.util.function.BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertTrue(condition.getAsBoolean(), "condition was not met before timeout");
    }
}
