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

package burp.controller.planner;

import burp.api.montoya.http.message.HttpMessage;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.controller.LogController;
import burp.model.HttpRequestResponsePair;
import burp.view.PlannerView;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.nio.charset.StandardCharsets;

public final class PlannerPreviewPresenter {
    private final PlannerView view;
    private final LogController logger;
    private String viewMode = "Pretty";
    private HttpRequestResponsePair selectedRequest;

    public PlannerPreviewPresenter(PlannerView view, LogController logger) {
        this.view = view;
        this.logger = logger;
    }

    public HttpRequestResponsePair getSelectedRequest() {
        return selectedRequest;
    }

    public boolean isSelected(HttpRequestResponsePair request) {
        return selectedRequest != null && selectedRequest.equals(request);
    }

    public void select(HttpRequestResponsePair request) {
        selectedRequest = request;
        if (request == null) {
            view.hidePreviewPanel();
            return;
        }
        view.showPreviewPanel();
        renderCurrentMode();
    }

    public void clear() {
        selectedRequest = null;
        view.hidePreviewPanel();
    }

    public void setViewMode(String mode) {
        viewMode = mode;
        renderCurrentMode();
    }

    public String getViewMode() {
        return viewMode;
    }

    public void renderCurrentMode() {
        view.clearPreviewContent();
        view.selectViewMode(viewMode);
        switch (viewMode) {
            case "Raw" -> formatRawContent();
            case "Hex" -> formatHexContent();
            default -> formatPrettyContent();
        }
        view.resetPreviewCaret();
    }

    public String[] formatRawContent() {
        String[] content = rawPreviewContent();
        setPlainPreview(view.getRequestPreviewArea(), content[0],
                "No request data available", "defaultRaw");
        setPlainPreview(view.getResponsePreviewArea(), content[1],
                "No response data available", "defaultRaw");
        return content;
    }

    public String[] formatPrettyContent() {
        HttpRequest request = selectedRequest == null ? null : selectedRequest.getOriginalRequest();
        HttpResponse response = selectedRequest == null ? null : selectedRequest.getOriginalResponse();
        renderPretty(request, view.getRequestPreviewArea(), "No request data available");
        renderPretty(response, view.getResponsePreviewArea(), "No response data available");
        return rawPreviewContent();
    }

    public String[] formatHexContent() {
        String[] rawContent = rawPreviewContent();
        String requestHex = HttpPreviewFormatter.hex(rawContent[0].getBytes(StandardCharsets.UTF_8));
        String responseHex = HttpPreviewFormatter.hex(rawContent[1].getBytes(StandardCharsets.UTF_8));
        setPlainPreview(view.getRequestPreviewArea(), requestHex,
                "No request data available", "defaultHex");
        setPlainPreview(view.getResponsePreviewArea(), responseHex,
                "No response data available", "defaultHex");
        return new String[] {requestHex, responseHex};
    }

    private String[] rawPreviewContent() {
        if (selectedRequest == null) {
            return new String[] {"", ""};
        }
        return new String[] {
                HttpPreviewFormatter.raw(selectedRequest.getOriginalRequest()),
                HttpPreviewFormatter.raw(selectedRequest.getOriginalResponse())
        };
    }

    private void renderPretty(HttpMessage message, JTextPane textPane, String emptyMessage) {
        if (message == null) {
            textPane.setText(emptyMessage);
            return;
        }
        textPane.setText("");
        StyledDocument document = textPane.getStyledDocument();
        Style normal = textPane.addStyle("default", null);
        StyleConstants.setFontFamily(normal, "Monospaced");
        StyleConstants.setFontSize(normal, 13);
        Style accent = textPane.addStyle("blue", normal);
        StyleConstants.setForeground(accent, new Color(191, 211, 229));
        try {
            String startLine = message instanceof HttpRequest request
                    ? request.method() + " " + request.path() + " " + request.httpVersion()
                    : ((HttpResponse) message).httpVersion() + " "
                            + ((HttpResponse) message).statusCode() + " "
                            + ((HttpResponse) message).reasonPhrase();
            document.insertString(document.getLength(), softWrap(startLine) + "\n", accent);
            for (var header : message.headers()) {
                document.insertString(document.getLength(), header.name() + ": ", accent);
                document.insertString(document.getLength(), softWrap(header.value()) + "\n", normal);
            }
            String body = message.bodyToString();
            if (body != null && !body.isEmpty()) {
                document.insertString(document.getLength(), "\n" + softWrap(body), normal);
            }
        } catch (BadLocationException exception) {
            logger.logError("Error formatting pretty content: " + exception.getMessage());
        }
    }

    private static String softWrap(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder wrapped = new StringBuilder(value.length() + value.length() / 50);
        int runLength = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isWhitespace(character)) {
                runLength = 0;
            } else if (++runLength > 50) {
                wrapped.append('\u200B');
                runLength = 1;
            }
            wrapped.append(character);
        }
        return wrapped.toString();
    }

    private static void setPlainPreview(JTextPane textPane, String content,
            String emptyMessage, String styleName) {
        textPane.setDocument(textPane.getEditorKit().createDefaultDocument());
        textPane.setText(content.isEmpty() ? emptyMessage : content);
        StyledDocument document = textPane.getStyledDocument();
        Style style = textPane.addStyle(styleName, null);
        StyleConstants.setFontFamily(style, "Monospaced");
        StyleConstants.setFontSize(style, 13);
        document.setCharacterAttributes(0, document.getLength(), style, true);
    }
}
