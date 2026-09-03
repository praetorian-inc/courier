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
import burp.api.montoya.organizer.OrganizerItem;
import burp.controller.ConfigurationController;
import burp.controller.LogController;
import burp.serialization.dto.OrganizerItemDto;
import burp.serialization.serializer.OrganizerItemSerializer;
import burp.utils.MonitoredHashMap;
import burp.utils.SessionManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrganizerHandler implements AutoCloseable {
    private static final long INTERVAL_SECONDS = 30;

    private final MontoyaApi api;
    private final ConfigurationController configuration;
    private final LogController logger;
    private final ConcurrentHashMap<Integer, String> processedItems = new ConcurrentHashMap<>();

    private volatile ScheduledExecutorService scheduler;
    private volatile MonitoredHashMap<String, OrganizerItemDto> organizerItems;

    public OrganizerHandler(MontoyaApi api, ConfigurationController configuration, LogController logger) {
        this.api = api;
        this.configuration = configuration;
        this.logger = logger;
    }

    public synchronized void start(SessionManager sessionManager) {
        close();
        organizerItems = new MonitoredHashMap<>(
                512, INTERVAL_SECONDS, TimeUnit.SECONDS, sessionManager, logger, "organizer", configuration);
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "OrganizerHandler-Scheduler");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::retrieveAndStoreItems, 0, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void retrieveAndStoreItems() {
        MonitoredHashMap<String, OrganizerItemDto> destination = organizerItems;
        if (destination == null) {
            return;
        }
        try {
            List<OrganizerItem> items = api.organizer().items();
            if (items == null) {
                return;
            }
            Map<Integer, OrganizerItemDto> currentDtos = new LinkedHashMap<>();
            Map<Integer, String> currentFingerprints = new LinkedHashMap<>();
            for (OrganizerItem item : items) {
                OrganizerItemDto dto = OrganizerItemSerializer.toDto(item);
                if (dto == null || dto.getId() == null || dto.getId().isEmpty()) {
                    continue;
                }
                int itemId = item.id();
                currentDtos.put(itemId, dto);
                currentFingerprints.put(itemId,
                        burp.serialization.util.JsonMapperUtil.getConfiguredMapper()
                                .writeValueAsString(dto));
            }

            synchronized (this) {
                if (destination != organizerItems) {
                    return;
                }
                Set<String> currentIds = currentDtos.values().stream()
                        .map(OrganizerItemDto::getId)
                        .collect(Collectors.toSet());
                destination.keySet().removeIf(id -> !currentIds.contains(id));
                processedItems.keySet().retainAll(currentDtos.keySet());

                currentDtos.forEach((itemId, dto) -> {
                    String fingerprint = currentFingerprints.get(itemId);
                    if (!fingerprint.equals(processedItems.get(itemId))) {
                        destination.put(dto.getId(), dto);
                        processedItems.put(itemId, fingerprint);
                    }
                });
            }
        } catch (Exception exception) {
            logger.logError("OrganizerHandler: Error retrieving items: " + exception.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        ScheduledExecutorService currentScheduler = scheduler;
        scheduler = null;
        if (currentScheduler != null) {
            currentScheduler.shutdownNow();
        }
        MonitoredHashMap<String, OrganizerItemDto> currentItems = organizerItems;
        organizerItems = null;
        if (currentItems != null) {
            currentItems.close();
        }
        processedItems.clear();
    }

    public void cleanup() {
        close();
    }
}
