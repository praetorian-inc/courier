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

package burp.serialization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO class representing the JSON structure for HTTP request/response pairs
 */
public class HttpRequestResponsePairDto {
    
    @JsonProperty("originalRequest")
    private HttpRequestDto originalRequest;
    
    @JsonProperty("originalResponse") 
    private HttpResponseDto originalResponse;
    
    @JsonProperty("modifiedRequest")
    private HttpRequestDto modifiedRequest;
    
    @JsonProperty("modifiedResponse")
    private HttpResponseDto modifiedResponse;
    
    @JsonProperty("wasRequestIntercepted")
    private boolean wasRequestIntercepted;
    
    @JsonProperty("wasResponseIntercepted") 
    private boolean wasResponseIntercepted;
    
    @JsonProperty("wasRequestModified")
    private boolean wasRequestModified;
    
    @JsonProperty("wasResponseModified")
    private boolean wasResponseModified;
    
    @JsonProperty("wasModifiedRequestBodyBase64Encoded")
    private boolean wasModifiedRequestBodyBase64Encoded;
    
    @JsonProperty("wasModifiedResponseBodyBase64Encoded")
    private boolean wasModifiedResponseBodyBase64Encoded;
    
    @JsonProperty("wasRequestBodyBase64Encoded")
    private boolean wasRequestBodyBase64Encoded;
    
    @JsonProperty("wasResponseBodyBase64Encoded")
    private boolean wasResponseBodyBase64Encoded;
    
    @JsonProperty("toolSource")
    private String toolSource;
    
    @JsonProperty("originalRequestTime")
    private long originalRequestTime;
    
    @JsonProperty("originalResponseTime")
    private long originalResponseTime;
    
    @JsonProperty("modifiedRequestTime")
    private long modifiedRequestTime;
    
    @JsonProperty("modifiedResponseTime")
    private long modifiedResponseTime;
    
    @JsonProperty("originalRequestNotes")
    private String originalRequestNotes;
    
    @JsonProperty("originalResponseNotes")
    private String originalResponseNotes;
    
    @JsonProperty("modifiedRequestNotes")
    private String modifiedRequestNotes;
    
    @JsonProperty("modifiedResponseNotes")
    private String modifiedResponseNotes;
    
    @JsonProperty("originalRequestHighlighted")
    private boolean originalRequestHighlighted;
    
    @JsonProperty("originalResponseHighlighted")
    private boolean originalResponseHighlighted;
    
    @JsonProperty("modifiedRequestHighlighted")
    private boolean modifiedRequestHighlighted;
    
    @JsonProperty("modifiedResponseHighlighted")
    private boolean modifiedResponseHighlighted;
    
    // Default constructor
    public HttpRequestResponsePairDto() {}
    
