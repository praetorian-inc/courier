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

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.model.HttpRequestResponsePair;
import burp.utils.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PlannerRequestPayloadFactory {
    static final int MAX_BODY_BYTES_PER_MESSAGE = 16 * 1024;
    static final int MAX_TOTAL_BODY_BYTES = 64 * 1024;
    private static final int MAX_HEADERS = 100;
    private static final int MAX_HEADER_NAME_CHARACTERS = 256;
    private static final int MAX_HEADER_VALUE_CHARACTERS = 2048;
    private static final int MAX_URL_CHARACTERS = 8192;
    private static final int MAX_NOTES_CHARACTERS = 4096;

    Object create(List<HttpRequestResponsePair> selectedRequests) {
        BodyBudget bodyBudget = new BodyBudget();
        List<Map<String, Object>> payloads = selectedRequests.stream()
                .map(pair -> pairPayload(pair, bodyBudget))
                .toList();
        return payloads.size() == 1 ? payloads.get(0) : payloads;
    }

    private Map<String, Object> pairPayload(HttpRequestResponsePair pair, BodyBudget bodyBudget) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("originalRequest", requestPayload(
                pair.getOriginalRequest(), pair.wasRequestBodyBase64Encoded, bodyBudget));
        payload.put("originalResponse", responsePayload(
                pair.getOriginalResponse(), pair.wasResponseBodyBase64Encoded, bodyBudget));
        payload.put("modifiedRequest", requestPayload(
                pair.getModifiedRequest(), pair.wasModifiedRequestBodyBase64Encoded, bodyBudget));
        payload.put("modifiedResponse", responsePayload(
                pair.getModifiedResponse(), pair.wasModifiedResponseBodyBase64Encoded, bodyBudget));
        payload.put("wasRequestIntercepted", pair.wasRequestIntercepted);
        payload.put("wasResponseIntercepted", pair.wasResponseIntercepted);
        payload.put("wasRequestModified", pair.wasRequestModified);
        payload.put("wasResponseModified", pair.wasResponseModified);
        payload.put("toolSource", pair.toolSource);
        payload.put("originalRequestTime", pair.originalRequestTime);
        payload.put("originalResponseTime", pair.originalResponseTime);
        payload.put("modifiedRequestTime", pair.modifiedRequestTime);
        payload.put("modifiedResponseTime", pair.modifiedResponseTime);
        payload.put("originalRequestNotes", truncate(pair.getOriginalRequestNotes(), MAX_NOTES_CHARACTERS));
        payload.put("originalResponseNotes", truncate(pair.getOriginalResponseNotes(), MAX_NOTES_CHARACTERS));
        payload.put("modifiedRequestNotes", truncate(pair.getModifiedRequestNotes(), MAX_NOTES_CHARACTERS));
        payload.put("modifiedResponseNotes", truncate(pair.getModifiedResponseNotes(), MAX_NOTES_CHARACTERS));
        return payload;
    }

    private Map<String, Object> requestPayload(HttpRequest request, boolean alreadyBase64Encoded,
            BodyBudget bodyBudget) {
        if (request == null) {
            return null;
        }
        BodySample body = bodySample(request.body(), alreadyBase64Encoded,
                Utils.isBinaryContentType(request.headerValue("Content-Type")), bodyBudget);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("body", body.content());
        payload.put("bodyLength", body.originalLength());
        payload.put("bodyTruncated", body.truncated());
        payload.put("bodyEncoding", body.encoding());
        payload.put("inScope", request.isInScope());
        payload.put("method", request.method());
        payload.put("path", truncate(request.path(), MAX_URL_CHARACTERS));
        payload.put("headers", headers(request.headers()));
        payload.put("url", truncate(request.url(), MAX_URL_CHARACTERS));
        return payload;
    }

    private Map<String, Object> responsePayload(HttpResponse response, boolean alreadyBase64Encoded,
            BodyBudget bodyBudget) {
        if (response == null) {
            return null;
        }
        BodySample body = bodySample(response.body(), alreadyBase64Encoded,
                Utils.isBinaryContentType(response.headerValue("Content-Type")), bodyBudget);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("body", body.content());
        payload.put("bodyLength", body.originalLength());
        payload.put("bodyTruncated", body.truncated());
        payload.put("bodyEncoding", body.encoding());
        payload.put("headers", headers(response.headers()));
        payload.put("statusCode", response.statusCode());
        return payload;
    }

    private BodySample bodySample(ByteArray body, boolean alreadyBase64Encoded,
            boolean binary, BodyBudget bodyBudget) {
        boolean base64 = alreadyBase64Encoded || binary;
        if (body == null || body.length() == 0) {
            return new BodySample("", 0, false, base64 ? "base64" : "utf-8");
        }
        int sampleLength = bodyBudget.claim(body.length());
        if (alreadyBase64Encoded && sampleLength < body.length()) {
            sampleLength -= sampleLength % 4;
        }
        byte[] bytes = sampleLength == body.length()
                ? body.getBytes()
                : body.subArray(0, sampleLength).getBytes();
        String content = binary && !alreadyBase64Encoded
                ? Base64.getEncoder().encodeToString(bytes)
                : new String(bytes, StandardCharsets.UTF_8);
        return new BodySample(content, body.length(), sampleLength < body.length(),
                base64 ? "base64" : "utf-8");
    }

    private List<Map<String, String>> headers(List<HttpHeader> headers) {
        List<Map<String, String>> values = new ArrayList<>();
        if (headers == null) {
            return values;
        }
        for (int index = 0; index < Math.min(headers.size(), MAX_HEADERS); index++) {
            HttpHeader header = headers.get(index);
            Map<String, String> value = new LinkedHashMap<>();
            value.put(truncate(header.name(), MAX_HEADER_NAME_CHARACTERS),
                    truncate(header.value(), MAX_HEADER_VALUE_CHARACTERS));
            values.add(value);
        }
        return values;
    }

    private static String truncate(String value, int maximumCharacters) {
        if (value == null || value.length() <= maximumCharacters) {
            return value == null ? "" : value;
        }
        return value.substring(0, maximumCharacters) + "…";
    }

    private static final class BodyBudget {
        private int remaining = MAX_TOTAL_BODY_BYTES;

        int claim(int bodyLength) {
            int claimed = Math.min(Math.min(bodyLength, MAX_BODY_BYTES_PER_MESSAGE), remaining);
            remaining -= claimed;
            return claimed;
        }
    }

    private record BodySample(String content, int originalLength, boolean truncated, String encoding) {
    }
}
