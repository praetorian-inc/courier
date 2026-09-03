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

import burp.controller.ConfigurationController;
import burp.controller.LogController;
import burp.serialization.util.JsonMapperUtil;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.BinaryOperator;

public class MonitoredHashMap<K, V> implements Map<K, V>, AutoCloseable {
    private static final int MAX_SHUTDOWN_ATTEMPTS = 3;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30;
    public enum SyncReason {
        SIZE_THRESHOLD_EXCEEDED,
        SCHEDULED_SYNC,
        SHUTDOWN_SYNC
    }

    @FunctionalInterface
    public interface BatchUploader {
        CompletableFuture<Boolean> upload(Map<String, Object> payload, SyncReason reason);
    }

    private final ConcurrentHashMap<K, V> data;
    private final Supplier<Map<String, Object>> metadataSupplier;
    private final int sizeThreshold;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean syncInProgress = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<CompletableFuture<Void>> inFlight =
            new AtomicReference<>(CompletableFuture.completedFuture(null));
    private final AtomicReference<Map<K, V>> activeBatch = new AtomicReference<>();
    private final BooleanSupplier uploadEnabled;
    private final BatchUploader uploader;
    private final LogController logger;
    private final Consumer<Map<String, Object>> shutdownFailureHandler;
    private volatile BinaryOperator<V> failureMerger = (failed, current) -> current;
    private volatile Consumer<SyncReason> syncListener = ignored -> { };

    public MonitoredHashMap(int sizeThreshold, long scheduledInterval, TimeUnit timeUnit,
            SessionManager sessionManager, LogController logger, String mapType,
            ConfigurationController configurationController) {
        this(sizeThreshold, scheduledInterval, timeUnit,
                () -> createMetadata(sizeThreshold, scheduledInterval, timeUnit, sessionManager, mapType,
                        configurationController),
                () -> sessionManager != null && sessionManager.isEnabled(),
                createUploader(sessionManager, logger, mapType),
                logger,
                payload -> PendingUploadStore.queue(payload, mapType, sessionManager, logger));
    }

    MonitoredHashMap(int sizeThreshold, long scheduledInterval, TimeUnit timeUnit,
            Map<String, Object> metadata, BooleanSupplier uploadEnabled, BatchUploader uploader,
            LogController logger) {
        this(sizeThreshold, scheduledInterval, timeUnit, () -> metadata,
                uploadEnabled, uploader, logger, ignored -> { });
    }

    MonitoredHashMap(int sizeThreshold, long scheduledInterval, TimeUnit timeUnit,
            Supplier<Map<String, Object>> metadataSupplier, BooleanSupplier uploadEnabled,
            BatchUploader uploader, LogController logger) {
        this(sizeThreshold, scheduledInterval, timeUnit, metadataSupplier,
                uploadEnabled, uploader, logger, ignored -> { });
    }

