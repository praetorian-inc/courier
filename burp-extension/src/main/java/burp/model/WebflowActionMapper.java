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

import java.util.Locale;

public final class WebflowActionMapper {
    private WebflowActionMapper() {
    }

    public static Webflow.WebflowStep.StepType toStepType(String action) {
        if (action == null) {
            return Webflow.WebflowStep.StepType.OTHER;
        }
        return switch (action.toLowerCase(Locale.ROOT)) {
            case "navigate", "goto", "navigation" -> Webflow.WebflowStep.StepType.NAVIGATION;
            case "click" -> Webflow.WebflowStep.StepType.CLICK;
            case "submit" -> Webflow.WebflowStep.StepType.SUBMIT;
            case "fill", "input", "type" -> Webflow.WebflowStep.StepType.FILL;
            case "select", "selectoption" -> Webflow.WebflowStep.StepType.SELECT;
            case "check" -> Webflow.WebflowStep.StepType.CHECK;
            case "uncheck" -> Webflow.WebflowStep.StepType.UNCHECK;
            case "press" -> Webflow.WebflowStep.StepType.PRESS;
            case "wait" -> Webflow.WebflowStep.StepType.WAIT;
            case "hover" -> Webflow.WebflowStep.StepType.HOVER;
            case "upload" -> Webflow.WebflowStep.StepType.UPLOAD;
            case "scroll" -> Webflow.WebflowStep.StepType.SCROLL;
            case "assert", "expect" -> Webflow.WebflowStep.StepType.ASSERTION;
            case "request", "response" -> Webflow.WebflowStep.StepType.NETWORK;
            default -> Webflow.WebflowStep.StepType.OTHER;
        };
    }

    public static String describe(String action, String selector, String value, String text) {
        String type = action == null ? "" : action.toLowerCase(Locale.ROOT);
        String safeSelector = selector == null ? "" : selector;
        String safeValue = value == null ? "" : value;
        String safeText = text == null ? "" : text.trim();
        return switch (type) {
            case "navigate", "goto", "navigation" -> safeValue.isEmpty()
                    ? "Navigate to page" : "Navigate to " + safeValue;
            case "click" -> safeText.isEmpty()
                    ? "Click element " + safeSelector : "Click on '" + safeText + "'";
            case "fill", "input", "type" -> safeValue.isEmpty()
                    ? "Fill field " + safeSelector
                    : "Fill '" + safeSelector + "' with '" + safeValue + "'";
            case "submit" -> "Submit form";
            case "selectoption", "select" -> safeValue.isEmpty()
                    ? "Select option from " + safeSelector
                    : "Select '" + safeValue + "' from " + safeSelector;
            case "check" -> "Check checkbox " + safeSelector;
            case "uncheck" -> "Uncheck checkbox " + safeSelector;
            case "press" -> safeValue.isEmpty()
                    ? "Press key on " + safeSelector
                    : "Press '" + safeValue + "' key on " + safeSelector;
            case "hover" -> "Hover over " + safeSelector;
            case "scroll" -> "Scroll to " + safeSelector;
            default -> type + " action on " + safeSelector;
        };
    }
}
