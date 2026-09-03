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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ConfigurationControllerTest {
    @Test
    void normalizesAndDefensivelyExposesExcludedExtensions() {
        ConfigurationController controller = new ConfigurationController(mock(LogController.class));

        assertFalse(controller.isExcludedExtension("png"));
        assertTrue(controller.isEnableAITrainingEnabled());
        controller.handleExcludedExtensionsChange(" .PNG, js, JS, , .Css ");

        assertTrue(controller.isExcludedExtension("png"));
        assertTrue(controller.isExcludedExtension(".JS"));
        assertEquals(3, controller.getExcludedExtensions().size());
        assertThrows(UnsupportedOperationException.class,
                () -> controller.getExcludedExtensions().add("gif"));
    }

    @Test
    void acceptsEmptyConfiguration() {
        ConfigurationController controller = new ConfigurationController(mock(LogController.class));
        controller.handleExcludedExtensionsChange(null);
        assertTrue(controller.getExcludedExtensions().isEmpty());
        assertFalse(controller.isExcludedExtension(null));
    }
}
