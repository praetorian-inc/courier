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

package burp.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebflowActionMapperTest {
    @Test
    void providesOneCanonicalActionMapping() {
        assertEquals(Webflow.WebflowStep.StepType.NAVIGATION,
                WebflowActionMapper.toStepType("goto"));
        assertEquals(Webflow.WebflowStep.StepType.FILL,
                WebflowActionMapper.toStepType("input"));
        assertEquals(Webflow.WebflowStep.StepType.SUBMIT,
                WebflowActionMapper.toStepType("submit"));
        assertEquals(Webflow.WebflowStep.StepType.OTHER,
                WebflowActionMapper.toStepType("custom"));
    }

    @Test
    void createsStableDescriptionsWithNullSafeInputs() {
        assertEquals("Click on 'Save'", WebflowActionMapper.describe("click", "#save", "", " Save "));
        assertEquals("Fill '#name' with 'example-value'",
                WebflowActionMapper.describe("fill", "#name", "example-value", null));
        assertDoesNotThrow(() -> WebflowActionMapper.describe(null, null, null, null));
    }

    @Test
    void generatesEscapedReplayCodeForEveryCoreAction() {
        Webflow.WebflowStep fill = new Webflow.WebflowStep(
                Webflow.WebflowStep.StepType.FILL, "#field\\\"", "line 1\nline 2", "", 1);
        Webflow.WebflowStep press = new Webflow.WebflowStep(
                Webflow.WebflowStep.StepType.PRESS, "#field", "Enter", "", 2);
        Webflow.WebflowStep submit = new Webflow.WebflowStep(
                Webflow.WebflowStep.StepType.SUBMIT, "form#login", "", "", 3);
        Webflow.WebflowStep upload = new Webflow.WebflowStep(
                Webflow.WebflowStep.StepType.UPLOAD, "#file", "report.txt", "", 4);

        assertEquals("page.fill(\"#field\\\\\\\"\", \"line 1\\nline 2\");",
                fill.toPlaywrightCode());
        assertEquals("page.press(\"#field\", \"Enter\");", press.toPlaywrightCode());
        assertEquals("page.locator(\"form#login\").evaluate("
                + "\"form => form.requestSubmit()\");", submit.toPlaywrightCode());
        assertEquals("page.setInputFiles(\"#file\", java.nio.file.Paths.get(\"report.txt\"));",
                upload.toPlaywrightCode());
    }
}
