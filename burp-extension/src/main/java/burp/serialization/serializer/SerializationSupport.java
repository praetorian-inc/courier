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

package burp.serialization.serializer;

import burp.api.montoya.http.message.HttpHeader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SerializationSupport {
    private SerializationSupport() {
    }

    static List<Map<String, String>> headers(List<HttpHeader> headers) {
        List<Map<String, String>> values = new ArrayList<>();
        if (headers == null) {
            return values;
        }
        for (HttpHeader header : headers) {
            Map<String, String> value = new LinkedHashMap<>();
            value.put(header.name(), header.value());
            values.add(value);
        }
        return values;
    }

    static String value(String value) {
        return value == null ? "" : value;
    }
}
