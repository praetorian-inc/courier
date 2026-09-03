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

import burp.model.Webflow.WebflowStep;
import burp.serialization.dto.WebflowStepDto;
import burp.serialization.dto.HttpRequestDto;
import burp.serialization.dto.HttpResponseDto;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Jackson serializer for WebflowStep objects
 */
public class WebflowStepSerializer extends JsonSerializer<WebflowStep> {

    @Override
    public void serialize(WebflowStep step, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        
        gen.writeObject(toDto(step));
    }
    
    /**
     * Helper method to convert WebflowStep to WebflowStepDto
     */
    public static WebflowStepDto toDto(WebflowStep step) {
        if (step == null) {
            return null;
        }
        
        try {
            // Extract fields safely
            String action = step.getAction() != null ? step.getAction() : "";
            String selector = step.getSelector() != null ? step.getSelector() : "";
            String value = step.getValue() != null ? step.getValue() : "";
            String description = step.getDescription() != null ? step.getDescription() : "";
            int order = step.getOrder();
            long timestamp = step.getTimestamp();
            String url = step.getUrl() != null ? step.getUrl() : "";
            String elementText = step.getElementText() != null ? step.getElementText() : "";
            String stepType = step.getStepType() != null ? step.getStepType().name() : "";
            String playwrightCode = "";
            
            // Generate playwright code safely
            try {
                playwrightCode = step.toPlaywrightCode();
            } catch (Exception e) {
                playwrightCode = "// Error generating playwright code: " + e.getMessage();
            }
            
            // Get correlated requests DTOs (already DTOs, no conversion needed)
            List<HttpRequestDto> requestDtos = new ArrayList<>();
            try {
                if (step.getCorrelatedRequests() != null) {
                    requestDtos.addAll(step.getCorrelatedRequests());
                }
            } catch (Exception e) {
                // Continue with empty requests list
            }
            
            // Get correlated responses DTOs (already DTOs, no conversion needed)
            List<HttpResponseDto> responseDtos = new ArrayList<>();
            try {
                if (step.getCorrelatedResponses() != null) {
                    responseDtos.addAll(step.getCorrelatedResponses());
                }
            } catch (Exception e) {
                // Continue with empty responses list
            }
            
            // Create DTO
            return new WebflowStepDto(
                action,
                selector,
                value,
                description,
                order,
                timestamp,
                url,
                elementText,
                stepType,
                playwrightCode,
                requestDtos,
                responseDtos
            );
            
        } catch (Exception e) {
            return null;
        }
    }
}
