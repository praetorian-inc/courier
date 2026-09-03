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

import burp.model.HttpRequestResponsePair;
import burp.serialization.dto.HttpRequestResponsePairDto;
import burp.serialization.dto.HttpRequestDto;
import burp.serialization.dto.HttpResponseDto;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Custom Jackson serializer for HttpRequestResponsePair objects
 */
public class HttpRequestResponsePairSerializer extends JsonSerializer<HttpRequestResponsePair> {

    @Override
    public void serialize(HttpRequestResponsePair pair, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        
        gen.writeObject(toDto(pair));
    }
    
    /**
     * Helper method to convert HttpRequestResponsePair to HttpRequestResponsePairDto
     */
    public static HttpRequestResponsePairDto toDto(HttpRequestResponsePair pair) {
        if (pair == null) {
            return null;
        }
        
        // Convert HttpRequest and HttpResponse objects to their respective DTOs
            HttpRequestDto originalRequestDto = null;
            if (pair.getOriginalRequest() != null) {
                originalRequestDto = HttpRequestSerializer.toDto(pair.getOriginalRequest());
            }
            
            HttpResponseDto originalResponseDto = null;
            if (pair.getOriginalResponse() != null) {
                originalResponseDto = HttpResponseSerializer.toDto(pair.getOriginalResponse());
            }
            
            HttpRequestDto modifiedRequestDto = null;
            if (pair.getModifiedRequest() != null) {
                modifiedRequestDto = HttpRequestSerializer.toDto(pair.getModifiedRequest());
            }
            
            HttpResponseDto modifiedResponseDto = null;
            if (pair.getModifiedResponse() != null) {
                modifiedResponseDto = HttpResponseSerializer.toDto(pair.getModifiedResponse());
            }
            
        return new HttpRequestResponsePairDto(
                originalRequestDto,
                originalResponseDto,
                modifiedRequestDto,
                modifiedResponseDto,
                pair.wasRequestIntercepted,
                pair.wasResponseIntercepted,
                pair.wasRequestModified,
                pair.wasResponseModified,
                pair.wasModifiedRequestBodyBase64Encoded,
                pair.wasModifiedResponseBodyBase64Encoded,
                pair.wasRequestBodyBase64Encoded,
                pair.wasResponseBodyBase64Encoded,
                pair.toolSource,
                pair.originalRequestTime,
                pair.originalResponseTime,
                pair.modifiedRequestTime,
                pair.modifiedResponseTime,
                pair.getOriginalRequestNotes(),
                pair.getOriginalResponseNotes(),
                pair.getModifiedRequestNotes(),
                pair.getModifiedResponseNotes(),
                pair.isOriginalRequestHighlighted(),
                pair.isOriginalResponseHighlighted(),
                pair.isModifiedRequestHighlighted(),
                pair.isModifiedResponseHighlighted()
            );
    }
}
