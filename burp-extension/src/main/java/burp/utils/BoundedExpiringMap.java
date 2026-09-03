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

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

public final class BoundedExpiringMap<K, V> extends AbstractMap<K, V> {
    private static final int DEFAULT_MAX_ENTRIES = 4_096;
    private static final long DEFAULT_TTL_MINUTES = 5;

    private final ConcurrentHashMap<K, TimedValue<V>> data = new ConcurrentHashMap<>();
    private final int maximumEntries;
    private final long ttlNanos;
    private final LongSupplier ticker;

    public BoundedExpiringMap() {
        this(DEFAULT_MAX_ENTRIES, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES, System::nanoTime);
    }

    BoundedExpiringMap(int maximumEntries, long ttl, TimeUnit timeUnit, LongSupplier ticker) {
        this.maximumEntries = maximumEntries;
        this.ttlNanos = timeUnit.toNanos(ttl);
        this.ticker = ticker;
    }

    @Override
    public V put(K key, V value) {
        long now = ticker.getAsLong();
        TimedValue<V> previous = data.put(key, new TimedValue<>(value, now));
        evict(now);
        return liveValue(previous, now);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        while (true) {
            long now = ticker.getAsLong();
            TimedValue<V> current = data.get(key);
            if (current != null && !isExpired(current, now)) {
                return current.value();
            }
            if (current != null) {
                data.remove(key, current);
                continue;
            }
            if (data.putIfAbsent(key, new TimedValue<>(value, now)) == null) {
                evict(now);
                return null;
            }
        }
    }

    @Override
    public V get(Object key) {
        long now = ticker.getAsLong();
        TimedValue<V> value = data.get(key);
        if (value != null && isExpired(value, now)) {
            data.remove(key, value);
            return null;
        }
        return value == null ? null : value.value();
    }

    @Override
    public V remove(Object key) {
        return liveValue(data.remove(key), ticker.getAsLong());
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public int size() {
        evict(ticker.getAsLong());
        return data.size();
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        long now = ticker.getAsLong();
        evict(now);
        data.forEach((key, value) -> action.accept(key, value.value()));
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        long now = ticker.getAsLong();
        evict(now);
        Set<Entry<K, V>> entries = new HashSet<>();
        data.forEach((key, value) -> entries.add(new SimpleImmutableEntry<>(key, value.value())));
        return Set.copyOf(entries);
    }

    private void evict(long now) {
        data.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
        while (data.size() > maximumEntries) {
            Map.Entry<K, TimedValue<V>> oldest = data.entrySet().stream()
                    .min(Map.Entry.comparingByValue(
                            java.util.Comparator.comparingLong(TimedValue::createdAtNanos)))
                    .orElse(null);
            if (oldest == null || !data.remove(oldest.getKey(), oldest.getValue())) {
                break;
            }
        }
    }

    private V liveValue(TimedValue<V> value, long now) {
        return value == null || isExpired(value, now) ? null : value.value();
    }

    private boolean isExpired(TimedValue<V> value, long now) {
        return now - value.createdAtNanos() >= ttlNanos;
    }

    private record TimedValue<V>(V value, long createdAtNanos) {
    }
}
