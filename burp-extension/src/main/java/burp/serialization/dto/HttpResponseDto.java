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
import java.util.List;
import java.util.Map;

/**
 * DTO class representing the JSON structure for HTTP responses
 */
public class HttpResponseDto {
    
    @JsonProperty("body")
    private String body;
    
    @JsonProperty("messageId")
    private int messageId;
    
    @JsonProperty("inScope")
    private boolean inScope;
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("headers")
    private List<Map<String, String>> headers;
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("statusCode")
    private int statusCode;
    
    
    // Default constructor
    public HttpResponseDto() {}
    
    // Constructor with all fields
    public HttpResponseDto(String body, int messageId, boolean inScope, String method, 
                          String path, List<Map<String, String>> headers, String url, int statusCode) {
        this.body = body;
        this.messageId = messageId;
        this.inScope = inScope;
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.url = url;
        this.statusCode = statusCode;
    }
    
    // Getters and setters
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public int getMessageId() {
        return messageId;
    }
    
    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }
    
    public boolean isInScope() {
        return inScope;
    }
    
    public void setInScope(boolean inScope) {
        this.inScope = inScope;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public List<Map<String, String>> getHeaders() {
        return headers;
    }
    
    public void setHeaders(List<Map<String, String>> headers) {
        this.headers = headers;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    
}
