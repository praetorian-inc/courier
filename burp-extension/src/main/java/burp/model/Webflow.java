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

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import burp.serialization.dto.HttpRequestDto;
import burp.serialization.dto.HttpResponseDto;

public class Webflow {
    private String id;
    private String name;
    private String description;
    private String projectName;
    private String startUrl;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private List<WebflowStep> steps;
    private List<HttpRequestDto> uncorrelatedRequests;
    private List<HttpResponseDto> uncorrelatedResponses;
    
    public Webflow() {
        this.steps = new CopyOnWriteArrayList<>();
        this.uncorrelatedRequests = new CopyOnWriteArrayList<>();
        this.uncorrelatedResponses = new CopyOnWriteArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
    }
    
    public Webflow(String name, String description, String projectName, String startUrl) {
        this();
        this.name = name;
        this.description = description;
        this.projectName = projectName;
        this.startUrl = startUrl;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.lastModified = LocalDateTime.now();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.lastModified = LocalDateTime.now();
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
        this.lastModified = LocalDateTime.now();
    }
    
    
    public String getStartUrl() {
        return startUrl;
    }
    
    public void setStartUrl(String startUrl) {
        this.startUrl = startUrl;
        this.lastModified = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
    
    public List<WebflowStep> getSteps() {
        return steps;
    }
    
    public void setSteps(List<WebflowStep> steps) {
        this.steps = new CopyOnWriteArrayList<>(steps == null ? List.of() : steps);
        this.lastModified = LocalDateTime.now();
    }
    
    public void addStep(WebflowStep step) {
        this.steps.add(step);
        this.lastModified = LocalDateTime.now();
    }
    
    public void removeStep(WebflowStep step) {
        this.steps.remove(step);
        this.lastModified = LocalDateTime.now();
    }
    
    public List<HttpRequestDto> getUncorrelatedRequests() {
        return uncorrelatedRequests;
    }
    
    public void setUncorrelatedRequests(List<HttpRequestDto> uncorrelatedRequests) {
        this.uncorrelatedRequests = new CopyOnWriteArrayList<>(
                uncorrelatedRequests == null ? List.of() : uncorrelatedRequests);
        this.lastModified = LocalDateTime.now();
    }
    
    public void addUncorrelatedRequest(HttpRequestDto request) {
        this.uncorrelatedRequests.add(request);
        this.lastModified = LocalDateTime.now();
    }
    
    public List<HttpResponseDto> getUncorrelatedResponses() {
        return uncorrelatedResponses;
    }
    
    public void setUncorrelatedResponses(List<HttpResponseDto> uncorrelatedResponses) {
        this.uncorrelatedResponses = new CopyOnWriteArrayList<>(
                uncorrelatedResponses == null ? List.of() : uncorrelatedResponses);
        this.lastModified = LocalDateTime.now();
    }
    
    public void addUncorrelatedResponse(HttpResponseDto response) {
        this.uncorrelatedResponses.add(response);
        this.lastModified = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return name + " (" + steps.size() + " steps)";
    }
    
    // Inner class for webflow steps
    public static class WebflowStep {
        private String action;
        private String selector;
        private String value;
        private String description;
        private int order;
        private long timestamp;
        private String url; // The URL where this action occurred
        private String elementText; // Text content of the element if applicable
        private StepType stepType;
        private List<HttpRequestDto> correlatedRequests; // HTTP requests triggered by this action
        private List<HttpResponseDto> correlatedResponses; // HTTP responses from correlated requests
        
        public enum StepType {
            NAVIGATION,    // Navigate to URL
            CLICK,         // Click on element
            SUBMIT,        // Submit a form
            FILL,          // Fill input field
            SELECT,        // Select from dropdown
            CHECK,         // Check checkbox
            UNCHECK,       // Uncheck checkbox
            PRESS,         // Press key
            WAIT,          // Wait for element/condition
            HOVER,         // Hover over element
            UPLOAD,        // Upload file
            SCROLL,        // Scroll action
            ASSERTION,     // Assert something about page
            NETWORK,       // Network request/response
            OTHER          // Other custom actions
        }
        
        public WebflowStep() {
            this.timestamp = System.currentTimeMillis();
            this.correlatedRequests = new CopyOnWriteArrayList<>();
            this.correlatedResponses = new CopyOnWriteArrayList<>();
        }
        
        public WebflowStep(String action, String selector, String value, String description, int order) {
            this();
            this.action = action;
            this.selector = selector;
            this.value = value;
            this.description = description;
            this.order = order;
            this.stepType = determineStepType(action);
        }
        
        public WebflowStep(StepType stepType, String selector, String value, String description, int order) {
            this();
            this.stepType = stepType;
            this.action = stepType.name().toLowerCase();
            this.selector = selector;
            this.value = value;
            this.description = description;
            this.order = order;
        }
        
        private StepType determineStepType(String action) {
            return WebflowActionMapper.toStepType(action);
        }
        
        // Getters and Setters
        public String getAction() {
            return action;
        }
        
        public void setAction(String action) {
            this.action = action;
        }
        
        public String getSelector() {
            return selector;
        }
        
        public void setSelector(String selector) {
            this.selector = selector;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public int getOrder() {
            return order;
        }
        
        public void setOrder(int order) {
            this.order = order;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getElementText() {
            return elementText;
        }
        
        public void setElementText(String elementText) {
            this.elementText = elementText;
        }
        
        public StepType getStepType() {
            return stepType;
        }
        
        public void setStepType(StepType stepType) {
            this.stepType = stepType;
            if (stepType != null) {
                this.action = stepType.name().toLowerCase();
            }
        }
        
        public List<HttpRequestDto> getCorrelatedRequests() {
            return correlatedRequests;
        }
        
        public void setCorrelatedRequests(List<HttpRequestDto> correlatedRequests) {
            this.correlatedRequests = new CopyOnWriteArrayList<>(
                    correlatedRequests == null ? List.of() : correlatedRequests);
        }
        
        public void addCorrelatedRequest(HttpRequestDto request) {
            this.correlatedRequests.add(request);
        }
        
        public List<HttpResponseDto> getCorrelatedResponses() {
            return correlatedResponses;
        }
        
        public void setCorrelatedResponses(List<HttpResponseDto> correlatedResponses) {
            this.correlatedResponses = new CopyOnWriteArrayList<>(
                    correlatedResponses == null ? List.of() : correlatedResponses);
        }
        
        public void addCorrelatedResponse(HttpResponseDto response) {
            this.correlatedResponses.add(response);
        }
        
        /**
         * Generates a Playwright-like code representation of this step
         */
        public String toPlaywrightCode() {
            StepType type = stepType == null ? StepType.OTHER : stepType;
            String selectorLiteral = javaStringLiteral(selector);
            String valueLiteral = javaStringLiteral(value);
            return switch (type) {
                case NAVIGATION -> "page.navigate(" + valueLiteral + ");";
                case CLICK -> "page.click(" + selectorLiteral + ");";
                case SUBMIT -> "page.locator(" + selectorLiteral
                        + ").evaluate(\"form => form.requestSubmit()\");";
                case FILL -> "page.fill(" + selectorLiteral + ", " + valueLiteral + ");";
                case SELECT -> "page.selectOption(" + selectorLiteral + ", " + valueLiteral + ");";
                case CHECK -> "page.check(" + selectorLiteral + ");";
                case UNCHECK -> "page.uncheck(" + selectorLiteral + ");";
                case PRESS -> "page.press(" + selectorLiteral + ", " + valueLiteral + ");";
                case HOVER -> "page.hover(" + selectorLiteral + ");";
                case WAIT -> "page.waitForSelector(" + selectorLiteral + ");";
                case UPLOAD -> "page.setInputFiles(" + selectorLiteral
                        + ", java.nio.file.Paths.get(" + valueLiteral + "));";
                case SCROLL -> "page.locator(" + selectorLiteral + ").scrollIntoViewIfNeeded();";
                case ASSERTION -> "expect(page.locator(" + selectorLiteral + ")).toBeVisible();";
                default -> "// " + singleLine(action) + ": " + singleLine(description);
            };
        }

        private static String javaStringLiteral(String value) {
            String source = value == null ? "" : value;
            StringBuilder escaped = new StringBuilder(source.length() + 2).append('"');
            for (int index = 0; index < source.length(); index++) {
                char character = source.charAt(index);
                switch (character) {
                    case '\\' -> escaped.append("\\\\");
                    case '"' -> escaped.append("\\\"");
                    case '\n' -> escaped.append("\\n");
                    case '\r' -> escaped.append("\\r");
                    case '\t' -> escaped.append("\\t");
                    default -> {
                        if (Character.isISOControl(character)) {
                            escaped.append(String.format("\\u%04x", (int) character));
                        } else {
                            escaped.append(character);
                        }
                    }
                }
            }
            return escaped.append('"').toString();
        }

        private static String singleLine(String value) {
            return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
        }
        
        @Override
        public String toString() {
            return order + ". " + action + " - " + description;
        }
    }
}
