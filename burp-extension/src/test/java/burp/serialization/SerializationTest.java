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

package burp.serialization;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.collaborator.Interaction;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.organizer.OrganizerItem;
import burp.api.montoya.organizer.OrganizerItemStatus;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueDefinition;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.model.HttpRequestResponsePair;
import burp.serialization.dto.ConversationRequestDto;
import burp.serialization.dto.HttpRequestDto;
import burp.serialization.dto.HttpResponseDto;
import burp.serialization.dto.MessageResponseDto;
import burp.serialization.serializer.HttpRequestSerializer;
import burp.serialization.serializer.HttpResponseSerializer;
import burp.serialization.serializer.AuditIssueSerializer;
import burp.serialization.serializer.InteractionSerializer;
import burp.serialization.serializer.OrganizerItemSerializer;
import burp.serialization.util.JsonMapperUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SerializationTest {
    private final ObjectMapper mapper = JsonMapperUtil.getConfiguredMapper();

    @Test
    void requestDtoConversionHasOneCanonicalExtractionPath() {
        HttpRequest request = mock(HttpRequest.class);
        ByteArray body = mock(ByteArray.class);
        HttpHeader header = mock(HttpHeader.class);
        when(request.body()).thenReturn(body);
        when(request.bodyToString()).thenReturn("request-body");
        when(request.method()).thenReturn("POST");
        when(request.path()).thenReturn("/submit");
        when(request.url()).thenReturn("https://example.test/submit");
        when(request.isInScope()).thenReturn(true);
        when(request.headers()).thenReturn(List.of(header));
        when(header.name()).thenReturn("X-Test");
        when(header.value()).thenReturn("yes");

        HttpRequestDto dto = HttpRequestSerializer.toDto(request);
        assertEquals("request-body", dto.getBody());
        assertEquals(List.of(java.util.Map.of("X-Test", "yes")), dto.getHeaders());
    }

    @Test
    void responseConversionUsesBodyTextRatherThanObjectToString() {
        HttpResponse response = mock(HttpResponse.class);
        ByteArray body = mock(ByteArray.class);
        when(response.body()).thenReturn(body);
        when(response.bodyToString()).thenReturn("response-body");
        when(response.headers()).thenReturn(List.of());
        when(response.statusCode()).thenReturn((short) 201);

        HttpResponseDto dto = HttpResponseSerializer.toDto(response);
        assertEquals("response-body", dto.getBody());
        assertEquals(201, dto.getStatusCode());
    }

    @Test
    void defaultDtoSerializationOmitsEmptyOptionalConversationFields() throws Exception {
        ConversationRequestDto request = new ConversationRequestDto("hello", "query");
        JsonNode json = mapper.readTree(mapper.writeValueAsString(request));
        assertFalse(json.has("conversationId"));
        assertEquals("hello", json.get("message").asText());

        ConversationRequestDto emptyMessage = new ConversationRequestDto("", "query");
        assertTrue(mapper.readTree(mapper.writeValueAsString(emptyMessage)).has("message"));
    }

    @Test
    void diagnosticStringsDoNotExposeMessageContentOrKeys() {
        ConversationRequestDto request = new ConversationRequestDto("conversation", "message-value", "query");
        MessageResponseDto response = new MessageResponseDto();
        response.setKey("key-value");
        response.setContent("content-value");

        assertFalse(request.toString().contains("message-value"));
        assertFalse(response.toString().contains("key-value"));
        assertFalse(response.toString().contains("content-value"));
    }

    @Test
    void requestAndResponseExtractionFailuresArePropagated() {
        HttpRequest request = mock(HttpRequest.class);
        when(request.body()).thenThrow(new IllegalStateException("request unavailable"));
        HttpResponse response = mock(HttpResponse.class);
        when(response.body()).thenThrow(new IllegalStateException("response unavailable"));

        assertThrows(IllegalArgumentException.class, () -> HttpRequestSerializer.toDto(request));
        assertThrows(IllegalArgumentException.class, () -> HttpResponseSerializer.toDto(response));
        assertThrows(Exception.class, () -> mapper.writeValueAsString(
                new HttpRequestResponsePair().setOriginalRequest(request, 1)));
    }

    @Test
    void auditIssueConversionRetainsOriginalCollaboratorInteractions() {
        AuditIssue issue = mock(AuditIssue.class);
        AuditIssueDefinition definition = mock(AuditIssueDefinition.class);
        when(issue.baseUrl()).thenReturn("https://example.test");
        when(issue.name()).thenReturn("Interaction issue");
        when(issue.detail()).thenReturn("detail");
        when(issue.remediation()).thenReturn("fix");
        when(issue.severity()).thenReturn(AuditIssueSeverity.HIGH);
        when(issue.confidence()).thenReturn(AuditIssueConfidence.CERTAIN);
        when(issue.definition()).thenReturn(definition);
        when(definition.background()).thenReturn("background");
        when(definition.remediation()).thenReturn("definition fix");
        when(issue.collaboratorInteractions()).thenReturn(List.of(mock(Interaction.class)));

        var dto = AuditIssueSerializer.toDto(issue, List.of());

        assertEquals(1, dto.getCollaboratorInteractions().size());
    }

    @Test
    void interactionConversionUsesTheClientAddress() throws Exception {
        Interaction interaction = mock(Interaction.class);
        when(interaction.id()).thenReturn(mock(burp.api.montoya.collaborator.InteractionId.class));
        when(interaction.type()).thenReturn(burp.api.montoya.collaborator.InteractionType.HTTP);
        when(interaction.timeStamp()).thenReturn(ZonedDateTime.now());
        when(interaction.dnsDetails()).thenReturn(Optional.empty());
        when(interaction.customData()).thenReturn(Optional.empty());
        when(interaction.clientIp()).thenReturn(InetAddress.getByName("192.0.2.10"));
        when(interaction.toString()).thenReturn("IP: 198.51.100.20");

        var dto = InteractionSerializer.toDto(interaction);

        assertEquals("192.0.2.10", dto.getClientIp());
    }

    @Test
    void organizerConversionIncludesMutableStatusAndHighlight() {
        OrganizerItem item = mock(OrganizerItem.class);
        Annotations annotations = mock(Annotations.class);
        when(item.id()).thenReturn(7);
        when(item.status()).thenReturn(OrganizerItemStatus.IN_PROGRESS);
        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("mailto:user@example.test");
        when(request.method()).thenReturn("GET");
        when(item.request()).thenReturn(request);
        when(item.annotations()).thenReturn(annotations);
        when(annotations.notes()).thenReturn("reviewing");
        when(annotations.hasHighlightColor()).thenReturn(true);
        when(annotations.highlightColor()).thenReturn(HighlightColor.ORANGE);

        var dto = OrganizerItemSerializer.toDto(item);

        assertEquals("IN_PROGRESS", dto.getStatus());
        assertEquals("", dto.getHost());
        assertEquals("", dto.getPath());
        assertEquals("ORANGE", dto.getHighlightColor());
        assertEquals("reviewing", dto.getNotes());
    }

    @Test
    void pairSerializerPreservesWireFieldNames() throws Exception {
        HttpRequest request = mock(HttpRequest.class);
        ByteArray body = mock(ByteArray.class);
        when(request.body()).thenReturn(body);
        when(request.bodyToString()).thenReturn("payload");
        when(request.method()).thenReturn("POST");
        when(request.path()).thenReturn("/path");
        when(request.url()).thenReturn("https://example.test/path");
        when(request.headers()).thenReturn(List.of());
        HttpRequestResponsePair pair = new HttpRequestResponsePair()
                .setOriginalRequest(request, 10L)
                .setToolSource("Proxy");

        JsonNode json = mapper.readTree(mapper.writeValueAsString(pair));

        assertEquals("Proxy", json.get("toolSource").asText());
        assertEquals("payload", json.get("originalRequest").get("body").asText());
        assertTrue(json.has("wasRequestModified"));
    }
}
