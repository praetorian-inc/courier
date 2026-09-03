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

package burp.model;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

public final class HttpRequestResponsePair {
    private HttpRequest originalRequest;
    private HttpResponse originalResponse;
    private HttpRequest modifiedRequest;
    private HttpResponse modifiedResponse;
    private String originalRequestNotes = "";
    private String originalResponseNotes = "";
    private String modifiedRequestNotes = "";
    private String modifiedResponseNotes = "";
    private boolean originalRequestHighlighted = false;
    private boolean originalResponseHighlighted = false;
    private boolean modifiedRequestHighlighted = false;
    private boolean modifiedResponseHighlighted = false;
    public boolean wasRequestIntercepted = false;
    public boolean wasResponseIntercepted = false;
    public boolean wasRequestModified = false;
    public boolean wasResponseModified = false;
    public boolean wasRequestBodyBase64Encoded = false;
    public boolean wasResponseBodyBase64Encoded = false;
    public boolean wasModifiedRequestBodyBase64Encoded = false;
    public boolean wasModifiedResponseBodyBase64Encoded = false;
    public String toolSource = "";
    public long originalRequestTime = 0;
    public long originalResponseTime = 0;
    public long modifiedRequestTime = 0;
    public long modifiedResponseTime = 0;

    public HttpRequest getOriginalRequest() {
        return originalRequest;
    }

    public HttpResponse getOriginalResponse() {
        return originalResponse;
    }

    public String getOriginalRequestNotes() {
        return originalRequestNotes;
    }

    public String getOriginalResponseNotes() {
        return originalResponseNotes;
    }

    public String getModifiedRequestNotes() {
        return modifiedRequestNotes;
    }

    public String getModifiedResponseNotes() {
        return modifiedResponseNotes;
    }

    public boolean isOriginalRequestHighlighted() {
        return originalRequestHighlighted;
    }

    public boolean isOriginalResponseHighlighted() {
        return originalResponseHighlighted;
    }

    public boolean isModifiedRequestHighlighted() {
        return modifiedRequestHighlighted;
    }

    public boolean isModifiedResponseHighlighted() {
        return modifiedResponseHighlighted;
    }

    public HttpRequestResponsePair setOriginalRequestNotes(String originalRequestNotes) {
        this.originalRequestNotes = originalRequestNotes;
        return this;
    }

    public HttpRequestResponsePair setOriginalResponseNotes(String originalResponseNotes) {
        this.originalResponseNotes = originalResponseNotes;
        return this;
    }

    public HttpRequestResponsePair setOriginalRequestHighlighted(boolean originalRequestHighlighted) {
        this.originalRequestHighlighted = originalRequestHighlighted;
        return this;
    }

    public HttpRequestResponsePair setOriginalResponseHighlighted(boolean originalResponseHighlighted) {
        this.originalResponseHighlighted = originalResponseHighlighted;
        return this;
    }

    public HttpRequestResponsePair setModifiedRequestHighlighted(boolean modifiedRequestHighlighted) {
        this.modifiedRequestHighlighted = modifiedRequestHighlighted;
        return this;
    }

    public HttpRequestResponsePair setModifiedResponseHighlighted(boolean modifiedResponseHighlighted) {
        this.modifiedResponseHighlighted = modifiedResponseHighlighted;
        return this;
    }

    public HttpRequestResponsePair setModifiedRequestNotes(String modifiedRequestNotes) {
        this.modifiedRequestNotes = modifiedRequestNotes;
        return this;
    }

    public HttpRequestResponsePair setModifiedResponseNotes(String modifiedResponseNotes) {
        this.modifiedResponseNotes = modifiedResponseNotes;
        return this;
    }

    public HttpRequestResponsePair setOriginalRequest(HttpRequest request, long now) {
        this.originalRequestTime = now;
        this.originalRequest = request;
        return this;
    }

    public HttpRequestResponsePair setOriginalResponse(HttpResponse response, long now) {
        this.originalResponseTime = now;
        this.originalResponse = response;
        return this;
    }

    public HttpRequestResponsePair setModifiedRequest(HttpRequest modifiedRequest, long now) {
        this.modifiedRequestTime = now;
        this.modifiedRequest = modifiedRequest;
        return this;
    }

    public HttpRequestResponsePair setModifiedResponse(HttpResponse modifiedResponse, long now) {
        this.modifiedResponseTime = now;
        this.modifiedResponse = modifiedResponse;
        return this;
    }

    public HttpRequestResponsePair setWasRequestBodyBase64Encoded(boolean wasRequestBodyBase64Encoded) {
        this.wasRequestBodyBase64Encoded = wasRequestBodyBase64Encoded;
        return this;
    }

    public HttpRequestResponsePair setWasResponseBodyBase64Encoded(boolean wasResponseBodyBase64Encoded) {
        this.wasResponseBodyBase64Encoded = wasResponseBodyBase64Encoded;
        return this;
    }

    public boolean isWasRequestBodyBase64Encoded() {
        return wasRequestBodyBase64Encoded;
    }

    public boolean isWasResponseBodyBase64Encoded() {
        return wasResponseBodyBase64Encoded;
    }

