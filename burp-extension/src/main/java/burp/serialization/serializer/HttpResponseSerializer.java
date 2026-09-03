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

import burp.api.montoya.http.message.responses.HttpResponse;
import burp.serialization.dto.HttpResponseDto;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class HttpResponseSerializer extends JsonSerializer<HttpResponse> {
    @Override
    public void serialize(HttpResponse response, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        generator.writeObject(toDto(response));
    }

    public static HttpResponseDto toDto(HttpResponse response) {
        if (response == null) {
            return null;
        }
        try {
            return new HttpResponseDto(
                    response.body() == null ? "" : response.bodyToString(),
                    0,
                    false,
                    "",
                    "",
                    SerializationSupport.headers(response.headers()),
                    "",
                    response.statusCode());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to serialize HTTP response", exception);
        }
    }
}
