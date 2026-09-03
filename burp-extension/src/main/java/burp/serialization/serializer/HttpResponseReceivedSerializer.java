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

import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.serialization.dto.HttpResponseReceivedDto;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class HttpResponseReceivedSerializer extends JsonSerializer<HttpResponseReceived> {
    @Override
    public void serialize(HttpResponseReceived response, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        HttpRequest request = response.initiatingRequest();
        generator.writeObject(new HttpResponseReceivedDto(
                response.body() == null ? "" : response.bodyToString(),
                response.messageId(),
                request != null && request.isInScope(),
                request == null ? "" : SerializationSupport.value(request.method()),
                request == null ? "" : SerializationSupport.value(request.path()),
                SerializationSupport.headers(response.headers()),
                request == null ? "" : SerializationSupport.value(request.url()),
                response.statusCode(),
                response.toolSource() == null ? "" : response.toolSource().toolType().toolName(),
                response.annotations() == null ? "" : response.annotations().notes(),
                response.annotations() != null && response.annotations().hasHighlightColor()));
    }
}
