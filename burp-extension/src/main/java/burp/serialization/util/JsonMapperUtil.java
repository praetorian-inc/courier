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

package burp.serialization.util;

import burp.api.montoya.collaborator.Interaction;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.organizer.OrganizerItem;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.model.HttpRequestResponsePair;
import burp.model.Webflow;
import burp.serialization.serializer.AuditIssueSerializer;
import burp.serialization.serializer.HttpRequestResponsePairSerializer;
import burp.serialization.serializer.HttpRequestSerializer;
import burp.serialization.serializer.HttpRequestToBeSentSerializer;
import burp.serialization.serializer.HttpResponseReceivedSerializer;
import burp.serialization.serializer.HttpResponseSerializer;
import burp.serialization.serializer.InteractionSerializer;
import burp.serialization.serializer.OrganizerItemSerializer;
import burp.serialization.serializer.WebflowSerializer;
import burp.serialization.serializer.WebflowStepSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public final class JsonMapperUtil {
    private static final ObjectMapper OBJECT_MAPPER = createConfiguredMapper();

    private JsonMapperUtil() {
    }

    public static ObjectMapper getConfiguredMapper() {
        return OBJECT_MAPPER;
    }

    private static ObjectMapper createConfiguredMapper() {
        SimpleModule module = new SimpleModule("BurpApiSerializers");
        module.addSerializer(HttpRequestToBeSent.class, new HttpRequestToBeSentSerializer());
        module.addSerializer(HttpResponseReceived.class, new HttpResponseReceivedSerializer());
        module.addSerializer(HttpRequest.class, new HttpRequestSerializer());
        module.addSerializer(HttpResponse.class, new HttpResponseSerializer());
        module.addSerializer(HttpRequestResponsePair.class, new HttpRequestResponsePairSerializer());
        module.addSerializer(AuditIssue.class, new AuditIssueSerializer());
        module.addSerializer(Interaction.class, new InteractionSerializer());
        module.addSerializer(Webflow.class, new WebflowSerializer());
        module.addSerializer(Webflow.WebflowStep.class, new WebflowStepSerializer());
        module.addSerializer(OrganizerItem.class, new OrganizerItemSerializer());
        return new ObjectMapper().registerModule(module);
    }
}
