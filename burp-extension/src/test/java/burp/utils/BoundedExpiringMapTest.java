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

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class BoundedExpiringMapTest {
    @Test
    void evictsOldestAndExpiredCorrelationEntries() {
        AtomicLong ticker = new AtomicLong();
        BoundedExpiringMap<Integer, String> map =
                new BoundedExpiringMap<>(2, 10, TimeUnit.NANOSECONDS, ticker::get);

        map.put(1, "first");
        ticker.incrementAndGet();
        map.put(2, "second");
        ticker.incrementAndGet();
        map.put(3, "third");

        assertNull(map.get(1));
        assertEquals(2, map.size());
        ticker.set(12);
        assertTrue(map.isEmpty());
    }
}
