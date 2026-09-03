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

import burp.model.Webflow;
import burp.serialization.dto.HttpRequestDto;
import burp.serialization.dto.HttpResponseDto;
import burp.serialization.util.JsonMapperUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WebflowJsonFormatter {
    private static final int MAX_STEPS = 300;
    private static final int MAX_EVIDENCE_ENTRIES = 300;
    private static final int MAX_BODY_PREVIEW = 4 * 1024;
    private static final int MAX_TOTAL_TEXT = 1_000_000;
    private final ObjectMapper objectMapper = JsonMapperUtil.getConfiguredMapper();

    String format(Webflow webflow) {
        try {
            PreviewBudget budget = new PreviewBudget();
            Map<String, Object> preview = new LinkedHashMap<>();
            preview.put("bounded", true);
            preview.put("maxSteps", MAX_STEPS);
            preview.put("maxEvidenceEntries", MAX_EVIDENCE_ENTRIES);
            preview.put("maxBodyPreviewCharacters", MAX_BODY_PREVIEW);
            preview.put("note", "Display preview only; the saved .courier recording retains complete data.");

            Map<String, Object> json = new LinkedHashMap<>();
            json.put("_preview", preview);
            json.put("id", budget.text(webflow.getId(), 2_048));
            json.put("name", budget.text(webflow.getName(), 2_048));
            json.put("description", budget.text(webflow.getDescription(), 8_192));
            json.put("projectName", budget.text(webflow.getProjectName(), 2_048));
            json.put("startUrl", budget.text(webflow.getStartUrl(), 8_192));
            json.put("createdAt", webflow.getCreatedAt() == null ? null : webflow.getCreatedAt().toString());
            json.put("lastModified", webflow.getLastModified() == null ? null : webflow.getLastModified().toString());
            json.put("stepsCount", webflow.getSteps().size());
            json.put("steps", steps(webflow.getSteps(), budget));
            json.put("omittedSteps", Math.max(0, webflow.getSteps().size() - MAX_STEPS));
            List<Map<String, Object>> uncorrelatedRequests = requests(
                    webflow.getUncorrelatedRequests(), budget);
            List<Map<String, Object>> uncorrelatedResponses = responses(
                    webflow.getUncorrelatedResponses(), budget);
            json.put("uncorrelatedRequests", uncorrelatedRequests);
            json.put("omittedUncorrelatedRequests", Math.max(0,
                    webflow.getUncorrelatedRequests().size() - uncorrelatedRequests.size()));
            json.put("uncorrelatedResponses", uncorrelatedResponses);
            json.put("omittedUncorrelatedResponses", Math.max(0,
                    webflow.getUncorrelatedResponses().size() - uncorrelatedResponses.size()));
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize webflow JSON preview", exception);
        }
    }

    private List<Map<String, Object>> steps(List<Webflow.WebflowStep> source, PreviewBudget budget) {
        List<Map<String, Object>> steps = new ArrayList<>();
        for (int index = 0; index < Math.min(source.size(), MAX_STEPS); index++) {
            ensureNotInterrupted();
            Webflow.WebflowStep step = source.get(index);
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("action", budget.text(step.getAction(), 1_024));
            json.put("selector", budget.text(step.getSelector(), 4_096));
            json.put("value", budget.text(step.getValue(), 4_096));
            json.put("description", budget.text(step.getDescription(), 4_096));
            json.put("order", step.getOrder());
            json.put("timestamp", step.getTimestamp());
            json.put("url", budget.text(step.getUrl(), 8_192));
            json.put("elementText", budget.text(step.getElementText(), 2_048));
            json.put("stepType", step.getStepType());
            json.put("playwrightCode", budget.text(safePlaywrightCode(step), 8_192));
            List<Map<String, Object>> requests = requests(step.getCorrelatedRequests(), budget);
            List<Map<String, Object>> responses = responses(step.getCorrelatedResponses(), budget);
            json.put("correlatedRequests", requests);
            json.put("omittedCorrelatedRequests", Math.max(0,
                    size(step.getCorrelatedRequests()) - requests.size()));
            json.put("correlatedResponses", responses);
            json.put("omittedCorrelatedResponses", Math.max(0,
                    size(step.getCorrelatedResponses()) - responses.size()));
            steps.add(json);
        }
        return steps;
    }

    private List<Map<String, Object>> requests(List<HttpRequestDto> source, PreviewBudget budget) {
        List<Map<String, Object>> requests = new ArrayList<>();
        if (source == null) {
            return requests;
        }
        for (HttpRequestDto request : source) {
            ensureNotInterrupted();
            if (!budget.takeEvidence()) {
                break;
            }
            Map<String, Object> json = new LinkedHashMap<>();
            putBody(json, request.getBody(), budget);
            json.put("messageId", request.getMessageId());
            json.put("inScope", request.isInScope());
            json.put("method", budget.text(request.getMethod(), 128));
            json.put("path", budget.text(request.getPath(), 8_192));
            json.put("headers", headers(request.getHeaders(), budget));
            json.put("url", budget.text(request.getUrl(), 8_192));
            requests.add(json);
        }
        return requests;
    }

    private List<Map<String, Object>> responses(List<HttpResponseDto> source, PreviewBudget budget) {
        List<Map<String, Object>> responses = new ArrayList<>();
        if (source == null) {
            return responses;
        }
        for (HttpResponseDto response : source) {
            ensureNotInterrupted();
            if (!budget.takeEvidence()) {
                break;
            }
            Map<String, Object> json = new LinkedHashMap<>();
            putBody(json, response.getBody(), budget);
            json.put("messageId", response.getMessageId());
            json.put("inScope", response.isInScope());
            json.put("method", budget.text(response.getMethod(), 128));
            json.put("path", budget.text(response.getPath(), 8_192));
            json.put("headers", headers(response.getHeaders(), budget));
            json.put("url", budget.text(response.getUrl(), 8_192));
            json.put("statusCode", response.getStatusCode());
            responses.add(json);
        }
        return responses;
    }

    private List<Map<String, String>> headers(List<Map<String, String>> source, PreviewBudget budget) {
        List<Map<String, String>> headers = new ArrayList<>();
        if (source == null) {
            return headers;
        }
        for (int index = 0; index < Math.min(100, source.size()); index++) {
            Map<String, String> header = new LinkedHashMap<>();
            source.get(index).forEach((name, value) ->
                    header.put(budget.text(name, 256), budget.text(value, 2_048)));
            headers.add(header);
        }
        return headers;
    }

    private void putBody(Map<String, Object> json, String body, PreviewBudget budget) {
        String value = body == null ? "" : body;
        int previewLength = Math.min(value.length(), MAX_BODY_PREVIEW);
        json.put("body", budget.text(value.substring(0, previewLength), MAX_BODY_PREVIEW));
        json.put("bodyLength", value.length());
        json.put("bodyTruncated", value.length() > previewLength);
    }

    private static void ensureNotInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new java.util.concurrent.CancellationException("JSON preview cancelled");
        }
    }

    private static String safePlaywrightCode(Webflow.WebflowStep step) {
        try {
            return step.toPlaywrightCode();
        } catch (Exception exception) {
            return "";
        }
    }

    private static int size(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private static final class PreviewBudget {
        private int remainingText = MAX_TOTAL_TEXT;
        private int remainingEvidence = MAX_EVIDENCE_ENTRIES;

        String text(String value, int fieldMaximum) {
            if (value == null || remainingText <= 0) {
                return "";
            }
            int length = Math.min(Math.min(value.length(), fieldMaximum), remainingText);
            remainingText -= length;
            return length < value.length() ? value.substring(0, length) + "…" : value;
        }

        boolean takeEvidence() {
            return remainingEvidence-- > 0;
        }
    }
}
