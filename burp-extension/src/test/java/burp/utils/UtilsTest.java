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

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {
    @Test
    void detectsNormalizedBinaryContentTypesAndExtensions() {
        assertTrue(Utils.isBinaryContentType("Image/PNG; charset=binary"));
        assertTrue(Utils.isBinaryContentType("application/pdf"));
        assertFalse(Utils.isBinaryContentType("application/json"));
        assertTrue(Utils.isBinaryFileExtension(".PNG"));
        assertFalse(Utils.isBinaryFileExtension(null));
    }

    @Test
    void detectsBinaryCharactersWithoutScanningAnEntirePayload() {
        assertTrue(Utils.isBinaryData("text\0data"));
        assertFalse(Utils.isBinaryData("normal text\nwith a line"));
        assertFalse(Utils.isBinaryData(""));
    }
}
