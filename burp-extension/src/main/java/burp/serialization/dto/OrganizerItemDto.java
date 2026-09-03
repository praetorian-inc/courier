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
 * DTO class representing the JSON structure for Organizer items
 * Based on the Burp Suite Montoya API OrganizerItem interface
 */
public class OrganizerItemDto {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("request")
    private HttpRequestDto request;
    
    @JsonProperty("response")
    private HttpResponseDto response;
    
    @JsonProperty("host")
    private String host;
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("statusCode")
    private short statusCode;
    
    @JsonProperty("mimeType")
    private String mimeType;
    
    @JsonProperty("contentLength")
    private int contentLength;
    
    @JsonProperty("notes")
    private String notes;

    @JsonProperty("status")
    private String status;

    @JsonProperty("highlightColor")
    private String highlightColor;
    
    // Default constructor
    public OrganizerItemDto() {}
    
    // Constructor with all fields
    public OrganizerItemDto(String id, String name, HttpRequestDto request, HttpResponseDto response,
                           String host, String path, String url, String method, short statusCode,
                           String mimeType, int contentLength, String notes,
                           String status, String highlightColor) {
        this.id = id;
        this.name = name;
        this.request = request;
        this.response = response;
        this.host = host;
        this.path = path;
        this.url = url;
        this.method = method;
        this.statusCode = statusCode;
        this.mimeType = mimeType;
        this.contentLength = contentLength;
        this.notes = notes;
        this.status = status;
        this.highlightColor = highlightColor;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public HttpRequestDto getRequest() {
        return request;
    }
    
    public void setRequest(HttpRequestDto request) {
        this.request = request;
    }
    
    public HttpResponseDto getResponse() {
        return response;
    }
    
    public void setResponse(HttpResponseDto response) {
        this.response = response;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public short getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(short statusCode) {
        this.statusCode = statusCode;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public int getContentLength() {
        return contentLength;
    }
    
    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHighlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(String highlightColor) {
        this.highlightColor = highlightColor;
    }
}
