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

import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.serialization.dto.HttpRequestToBeSentDto;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class HttpRequestToBeSentSerializer extends JsonSerializer<HttpRequestToBeSent> {
    @Override
    public void serialize(HttpRequestToBeSent request, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        generator.writeObject(new HttpRequestToBeSentDto(
                request.body() == null ? "" : request.bodyToString(),
                request.messageId(),
                request.isInScope(),
                SerializationSupport.value(request.method()),
                SerializationSupport.value(request.path()),
                SerializationSupport.headers(request.headers()),
                SerializationSupport.value(request.url()),
                request.toolSource() == null ? "" : request.toolSource().toolType().toolName(),
                request.annotations() == null ? "" : request.annotations().notes(),
                request.annotations() != null && request.annotations().hasHighlightColor()));
    }
}
