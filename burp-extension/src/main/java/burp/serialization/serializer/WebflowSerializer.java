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

package burp.serialization.serializer;

import burp.model.Webflow;
import burp.serialization.dto.WebflowDto;
import burp.serialization.dto.WebflowStepDto;
import burp.serialization.dto.HttpRequestDto;
import burp.serialization.dto.HttpResponseDto;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Jackson serializer for Webflow objects
 */
public class WebflowSerializer extends JsonSerializer<Webflow> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public void serialize(Webflow webflow, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        
        gen.writeObject(toDto(webflow));
    }
    
    /**
     * Helper method to convert Webflow to WebflowDto
     */
    public static WebflowDto toDto(Webflow webflow) {
        if (webflow == null) {
            return null;
        }
        
        try {
            // Extract fields safely
            String id = webflow.getId() != null ? webflow.getId() : "";
            String name = webflow.getName() != null ? webflow.getName() : "";
            String description = webflow.getDescription() != null ? webflow.getDescription() : "";
            String projectName = webflow.getProjectName() != null ? webflow.getProjectName() : "";
            String startUrl = webflow.getStartUrl() != null ? webflow.getStartUrl() : "";
            
            // Format dates safely
            String createdAt = "";
            String lastModified = "";
            try {
                if (webflow.getCreatedAt() != null) {
                    createdAt = webflow.getCreatedAt().format(FORMATTER);
                }
                if (webflow.getLastModified() != null) {
                    lastModified = webflow.getLastModified().format(FORMATTER);
                }
            } catch (Exception e) {
                // Keep empty strings if date formatting fails
            }
            
            // Convert steps to DTOs
            List<WebflowStepDto> stepDtos = new ArrayList<>();
            int stepsCount = 0;
            try {
                if (webflow.getSteps() != null) {
                    stepsCount = webflow.getSteps().size();
                    for (Webflow.WebflowStep step : webflow.getSteps()) {
                        WebflowStepDto stepDto = WebflowStepSerializer.toDto(step);
                        if (stepDto != null) {
                            stepDtos.add(stepDto);
                        }
                    }
                }
            } catch (Exception e) {
                // Continue with empty steps list
            }
            
            // Get uncorrelated requests DTOs (already DTOs, no conversion needed)
            List<HttpRequestDto> requestDtos = new ArrayList<>();
            try {
                if (webflow.getUncorrelatedRequests() != null) {
                    requestDtos.addAll(webflow.getUncorrelatedRequests());
                }
            } catch (Exception e) {
                // Continue with empty requests list
            }
            
            // Get uncorrelated responses DTOs (already DTOs, no conversion needed)
            List<HttpResponseDto> responseDtos = new ArrayList<>();
            try {
                if (webflow.getUncorrelatedResponses() != null) {
                    responseDtos.addAll(webflow.getUncorrelatedResponses());
                }
            } catch (Exception e) {
                // Continue with empty responses list
            }
            
            // Create DTO
            return new WebflowDto(
                id,
                name,
                description,
                projectName,
                startUrl,
                createdAt,
                lastModified,
                stepDtos,
                stepsCount,
                requestDtos,
                responseDtos
            );
            
        } catch (Exception e) {
            return null;
        }
    }
}
