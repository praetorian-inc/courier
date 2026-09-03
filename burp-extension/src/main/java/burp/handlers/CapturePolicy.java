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
import burp.controller.ConfigurationController;
import burp.controller.ConnectionController;
import burp.utils.Utils;

final class CapturePolicy {
    private final ConfigurationController configuration;
    private final MontoyaApi api;
    private final ConnectionController connection;

    CapturePolicy(ConfigurationController configuration, MontoyaApi api, ConnectionController connection) {
        this.configuration = configuration;
        this.api = api;
        this.connection = connection;
    }

    boolean shouldCapture(String url, String fileExtension) {
        return connection.isEnabled()
                && Utils.isInScope(url, configuration, api)
                && !configuration.isExcludedExtension(fileExtension);
    }
}
