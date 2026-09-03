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

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafeLogFormatterTest {
    @Test
    void removesPathsQueriesFragmentsAndUserInfoFromLoggedUrls() {
        assertEquals("https://example.test:8443",
                SafeLogFormatter.origin("https://user:example@example.test:8443/private?session=example#fragment"));
        assertEquals("unknown-origin", SafeLogFormatter.origin("not a URL"));
    }
}