    // Constructor with all fields
    public HttpRequestResponsePairDto(HttpRequestDto originalRequest, 
                                     HttpResponseDto originalResponse,
                                     HttpRequestDto modifiedRequest,
                                     HttpResponseDto modifiedResponse,
                                     boolean wasRequestIntercepted,
                                     boolean wasResponseIntercepted,
                                     boolean wasRequestModified,
                                     boolean wasResponseModified,
                                     boolean wasModifiedRequestBodyBase64Encoded,
                                     boolean wasModifiedResponseBodyBase64Encoded,
                                     boolean wasRequestBodyBase64Encoded,
                                     boolean wasResponseBodyBase64Encoded,
                                     String toolSource,
                                     long originalRequestTime,
                                     long originalResponseTime,
                                     long modifiedRequestTime,
                                     long modifiedResponseTime,
                                     String originalRequestNotes,
                                     String originalResponseNotes,
                                     String modifiedRequestNotes,
                                     String modifiedResponseNotes,
                                     boolean originalRequestHighlighted,
                                     boolean originalResponseHighlighted,
                                     boolean modifiedRequestHighlighted,
                                     boolean modifiedResponseHighlighted) {
        this.originalRequest = originalRequest;
        this.originalResponse = originalResponse;
        this.modifiedRequest = modifiedRequest;
        this.modifiedResponse = modifiedResponse;
        this.wasRequestIntercepted = wasRequestIntercepted;
        this.wasResponseIntercepted = wasResponseIntercepted;
        this.wasRequestModified = wasRequestModified;
        this.wasResponseModified = wasResponseModified;
        this.wasModifiedRequestBodyBase64Encoded = wasModifiedRequestBodyBase64Encoded;
        this.wasModifiedResponseBodyBase64Encoded = wasModifiedResponseBodyBase64Encoded;
        this.wasRequestBodyBase64Encoded = wasRequestBodyBase64Encoded;
        this.wasResponseBodyBase64Encoded = wasResponseBodyBase64Encoded;
        this.toolSource = toolSource;
        this.originalRequestTime = originalRequestTime;
        this.originalResponseTime = originalResponseTime;
        this.modifiedRequestTime = modifiedRequestTime;
        this.modifiedResponseTime = modifiedResponseTime;
        this.originalRequestNotes = originalRequestNotes;
        this.originalResponseNotes = originalResponseNotes;
        this.modifiedRequestNotes = modifiedRequestNotes;
        this.modifiedResponseNotes = modifiedResponseNotes;
        this.originalRequestHighlighted = originalRequestHighlighted;
        this.originalResponseHighlighted = originalResponseHighlighted;
        this.modifiedRequestHighlighted = modifiedRequestHighlighted;
        this.modifiedResponseHighlighted = modifiedResponseHighlighted;
    }
    
    // Getters and setters
    public HttpRequestDto getOriginalRequest() {
        return originalRequest;
    }
    
    public void setOriginalRequest(HttpRequestDto originalRequest) {
        this.originalRequest = originalRequest;
    }
    
    public HttpResponseDto getOriginalResponse() {
        return originalResponse;
    }
    
    public void setOriginalResponse(HttpResponseDto originalResponse) {
        this.originalResponse = originalResponse;
    }
    
    public HttpRequestDto getModifiedRequest() {
        return modifiedRequest;
    }
    
    public void setModifiedRequest(HttpRequestDto modifiedRequest) {
        this.modifiedRequest = modifiedRequest;
    }
    
    public HttpResponseDto getModifiedResponse() {
        return modifiedResponse;
    }
    
    public void setModifiedResponse(HttpResponseDto modifiedResponse) {
        this.modifiedResponse = modifiedResponse;
    }
    
    public boolean isWasRequestIntercepted() {
        return wasRequestIntercepted;
    }
    
    public void setWasRequestIntercepted(boolean wasRequestIntercepted) {
        this.wasRequestIntercepted = wasRequestIntercepted;
    }
    
    public boolean isWasResponseIntercepted() {
        return wasResponseIntercepted;
    }
    
    public void setWasResponseIntercepted(boolean wasResponseIntercepted) {
        this.wasResponseIntercepted = wasResponseIntercepted;
    }
    
    public boolean isWasRequestModified() {
        return wasRequestModified;
    }
    
    public void setWasRequestModified(boolean wasRequestModified) {
        this.wasRequestModified = wasRequestModified;
    }
    
    public boolean isWasResponseModified() {
        return wasResponseModified;
    }
    
    public void setWasResponseModified(boolean wasResponseModified) {
        this.wasResponseModified = wasResponseModified;
    }

    public boolean isWasModifiedRequestBodyBase64Encoded() {
        return wasModifiedRequestBodyBase64Encoded;
    }
    
    public void setWasModifiedRequestBodyBase64Encoded(boolean wasModifiedRequestBodyBase64Encoded) {
        this.wasModifiedRequestBodyBase64Encoded = wasModifiedRequestBodyBase64Encoded;
    }

    public boolean isWasModifiedResponseBodyBase64Encoded() {
        return wasModifiedResponseBodyBase64Encoded;
    }
    
