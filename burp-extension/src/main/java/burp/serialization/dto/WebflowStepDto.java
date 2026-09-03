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

/**
 * DTO class representing the JSON structure for Webflow Steps
 */
public class WebflowStepDto {
    
    @JsonProperty("action")
    private String action;
    
    @JsonProperty("selector")
    private String selector;
    
    @JsonProperty("value")
    private String value;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("order")
    private int order;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("elementText")
    private String elementText;
    
    @JsonProperty("stepType")
    private String stepType;
    
    @JsonProperty("playwrightCode")
    private String playwrightCode;
    
    @JsonProperty("correlatedRequests")
    private List<HttpRequestDto> correlatedRequests;
    
    @JsonProperty("correlatedResponses")
    private List<HttpResponseDto> correlatedResponses;
    
    // Default constructor
    public WebflowStepDto() {}
    
    // Constructor with all fields
    public WebflowStepDto(String action, String selector, String value, String description, 
                         int order, long timestamp, String url, String elementText, 
                         String stepType, String playwrightCode, 
                         List<HttpRequestDto> correlatedRequests, 
                         List<HttpResponseDto> correlatedResponses) {
        this.action = action;
        this.selector = selector;
        this.value = value;
        this.description = description;
        this.order = order;
        this.timestamp = timestamp;
        this.url = url;
        this.elementText = elementText;
        this.stepType = stepType;
        this.playwrightCode = playwrightCode;
        this.correlatedRequests = correlatedRequests;
        this.correlatedResponses = correlatedResponses;
    }
    
    // Getters and setters
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getSelector() {
        return selector;
    }
    
    public void setSelector(String selector) {
        this.selector = selector;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getElementText() {
        return elementText;
    }
    
    public void setElementText(String elementText) {
        this.elementText = elementText;
    }
    
    public String getStepType() {
        return stepType;
    }
    
    public void setStepType(String stepType) {
        this.stepType = stepType;
    }
    
    public String getPlaywrightCode() {
        return playwrightCode;
    }
    
    public void setPlaywrightCode(String playwrightCode) {
        this.playwrightCode = playwrightCode;
    }
    
    public List<HttpRequestDto> getCorrelatedRequests() {
        return correlatedRequests;
    }
    
    public void setCorrelatedRequests(List<HttpRequestDto> correlatedRequests) {
        this.correlatedRequests = correlatedRequests;
    }
    
    public List<HttpResponseDto> getCorrelatedResponses() {
        return correlatedResponses;
    }
    
    public void setCorrelatedResponses(List<HttpResponseDto> correlatedResponses) {
        this.correlatedResponses = correlatedResponses;
    }
}
