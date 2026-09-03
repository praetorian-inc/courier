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

import burp.api.montoya.scanner.audit.issues.AuditIssue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.*;

class IssuesHandlerTest {
    @Test
    void collisionProneIssueDetailsProduceDistinctKeys() {
        AuditIssue first = mock(AuditIssue.class);
        AuditIssue second = mock(AuditIssue.class);
        when(first.baseUrl()).thenReturn("https://example.test");
        when(second.baseUrl()).thenReturn("https://example.test");
        when(first.name()).thenReturn("Issue");
        when(second.name()).thenReturn("Issue");
        when(first.detail()).thenReturn("Aa");
        when(second.detail()).thenReturn("BB");

        assertNotEquals(IssuesHandler.issueIdentity(first), IssuesHandler.issueIdentity(second));
    }
}
