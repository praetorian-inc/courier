/**
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

// Enhanced tracking script similar to Playwright's codegen
class CodeGenTracker {
    constructor() {
        this.trackingId = '{UNIQUETRACKINGID}';
        this.setupTracking();
    }

    setupTracking() {
        // Click tracking
        document.addEventListener('click', (e) => {
            if (this.shouldIgnoreElement(e.target)) return;

            const selector = this.generateSelector(e.target);
            const action = {
                type: 'click',
                selector: selector,
                text: e.target.textContent?.trim().substring(0, 30),
                timestamp: Date.now()
            };

            console.log(this.trackingId + ':', JSON.stringify(action));
        }, true);

        // Input/Fill tracking
        document.addEventListener('input', (e) => {
            const selector = this.generateSelector(e.target);
            const action = {
                type: 'fill',
                selector: selector,
                value: e.target.isContentEditable ? (e.target.textContent ?? '') : e.target.value,
                timestamp: Date.now()
            };

            console.log(this.trackingId + ':', JSON.stringify(action));
        });

        // Form submission tracking
        document.addEventListener('submit', (e) => {
            const selector = this.generateSelector(e.target);
            const action = {
                type: 'submit',
                selector: selector,
                timestamp: Date.now()
            };

            console.log(this.trackingId + ':', JSON.stringify(action));
        });

        // Select dropdown changes
        document.addEventListener('change', (e) => {
            if (e.target.tagName === 'SELECT') {
                const selector = this.generateSelector(e.target);
                const action = {
                    type: 'selectOption',
                    selector: selector,
                    value: e.target.value,
                    text: e.target.selectedOptions[0]?.text,
                    timestamp: Date.now()
                };

                console.log(this.trackingId + ':', JSON.stringify(action));
            }
        });

        // Checkbox/Radio tracking
        document.addEventListener('change', (e) => {
            if (e.target.type === 'checkbox' || e.target.type === 'radio') {
                const selector = this.generateSelector(e.target);
                const action = {
                    type: e.target.checked ? 'check' : 'uncheck',
                    selector: selector,
                    timestamp: Date.now()
                };

                console.log(this.trackingId + ':', JSON.stringify(action));
            }
        });

        // Key press tracking (for special keys)
        document.addEventListener('keydown', (e) => {
            if (['Enter', 'Tab', 'Escape'].includes(e.key)) {
                const selector = this.generateSelector(e.target);
                const action = {
                    type: 'press',
                    selector: selector,
                    value: e.key,
                    timestamp: Date.now()
                };

                console.log(this.trackingId + ':', JSON.stringify(action));
            }
        });
    }

    shouldIgnoreElement(element) {
        // Ignore elements that shouldn't generate test code
        if (!element || element === document.body || element === document.documentElement) {
            return true;
        }

        // Ignore invisible elements
        const style = window.getComputedStyle(element);
        if (style.display === 'none' || style.visibility === 'hidden') {
            return true;
        }

        return false;
    }

    generateSelector(element) {
        // Priority order: data-testid > id > unique attributes > CSS selector

        // Check for test id
        if (element.hasAttribute('data-testid')) {
            return '[data-testid=\'' + this.escapeAttribute(element.getAttribute('data-testid')) + '\']';
        }

        // Check for id
        if (element.id) {
            return '#' + CSS.escape(element.id);
        }

        // Check for name attribute
        if (element.name) {
            return '[name=\'' + this.escapeAttribute(element.name) + '\']';
        }

        // Check for unique class or combination
        if (element.className) {
            const classes = String(element.className).split(' ').filter(c => c.trim());
            if (classes.length > 0) {
                // Try first class
                const classSelector = '.' + CSS.escape(classes[0]);
                if (document.querySelectorAll(classSelector).length === 1) {
                    return classSelector;
                }
            }
        }

        // Check for placeholder
        if (element.placeholder) {
            return '[placeholder=\'' + this.escapeAttribute(element.placeholder) + '\']';
        }

        // Check for text content (for buttons, links)
        if (element.textContent && element.textContent.trim()) {
            const text = element.textContent.trim();
            if (element.tagName === 'BUTTON' || element.tagName === 'A') {
                return 'text=' + text;
            }
        }

        // Fall back to CSS path
        return this.getCssPath(element);
    }

    escapeAttribute(value) {
        return String(value).replaceAll('\\', '\\\\').replaceAll("'", "\\'");
    }

    getCssPath(element) {
        const path = [];
        while (element && element.nodeType === Node.ELEMENT_NODE) {
            let selector = element.nodeName.toLowerCase();

            if (element.id) {
                selector = '#' + CSS.escape(element.id);
                path.unshift(selector);
                break;
            }

            if (element.className) {
                const classes = String(element.className).split(' ').filter(c => c.trim());
                if (classes.length > 0) {
                    selector += '.' + CSS.escape(classes[0]);
                }
            }

            // Add nth-child if needed for uniqueness
            const siblings = Array.from(element.parentNode?.children || []);
            if (siblings.length > 1) {
                const index = siblings.indexOf(element) + 1;
                selector += ':nth-child(' + index + ')';
            }

            path.unshift(selector);
            element = element.parentElement;
        }

        return path.join(' > ');
    }
}

// Initialize tracker when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => new CodeGenTracker());
} else {
    new CodeGenTracker();
}