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
 * DTO class representing the JSON structure for Webflows
 */
public class WebflowDto {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("projectName")
    private String projectName;
    
    @JsonProperty("startUrl")
    private String startUrl;
    
    @JsonProperty("createdAt")
    private String createdAt;
    
    @JsonProperty("lastModified")
    private String lastModified;
    
    @JsonProperty("steps")
    private List<WebflowStepDto> steps;
    
    @JsonProperty("stepsCount")
    private int stepsCount;
    
    @JsonProperty("uncorrelatedRequests")
    private List<HttpRequestDto> uncorrelatedRequests;
    
    @JsonProperty("uncorrelatedResponses")
    private List<HttpResponseDto> uncorrelatedResponses;
    
    // Default constructor
    public WebflowDto() {}
    
    // Constructor with all fields
    public WebflowDto(String id, String name, String description, String projectName, 
                     String startUrl, String createdAt, String lastModified, 
                     List<WebflowStepDto> steps, int stepsCount, 
                     List<HttpRequestDto> uncorrelatedRequests, 
                     List<HttpResponseDto> uncorrelatedResponses) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.projectName = projectName;
        this.startUrl = startUrl;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
        this.steps = steps;
        this.stepsCount = stepsCount;
        this.uncorrelatedRequests = uncorrelatedRequests;
        this.uncorrelatedResponses = uncorrelatedResponses;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public String getStartUrl() {
        return startUrl;
    }
    
    public void setStartUrl(String startUrl) {
        this.startUrl = startUrl;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
    
    public List<WebflowStepDto> getSteps() {
        return steps;
    }
    
    public void setSteps(List<WebflowStepDto> steps) {
        this.steps = steps;
    }
    
    public int getStepsCount() {
        return stepsCount;
    }
    
    public void setStepsCount(int stepsCount) {
        this.stepsCount = stepsCount;
    }
    
    public List<HttpRequestDto> getUncorrelatedRequests() {
        return uncorrelatedRequests;
    }
    
    public void setUncorrelatedRequests(List<HttpRequestDto> uncorrelatedRequests) {
        this.uncorrelatedRequests = uncorrelatedRequests;
    }
    
    public List<HttpResponseDto> getUncorrelatedResponses() {
        return uncorrelatedResponses;
    }
    
    public void setUncorrelatedResponses(List<HttpResponseDto> uncorrelatedResponses) {
        this.uncorrelatedResponses = uncorrelatedResponses;
    }
}
