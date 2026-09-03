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

import burp.api.montoya.collaborator.Interaction;
import burp.serialization.dto.InteractionDto;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class InteractionSerializer extends JsonSerializer<Interaction> {
    @Override
    public void serialize(Interaction interaction, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        generator.writeObject(toDto(interaction));
    }

    public static InteractionDto toDto(Interaction interaction) {
        if (interaction == null) {
            return null;
        }
        InteractionDto dto = new InteractionDto();
        try {
            dto.setId(interaction.id().toString());
            dto.setType(interaction.type().toString());
            dto.setHost(interaction.dnsDetails() == null ? "" : interaction.dnsDetails().toString());
            dto.setTimeStamp(interaction.timeStamp() == null ? "" : interaction.timeStamp().toString());
            dto.setCustomData(interaction.customData().orElse(""));
            dto.setClientIp(interaction.clientIp() == null
                    ? "" : interaction.clientIp().getHostAddress());
        } catch (Exception exception) {
            dto.setId("unknown");
            dto.setType("unknown");
            dto.setHost("");
            dto.setTimeStamp("");
            dto.setCustomData("");
            dto.setClientIp("");
        }
        dto.setPort(0);
        dto.setProtocol("");
        dto.setQuery("");
        return dto;
    }

}
