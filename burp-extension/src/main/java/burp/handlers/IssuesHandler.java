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

package burp.handlers;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.scanner.audit.AuditIssueHandler;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.controller.ConfigurationController;
import burp.controller.ConnectionController;
import burp.serialization.dto.AuditIssueDto;
import burp.serialization.serializer.AuditIssueSerializer;
import burp.utils.MonitoredHashMap;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;


public class IssuesHandler implements AuditIssueHandler {
    private final CapturePolicy capturePolicy;
    private volatile MonitoredHashMap<String, AuditIssueDto> issues;

    public IssuesHandler(ConfigurationController configuration, MontoyaApi api, ConnectionController connection) {
        capturePolicy = new CapturePolicy(configuration, api, connection);
    }

    public void setIssues(MonitoredHashMap<String, AuditIssueDto> issues) {
        this.issues = issues;
    }

    @Override
    public void handleNewAuditIssue(AuditIssue auditIssue) {
        MonitoredHashMap<String, AuditIssueDto> currentIssues = issues;
        if (currentIssues == null || !capturePolicy.shouldCapture(auditIssue.baseUrl(), "")) {
            return;
        }

        BinaryMessageSupport.EncodedPairs encodedPairs =
                BinaryMessageSupport.encodePairs(auditIssue.requestResponses());

        AuditIssueDto issueToStore = AuditIssueSerializer.toDto(
                auditIssue, encodedPairs.pairs());
        currentIssues.put(issueIdentity(auditIssue), issueToStore);
    }

    static String issueIdentity(AuditIssue auditIssue) {
        String identity = String.valueOf(auditIssue.baseUrl()) + '\0'
                + String.valueOf(auditIssue.name()) + '\0'
                + String.valueOf(auditIssue.detail());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to identify audit issue", exception);
        }
    }
}
