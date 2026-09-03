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

import burp.api.montoya.organizer.OrganizerItem;
import burp.serialization.dto.OrganizerItemDto;
import burp.serialization.dto.HttpRequestDto;
import burp.serialization.dto.HttpResponseDto;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Custom Jackson serializer for OrganizerItem objects
 */
public class OrganizerItemSerializer extends JsonSerializer<OrganizerItem> {

    @Override
    public void serialize(OrganizerItem item, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        
        gen.writeObject(toDto(item));
    }
    
    /**
     * Helper method to convert OrganizerItem to OrganizerItemDto
     */
    public static OrganizerItemDto toDto(OrganizerItem item) {
        if (item == null) {
            return null;
        }
        
        try {
            // Extract basic fields safely
            String id = "";
            String name = "";
            String host = "";
            String path = "";
            String url = "";
            String method = "";
            short statusCode = 0;
            String mimeType = "";
            int contentLength = 0;
            String notes = "";
            String status = "";
            String highlightColor = "";
            
            try {
                // Use the id() method from OrganizerItem interface
                id = String.valueOf(item.id());
            } catch (Exception e) {
                id = "unknown";
            }
            
            try {
                status = String.valueOf(item.status());
                name = "Organizer Item " + id + " (" + status + ")";
            } catch (Exception e) {
                name = "Organizer Item " + id;
            }
            
            try {
                // Extract URL components from the request instead of using deprecated url() method
                if (item.request() != null && item.request().url() != null) {
                    url = item.request().url();
                    java.net.URI uri = java.net.URI.create(url);
                    host = uri.getHost() == null ? "" : uri.getHost();
                    path = uri.getPath() == null ? "" : uri.getPath();
                }
            } catch (Exception e) {
                // Fallback values
                host = "";
                path = "";
                url = "";
            }
            
            try {
                // Get method from the request - OrganizerItem extends HttpRequestResponse
                if (item.request() != null) {
                    method = item.request().method();
                }
            } catch (Exception e) {
                method = "";
            }
            
            try {
                // Get status code from the response
                if (item.hasResponse() && item.response() != null) {
                    statusCode = item.response().statusCode();
                }
            } catch (Exception e) {
                statusCode = 0;
            }
            
            try {
                // Get MIME type from the response
                if (item.hasResponse() && item.response() != null) {
                    mimeType = item.response().mimeType().toString();
                }
            } catch (Exception e) {
                mimeType = "";
            }
            
            try {
                // Get content length from the response
                if (item.hasResponse() && item.response() != null) {
                    contentLength = item.response().body().length();
                }
            } catch (Exception e) {
                contentLength = 0;
            }
            
            try {
                // Extract notes from annotations
                if (item.annotations() != null && item.annotations().notes() != null) {
                    notes = item.annotations().notes();
                }
            } catch (Exception e) {
                notes = "";
            }
            
            try {
                if (item.annotations() != null && item.annotations().hasHighlightColor()) {
                    highlightColor = String.valueOf(item.annotations().highlightColor());
                }
            } catch (Exception e) {
                highlightColor = "";
            }

            // Convert request and response using existing serializers
            HttpRequestDto requestDto = null;
            HttpResponseDto responseDto = null;
            
            try {
                // OrganizerItem extends HttpRequestResponse, so we can access request() directly
                if (item.request() != null) {
                    requestDto = HttpRequestSerializer.toDto(item.request());
                }
            } catch (Exception e) {
                // Request conversion failed, leave as null
            }
            
            try {
                // Access response directly from OrganizerItem
                if (item.hasResponse() && item.response() != null) {
                    responseDto = HttpResponseSerializer.toDto(item.response());
                }
            } catch (Exception e) {
                // Response conversion failed, leave as null
            }
            
            // Create DTO
            return new OrganizerItemDto(
                id,
                name,
                requestDto,
                responseDto,
                host,
                path,
                url,
                method,
                statusCode,
                mimeType,
                contentLength,
                notes,
                status,
                highlightColor
            );
            
        } catch (Exception e) {
            return null;
        }
    }
}
