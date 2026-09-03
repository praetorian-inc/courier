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
 * DTO class representing the JSON structure for audit issues
 */
public class AuditIssueDto {
    
    @JsonProperty("baseUrl")
    private String baseUrl;
    
    @JsonProperty("collaboratorInteractions")
    private List<InteractionDto> collaboratorInteractions;
    
    @JsonProperty("confidence")
    private String confidence;
    
    @JsonProperty("requests")
    private List<HttpRequestDto> requests;
    
    @JsonProperty("responses")
    private List<HttpResponseDto> responses;
    
    @JsonProperty("name")
    private String name;

    @JsonProperty("detail")
    private String detail;
    
    @JsonProperty("remediation")
    private String remediation;
    
    @JsonProperty("severity")
    private String severity;
    
    // Default constructor
    public AuditIssueDto() {}
    
    // Constructor with all fields
    public AuditIssueDto(String baseUrl, List<InteractionDto> collaboratorInteractions, 
                        String confidence, List<HttpRequestDto> requests, 
                        List<HttpResponseDto> responses, String name, String remediation, String severity) {
        this.baseUrl = baseUrl;
        this.collaboratorInteractions = collaboratorInteractions;
        this.confidence = confidence;
        this.requests = requests;
        this.responses = responses;
        this.name = name;
        this.remediation = remediation;
        this.severity = severity;
    }
    
    // Getters and setters
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public List<InteractionDto> getCollaboratorInteractions() {
        return collaboratorInteractions;
    }
    
    public void setCollaboratorInteractions(List<InteractionDto> collaboratorInteractions) {
        this.collaboratorInteractions = collaboratorInteractions;
    }
    
    public String getConfidence() {
        return confidence;
    }
    
    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }
    
    public List<HttpRequestDto> getRequests() {
        return requests;
    }
    
    public void setRequests(List<HttpRequestDto> requests) {
        this.requests = requests;
    }
    
    public List<HttpResponseDto> getResponses() {
        return responses;
    }
    
    public void setResponses(List<HttpResponseDto> responses) {
        this.responses = responses;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getRemediation() {
        return remediation;
    }
    
    public void setRemediation(String remediation) {
        this.remediation = remediation;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
