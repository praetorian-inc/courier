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
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.serialization.dto.AuditIssueDto;
import burp.serialization.dto.HttpRequestDto;
import burp.serialization.dto.HttpResponseDto;
import burp.serialization.dto.InteractionDto;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AuditIssueSerializer extends JsonSerializer<AuditIssue> {
    @Override
    public void serialize(AuditIssue issue, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        generator.writeObject(toDto(issue));
    }

    public static AuditIssueDto toDto(AuditIssue issue) {
        return issue == null ? null : toDto(issue, issue.requestResponses());
    }

    public static AuditIssueDto toDto(AuditIssue issue, List<HttpRequestResponse> requestResponses) {
        if (issue == null) {
            return null;
        }

        AuditIssueDto dto = new AuditIssueDto();
        dto.setBaseUrl(safe(() -> issue.baseUrl()));
        dto.setConfidence(safe(() -> issue.confidence().toString()));
        dto.setName(safe(issue::name));
        dto.setSeverity(safe(() -> issue.severity().toString().toLowerCase(Locale.ROOT)));
        dto.setDetail(joinSections(safe(issue::detail), safe(() -> issue.definition().background())));
        dto.setRemediation(joinSections(
                safe(issue::remediation), safe(() -> issue.definition().remediation())));

        List<InteractionDto> interactions = new ArrayList<>();
        try {
            for (Interaction interaction : issue.collaboratorInteractions()) {
                InteractionDto interactionDto = InteractionSerializer.toDto(interaction);
                if (interactionDto != null) {
                    interactions.add(interactionDto);
                }
            }
        } catch (Exception ignored) {
        }
        dto.setCollaboratorInteractions(interactions);

        List<HttpRequestDto> requests = new ArrayList<>();
        List<HttpResponseDto> responses = new ArrayList<>();
        try {
            requestResponses.forEach(pair -> {
                HttpRequestDto request = HttpRequestSerializer.toDto(pair.request());
                HttpResponseDto response = HttpResponseSerializer.toDto(pair.response());
                if (request != null) {
                    requests.add(request);
                }
                if (response != null) {
                    responses.add(response);
                }
            });
        } catch (Exception ignored) {
        }
        dto.setRequests(requests);
        dto.setResponses(responses);
        return dto;
    }

    private static String safe(ValueSupplier supplier) {
        try {
            String value = supplier.get();
            return value == null ? "" : value;
        } catch (Exception exception) {
            return "";
        }
    }

    private static String joinSections(String first, String second) {
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        return first + "\n" + second;
    }

    @FunctionalInterface
    private interface ValueSupplier {
        String get();
    }
}