    public void setWasModifiedResponseBodyBase64Encoded(boolean wasModifiedResponseBodyBase64Encoded) {
        this.wasModifiedResponseBodyBase64Encoded = wasModifiedResponseBodyBase64Encoded;
    }

    public boolean isWasRequestBodyBase64Encoded() {
        return wasRequestBodyBase64Encoded;
    }
    
    public void setWasRequestBodyBase64Encoded(boolean wasRequestBodyBase64Encoded) {
        this.wasRequestBodyBase64Encoded = wasRequestBodyBase64Encoded;
    }

    public boolean isWasResponseBodyBase64Encoded() {
        return wasResponseBodyBase64Encoded;
    }
    
    public void setWasResponseBodyBase64Encoded(boolean wasResponseBodyBase64Encoded) {
        this.wasResponseBodyBase64Encoded = wasResponseBodyBase64Encoded;
    }

    public long getOriginalRequestTime() {
        return originalRequestTime;
    }
    
    public void setOriginalRequestTime(long originalRequestTime) {
        this.originalRequestTime = originalRequestTime;
    }

    public long getOriginalResponseTime() {
        return originalResponseTime;
    }
    
    public void setOriginalResponseTime(long originalResponseTime) {
        this.originalResponseTime = originalResponseTime;
    }

    public long getModifiedRequestTime() {
        return modifiedRequestTime;
    }
    
    public void setModifiedRequestTime(long modifiedRequestTime) {
        this.modifiedRequestTime = modifiedRequestTime;
    }

    public long getModifiedResponseTime() {
        return modifiedResponseTime;
    }
    
    public void setModifiedResponseTime(long modifiedResponseTime) {
        this.modifiedResponseTime = modifiedResponseTime;
    }

    public String getToolSource() {
        return toolSource;
    }
    
    public void setToolSource(String toolSource) {
        this.toolSource = toolSource;
    }

    public String getOriginalRequestNotes() {
        return originalRequestNotes;
    }
    
    public void setOriginalRequestNotes(String originalRequestNotes) {
        this.originalRequestNotes = originalRequestNotes;
    }

    public String getOriginalResponseNotes() {
        return originalResponseNotes;
    }
    
    public void setOriginalResponseNotes(String originalResponseNotes) {
        this.originalResponseNotes = originalResponseNotes;
    }

    public String getModifiedRequestNotes() {
        return modifiedRequestNotes;
    }
    
    public void setModifiedRequestNotes(String modifiedRequestNotes) {
        this.modifiedRequestNotes = modifiedRequestNotes;
    }

    public String getModifiedResponseNotes() {
        return modifiedResponseNotes;
    }
    
    public void setModifiedResponseNotes(String modifiedResponseNotes) {
        this.modifiedResponseNotes = modifiedResponseNotes;
    }

    public boolean isOriginalRequestHighlighted() {
        return originalRequestHighlighted;
    }
    
    public void setOriginalRequestHighlighted(boolean originalRequestHighlighted) {
        this.originalRequestHighlighted = originalRequestHighlighted;
    }

    public boolean isOriginalResponseHighlighted() {
        return originalResponseHighlighted;
    }
    
    public void setOriginalResponseHighlighted(boolean originalResponseHighlighted) {
        this.originalResponseHighlighted = originalResponseHighlighted;
    }

    public boolean isModifiedRequestHighlighted() {
        return modifiedRequestHighlighted;
    }
    
    public void setModifiedRequestHighlighted(boolean modifiedRequestHighlighted) {
        this.modifiedRequestHighlighted = modifiedRequestHighlighted;
    }

    public boolean isModifiedResponseHighlighted() {
        return modifiedResponseHighlighted;
    }
    
    public void setModifiedResponseHighlighted(boolean modifiedResponseHighlighted) {
        this.modifiedResponseHighlighted = modifiedResponseHighlighted;
    }
}
