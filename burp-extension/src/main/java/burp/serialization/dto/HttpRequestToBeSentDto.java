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
 * DTO class representing the JSON structure for HTTP requests to be sent
 * Extends HttpRequestDto with additional fields specific to HttpRequestToBeSent
 */
public class HttpRequestToBeSentDto extends HttpRequestDto {
    
    @JsonProperty("toolSource")
    private String toolSource;
    
    @JsonProperty("notes")
    private String notes;
    
    @JsonProperty("highlighted")
    private boolean highlighted;
    
    // Default constructor
    public HttpRequestToBeSentDto() {
        super();
    }
    
    // Constructor with all base fields plus additional fields
    public HttpRequestToBeSentDto(String body, int messageId, boolean inScope, String method, 
                                 String path, List<Map<String, String>> headers, String url,
                                 String toolSource, String notes, boolean highlighted) {
        super(body, messageId, inScope, method, path, headers, url);
        this.toolSource = toolSource;
        this.notes = notes;
        this.highlighted = highlighted;
    }
    
    // Getters and setters for additional fields
    public String getToolSource() {
        return toolSource;
    }
    
    public void setToolSource(String toolSource) {
        this.toolSource = toolSource;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public boolean isHighlighted() {
        return highlighted;
    }
    
    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }
}