    public HttpRequest getModifiedRequest() {
        return modifiedRequest;
    }

    public HttpResponse getModifiedResponse() {
        return modifiedResponse;
    }

    public boolean isWasModifiedRequestBodyBase64Encoded() {
        return wasModifiedRequestBodyBase64Encoded;
    }

    public boolean isWasModifiedResponseBodyBase64Encoded() {
        return wasModifiedResponseBodyBase64Encoded;
    }

    public String getToolSource() {
        return toolSource;
    }

    public HttpRequestResponsePair setToolSource(String toolSource) {
        this.toolSource = toolSource;
        return this;
    }

    public static HttpRequestResponsePair mergeFailedBatch(
            HttpRequestResponsePair failed, HttpRequestResponsePair current) {
        if (current == null) {
            return failed;
        }
        if (failed == null) {
            return current;
        }
        HttpRequestResponsePair merged = copyOf(current);
        if (merged.originalRequest == null && failed.originalRequest != null) {
            merged.setOriginalRequest(failed.originalRequest, failed.originalRequestTime)
                    .setOriginalRequestNotes(failed.originalRequestNotes)
                    .setOriginalRequestHighlighted(failed.originalRequestHighlighted);
            merged.wasRequestBodyBase64Encoded = failed.wasRequestBodyBase64Encoded;
        }
        if (merged.originalResponse == null && failed.originalResponse != null) {
            merged.setOriginalResponse(failed.originalResponse, failed.originalResponseTime)
                    .setOriginalResponseNotes(failed.originalResponseNotes)
                    .setOriginalResponseHighlighted(failed.originalResponseHighlighted);
            merged.wasResponseBodyBase64Encoded = failed.wasResponseBodyBase64Encoded;
        }
        if (merged.modifiedRequest == null && failed.modifiedRequest != null) {
            merged.setModifiedRequest(failed.modifiedRequest, failed.modifiedRequestTime)
                    .setModifiedRequestNotes(failed.modifiedRequestNotes)
                    .setModifiedRequestHighlighted(failed.modifiedRequestHighlighted);
            merged.wasModifiedRequestBodyBase64Encoded = failed.wasModifiedRequestBodyBase64Encoded;
        }
        if (merged.modifiedResponse == null && failed.modifiedResponse != null) {
            merged.setModifiedResponse(failed.modifiedResponse, failed.modifiedResponseTime)
                    .setModifiedResponseNotes(failed.modifiedResponseNotes)
                    .setModifiedResponseHighlighted(failed.modifiedResponseHighlighted);
            merged.wasModifiedResponseBodyBase64Encoded = failed.wasModifiedResponseBodyBase64Encoded;
        }
        merged.wasRequestIntercepted |= failed.wasRequestIntercepted;
        merged.wasResponseIntercepted |= failed.wasResponseIntercepted;
        merged.wasRequestModified |= failed.wasRequestModified;
        merged.wasResponseModified |= failed.wasResponseModified;
        if (merged.toolSource == null || merged.toolSource.isEmpty()) {
            merged.toolSource = failed.toolSource;
        }
        return merged;
    }

    private static HttpRequestResponsePair copyOf(HttpRequestResponsePair source) {
        HttpRequestResponsePair copy = new HttpRequestResponsePair();
        copy.originalRequest = source.originalRequest;
        copy.originalResponse = source.originalResponse;
        copy.modifiedRequest = source.modifiedRequest;
        copy.modifiedResponse = source.modifiedResponse;
        copy.originalRequestNotes = source.originalRequestNotes;
        copy.originalResponseNotes = source.originalResponseNotes;
        copy.modifiedRequestNotes = source.modifiedRequestNotes;
        copy.modifiedResponseNotes = source.modifiedResponseNotes;
        copy.originalRequestHighlighted = source.originalRequestHighlighted;
        copy.originalResponseHighlighted = source.originalResponseHighlighted;
        copy.modifiedRequestHighlighted = source.modifiedRequestHighlighted;
        copy.modifiedResponseHighlighted = source.modifiedResponseHighlighted;
        copy.wasRequestIntercepted = source.wasRequestIntercepted;
        copy.wasResponseIntercepted = source.wasResponseIntercepted;
        copy.wasRequestModified = source.wasRequestModified;
        copy.wasResponseModified = source.wasResponseModified;
        copy.wasRequestBodyBase64Encoded = source.wasRequestBodyBase64Encoded;
        copy.wasResponseBodyBase64Encoded = source.wasResponseBodyBase64Encoded;
        copy.wasModifiedRequestBodyBase64Encoded = source.wasModifiedRequestBodyBase64Encoded;
        copy.wasModifiedResponseBodyBase64Encoded = source.wasModifiedResponseBodyBase64Encoded;
        copy.toolSource = source.toolSource;
        copy.originalRequestTime = source.originalRequestTime;
        copy.originalResponseTime = source.originalResponseTime;
        copy.modifiedRequestTime = source.modifiedRequestTime;
        copy.modifiedResponseTime = source.modifiedResponseTime;
        return copy;
    }
}