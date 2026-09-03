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

package burp.controller.planner;

import burp.api.montoya.http.message.HttpMessage;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

public final class HttpPreviewFormatter {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private HttpPreviewFormatter() {
    }

    public static String raw(HttpMessage message) {
        if (message == null) {
            return "";
        }
        StringBuilder content = new StringBuilder();
        if (message instanceof HttpRequest request) {
            content.append(request.method()).append(' ')
                    .append(request.path()).append(' ')
                    .append(request.httpVersion()).append('\n');
        } else if (message instanceof HttpResponse response) {
            content.append(response.httpVersion()).append(' ')
                    .append(response.statusCode()).append(' ')
                    .append(response.reasonPhrase()).append('\n');
        }
        message.headers().forEach(header -> content
                .append(header.name()).append(": ").append(header.value()).append('\n'));
        content.append('\n').append(message.bodyToString());
        return content.toString();
    }

    public static String hex(byte[] bytes) {
        StringBuilder output = new StringBuilder(bytes.length * 4);
        for (int offset = 0; offset < bytes.length; offset += 16) {
            appendOffset(output, offset);
            int lineEnd = Math.min(offset + 16, bytes.length);
            for (int index = offset; index < offset + 16; index++) {
                if (index < lineEnd) {
                    int value = bytes[index] & 0xff;
                    output.append(HEX[value >>> 4]).append(HEX[value & 0x0f]).append(' ');
                } else {
                    output.append("   ");
                }
            }
            output.append(" |");
            for (int index = offset; index < lineEnd; index++) {
                int value = bytes[index] & 0xff;
                output.append(value >= 32 && value < 127 ? (char) value : '.');
            }
            output.append("|\n");
        }
        return output.toString();
    }

    private static void appendOffset(StringBuilder output, int offset) {
        for (int shift = 28; shift >= 0; shift -= 4) {
            output.append(HEX[(offset >>> shift) & 0x0f]);
        }
        output.append("  ");
    }
}
