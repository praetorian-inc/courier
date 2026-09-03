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

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.BiFunction;

final class BinaryMessageSupport {
    private BinaryMessageSupport() {
    }

    static boolean isBinary(HttpRequest request) {
        return request != null && (Utils.isBinaryContentType(request.headerValue("Content-Type"))
                || Utils.isBinaryFileExtension(request.fileExtension()));
    }

    static boolean isBinary(HttpResponse response, String initiatingRequestExtension) {
        return response != null && (Utils.isBinaryContentType(response.headerValue("Content-Type"))
                || Utils.isBinaryFileExtension(initiatingRequestExtension));
    }

    static HttpRequest encodeBody(HttpRequest request) {
        return request.withBody(Base64.getEncoder().encodeToString(request.body().getBytes()));
    }

    static HttpResponse encodeBody(HttpResponse response) {
        return response.withBody(Base64.getEncoder().encodeToString(response.body().getBytes()));
    }

    static boolean requestsEqual(HttpRequest first, HttpRequest second) {
        return first != null && second != null
                && Arrays.equals(first.toByteArray().getBytes(), second.toByteArray().getBytes());
    }

    static boolean responsesEqual(HttpResponse first, HttpResponse second) {
        return first != null && second != null
                && Arrays.equals(first.toByteArray().getBytes(), second.toByteArray().getBytes());
    }

    static EncodedPairs encodePairs(List<HttpRequestResponse> pairs) {
        return encodePairs(pairs, HttpRequestResponse::httpRequestResponse);
    }

    static EncodedPairs encodePairs(List<HttpRequestResponse> pairs,
            BiFunction<HttpRequest, HttpResponse, HttpRequestResponse> pairFactory) {
        List<HttpRequestResponse> encodedPairs = new ArrayList<>(pairs.size());
        boolean containsEncodedData = false;
        for (HttpRequestResponse pair : pairs) {
            EncodedPair encodedPair = encodePair(pair, pairFactory);
            encodedPairs.add(encodedPair.pair());
            containsEncodedData |= encodedPair.encoded();
        }
        return new EncodedPairs(List.copyOf(encodedPairs), containsEncodedData);
    }

    private static EncodedPair encodePair(HttpRequestResponse pair,
            BiFunction<HttpRequest, HttpResponse, HttpRequestResponse> pairFactory) {
        HttpRequest request = pair.request();
        HttpResponse response = pair.response();
        boolean requestEncoded = isBinary(request);
        boolean responseEncoded = isBinary(response, request == null ? "" : request.fileExtension());
        return new EncodedPair(
                pairFactory.apply(
                        requestEncoded ? encodeBody(request) : request,
                        responseEncoded ? encodeBody(response) : response),
                requestEncoded || responseEncoded);
    }

    private record EncodedPair(HttpRequestResponse pair, boolean encoded) {
    }

    record EncodedPairs(List<HttpRequestResponse> pairs, boolean encoded) {
    }
}
