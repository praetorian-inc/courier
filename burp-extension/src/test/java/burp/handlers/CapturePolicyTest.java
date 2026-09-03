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
import burp.api.montoya.scope.Scope;
import burp.controller.ConfigurationController;
import burp.controller.ConnectionController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CapturePolicyTest {
    @Test
    void appliesConnectionScopeAndExtensionRulesOnce() {
        ConfigurationController configuration = mock(ConfigurationController.class);
        ConnectionController connection = mock(ConnectionController.class);
        MontoyaApi api = mock(MontoyaApi.class);
        Scope scope = mock(Scope.class);
        when(api.scope()).thenReturn(scope);
        when(connection.isEnabled()).thenReturn(true);
        when(configuration.isRespectScopeEnabled()).thenReturn(true);
        when(scope.isInScope("https://example.test")).thenReturn(true);
        when(configuration.isExcludedExtension("png")).thenReturn(false);

        CapturePolicy policy = new CapturePolicy(configuration, api, connection);
        assertTrue(policy.shouldCapture("https://example.test", "png"));

        when(configuration.isExcludedExtension("png")).thenReturn(true);
        assertFalse(policy.shouldCapture("https://example.test", "png"));
        when(connection.isEnabled()).thenReturn(false);
        assertFalse(policy.shouldCapture("https://example.test", "html"));
    }
}
