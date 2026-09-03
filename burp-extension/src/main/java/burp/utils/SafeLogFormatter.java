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

import java.net.URI;

public final class SafeLogFormatter {
    private SafeLogFormatter() {
    }

    public static String origin(String url) {
        if (url == null || url.isBlank()) {
            return "unknown-origin";
        }
        try {
            URI uri = URI.create(url);
            if (uri.getHost() == null) {
                return "unknown-origin";
            }
            String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
            return uri.getScheme() + "://" + uri.getHost() + port;
        } catch (Exception exception) {
            return "unknown-origin";
        }
    }

    public static int length(String value) {
        return value == null ? 0 : value.length();
    }
}