    private MonitoredHashMap(int sizeThreshold, long scheduledInterval, TimeUnit timeUnit,
            Supplier<Map<String, Object>> metadataSupplier, BooleanSupplier uploadEnabled,
            BatchUploader uploader, LogController logger,
            Consumer<Map<String, Object>> shutdownFailureHandler) {
        this.sizeThreshold = sizeThreshold;
        this.data = new ConcurrentHashMap<>(sizeThreshold + 1);
        this.metadataSupplier = metadataSupplier;
        this.uploadEnabled = uploadEnabled;
        this.uploader = uploader;
        this.logger = logger;
        this.shutdownFailureHandler = shutdownFailureHandler;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "MonitoredHashMap-Scheduler");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(
                () -> triggerSync(SyncReason.SCHEDULED_SYNC),
                scheduledInterval,
                scheduledInterval,
                timeUnit);
    }

    private static Map<String, Object> createMetadata(int sizeThreshold, long scheduledInterval,
            TimeUnit timeUnit, SessionManager sessionManager, String mapType,
            ConfigurationController configuration) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("mapType", mapType);
        metadata.put("sizeThreshold", sizeThreshold);
        metadata.put("scheduledInterval", scheduledInterval);
        metadata.put("timeUnit", timeUnit.name());
        metadata.put("ai_enabled", configuration.isEnableAITrainingEnabled());
        metadata.put("scope_enabled", configuration.isRespectScopeEnabled());
        metadata.put("excluded_extensions", Set.copyOf(configuration.getExcludedExtensions()));
        metadata.put("target_application", sessionManager == null ? "" : sessionManager.getTargetApplication());
        return metadata;
    }

    private static BatchUploader createUploader(SessionManager sessionManager, LogController logger,
            String mapType) {
        return (payload, reason) -> CompletableFuture.supplyAsync(() -> {
            if (sessionManager == null) {
                return null;
            }
            String token = sessionManager.getChariotToken(logger);
            if (token == null || token.isEmpty()) {
                logger.logError("Failed to get Guard token for sync operation");
                return null;
            }
            String projectName = sessionManager.getProjectName().isEmpty()
                    ? "tempProject"
                    : sessionManager.getProjectName();
            String filename = "courier/" + projectName + "/" + Instant.now().toEpochMilli()
                    + "-" + mapType + ".courier";
            try {
                String json = JsonMapperUtil.getConfiguredMapper().writeValueAsString(payload);
                return new UploadRequest(token, filename, json);
            } catch (Exception exception) {
                logger.logError("Failed to serialize sync batch: " + exception.getMessage());
                return null;
            }
        }).thenCompose(uploadRequest -> {
            if (uploadRequest == null) {
                return CompletableFuture.completedFuture(false);
            }
            logger.logInfo("Uploading " + uploadRequest.filename() + " to Guard");
            return Utils.getPresignedURL(uploadRequest.token(), sessionManager.getChariotApiEndpoint(),
                            uploadRequest.filename(), sessionManager.getSelectedAccount(), logger)
                    .thenCompose(url -> url == null || url.isEmpty()
                            ? CompletableFuture.completedFuture(false)
                            : Utils.uploadJsonFile(uploadRequest.json(), url,
                                    "MonitoredHashMap-" + reason.name(), logger));
        });
    }

    public void setSyncListener(Consumer<SyncReason> listener) {
        syncListener = listener == null ? ignored -> { } : listener;
    }

    public void setFailureMerger(BinaryOperator<V> merger) {
        failureMerger = merger == null ? (failed, current) -> current : merger;
    }

    private void triggerSync(SyncReason reason) {
        if (data.isEmpty() || !uploadEnabled.getAsBoolean() || !syncInProgress.compareAndSet(false, true)) {
            return;
        }

        Map<K, V> batch = drainCurrentEntries();
        if (batch.isEmpty()) {
            syncInProgress.set(false);
            return;
        }

        activeBatch.set(batch);
        Map<String, Object> payload = createPayload(batch);

        CompletableFuture<Boolean> upload;
        try {
            upload = uploader.upload(payload, reason);
        } catch (Exception exception) {
            logger.logError("Unable to start sync: " + exception.getMessage());
            upload = CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Void> completion = new CompletableFuture<>();
        inFlight.set(completion);
        upload.whenComplete((successful, throwable) -> {
            try {
                boolean uploadSucceeded = throwable == null && Boolean.TRUE.equals(successful);
                if (!uploadSucceeded) {
                    batch.forEach((key, value) -> data.merge(key, value,
                            (current, restored) -> failureMerger.apply(restored, current)));
                    logger.logError("Failed to upload sync batch triggered by " + reason.name());
                } else {
                    logger.logDebug("Successfully uploaded sync batch triggered by " + reason.name());
                    syncListener.accept(reason);
                }
                syncInProgress.set(false);
                if (uploadSucceeded && !closed.get()
                        && !data.isEmpty() && data.size() >= sizeThreshold) {
                    triggerSync(SyncReason.SIZE_THRESHOLD_EXCEEDED);
                }
            } finally {
                activeBatch.compareAndSet(batch, null);
                completion.complete(null);
            }
        });
    }

    private Map<String, Object> createPayload(Map<K, V> batch) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("metadata", Map.copyOf(metadataSupplier.get()));
        payload.put("data", batch);
        return payload;
    }

    private Map<K, V> drainCurrentEntries() {
        Map<K, V> batch = new HashMap<>();
        data.forEach((key, value) -> {
            if (data.remove(key, value)) {
                batch.put(key, value);
            }
        });
        return batch;
    }

    private void checkThreshold() {
        if (data.size() >= sizeThreshold) {
            triggerSync(SyncReason.SIZE_THRESHOLD_EXCEEDED);
        }
    }

    CompletableFuture<Void> currentSync() {
        return inFlight.get();
    }

    @Override
    public V put(K key, V value) {
        V previous = data.put(key, value);
        checkThreshold();
        return previous;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> values) {
        data.putAll(values);
        checkThreshold();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        V previous = data.putIfAbsent(key, value);
        checkThreshold();
        return previous;
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        V merged = data.merge(key, value, remappingFunction);
        checkThreshold();
        return merged;
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V computed = data.compute(key, remappingFunction);
        checkThreshold();
        return computed;
    }

    @Override
    public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        V computed = data.computeIfAbsent(key, mappingFunction);
        checkThreshold();
        return computed;
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V computed = data.computeIfPresent(key, remappingFunction);
        checkThreshold();
        return computed;
    }

    @Override
    public V get(Object key) {
        return data.get(key);
    }

    @Override
    public V remove(Object key) {
        return data.remove(key);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return data.remove(key, value);
    }

    @Override
    public V replace(K key, V value) {
        return data.replace(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return data.replace(key, oldValue, newValue);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return data.containsValue(value);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public Set<K> keySet() {
        return data.keySet();
    }

    @Override
    public Collection<V> values() {
        return data.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return data.entrySet();
    }

    @Override
    public boolean equals(Object other) {
        return data.equals(other);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        scheduler.shutdownNow();
        if (!uploadEnabled.getAsBoolean()) {
            return;
        }

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(SHUTDOWN_TIMEOUT_SECONDS);
        int attempts = 0;
        while ((!data.isEmpty() || syncInProgress.get())
                && attempts < MAX_SHUTDOWN_ATTEMPTS && System.nanoTime() < deadline) {
            if (!syncInProgress.get()) {
                attempts++;
                triggerSync(SyncReason.SHUTDOWN_SYNC);
            }
            CompletableFuture<Void> pending = inFlight.get();
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                break;
            }
            try {
                pending.get(remainingNanos, TimeUnit.NANOSECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (TimeoutException exception) {
                logger.logError("Timed out waiting for final sync upload");
                break;
            } catch (Exception exception) {
                logger.logError("Unable to complete final sync upload: " + exception.getMessage());
            }
        }
        Map<K, V> unsaved = shutdownSnapshot();
        if (!unsaved.isEmpty()) {
            logger.logError("Unable to upload " + unsaved.size() + " record(s) before shutdown");
            shutdownFailureHandler.accept(createPayload(unsaved));
        }
    }

    Map<K, V> shutdownSnapshot() {
        Map<K, V> unsaved = new LinkedHashMap<>();
        Map<K, V> uploading = activeBatch.get();
        if (uploading != null) {
            unsaved.putAll(uploading);
        }
        data.forEach((key, value) -> unsaved.merge(key, value,
                (failed, current) -> failureMerger.apply(failed, current)));
        return unsaved;
    }

    private record UploadRequest(String token, String filename, String json) {
    }
}
