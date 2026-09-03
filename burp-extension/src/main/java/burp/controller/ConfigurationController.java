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

package burp.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigurationController {
    private final LogController logger;

    private volatile boolean respectScope;
    private volatile boolean showProxyData;
    private volatile boolean enableAITraining = true;
    private volatile Set<String> excludedExtensions = Collections.emptySet();

    public ConfigurationController(LogController logger) {
        this.logger = logger;
    }

    public void handleRespectScopeChange(boolean enabled) {
        respectScope = enabled;
        logger.logInfo("Scope filtering " + (enabled ? "enabled" : "disabled"));
        logger.logInfo(enabled
                ? "Only requests matching Burp's Target scope will be captured"
                : "All requests will be captured regardless of scope");
    }

    public void handleLogLevelChange(int selectedIndex) {
        logger.setCurrentLogLevel(selectedIndex);
    }

    public void handleShowProxyDataChange(boolean enabled) {
        showProxyData = enabled;
        logger.logInfo("Proxy data display " + (enabled ? "enabled" : "disabled"));
    }

    public void handleEnableAITrainingChange(boolean enabled) {
        enableAITraining = enabled;
        logger.logInfo("AI training data collection " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isExcludedExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return excludedExtensions.contains(normalizeExtension(extension));
    }

    public void handleExcludedExtensionsChange(String extensions) {
        excludedExtensions = parseExtensions(extensions);
        logger.logInfo("Excluded extensions: " + String.join(",", excludedExtensions));
    }

    public Set<String> getExcludedExtensions() {
        return excludedExtensions;
    }

    public boolean isRespectScopeEnabled() {
        return respectScope;
    }

    public boolean isShowProxyDataEnabled() {
        return showProxyData;
    }

    public boolean isEnableAITrainingEnabled() {
        return enableAITraining;
    }

    private static Set<String> parseExtensions(String extensions) {
        if (extensions == null || extensions.isBlank()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(Arrays.stream(extensions.split(","))
                .map(ConfigurationController::normalizeExtension)
                .filter(extension -> !extension.isEmpty())
                .collect(Collectors.toSet()));
    }

    private static String normalizeExtension(String extension) {
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }
}
