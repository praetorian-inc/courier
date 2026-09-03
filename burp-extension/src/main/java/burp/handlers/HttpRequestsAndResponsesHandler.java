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
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.controller.ConfigurationController;
import burp.controller.ConnectionController;
import burp.model.HttpRequestResponsePair;
import burp.utils.BoundedExpiringMap;
import burp.utils.MonitoredHashMap;

import java.util.Map;

public class HttpRequestsAndResponsesHandler implements HttpHandler {
    private final CapturePolicy capturePolicy;
    private final Map<Integer, HttpRequestResponsePair> pendingRequests = new BoundedExpiringMap<>();
    private volatile MonitoredHashMap<Integer, HttpRequestResponsePair> requestResponses;

    public HttpRequestsAndResponsesHandler(ConfigurationController configuration, MontoyaApi api,
            ConnectionController connection) {
        capturePolicy = new CapturePolicy(configuration, api, connection);
    }

    public void setRequestResponses(MonitoredHashMap<Integer, HttpRequestResponsePair> requestResponses) {
        MonitoredHashMap<Integer, HttpRequestResponsePair> previous = this.requestResponses;
        this.requestResponses = requestResponses;
        if (requestResponses == null) {
            if (previous != null) {
                pendingRequests.forEach(previous::putIfAbsent);
            }
            pendingRequests.clear();
        }
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent request) {
        MonitoredHashMap<Integer, HttpRequestResponsePair> currentResponses = requestResponses;
        if (currentResponses == null
                || request.toolSource().isFromTool(ToolType.PROXY)
                || !capturePolicy.shouldCapture(request.url(), request.fileExtension())) {
            return RequestToBeSentAction.continueWith(request);
        }

        long now = System.currentTimeMillis();
        HttpRequestResponsePair pair = new HttpRequestResponsePair()
                .setOriginalRequest(request, now)
                .setOriginalRequestNotes(request.annotations().notes())
                .setOriginalRequestHighlighted(request.annotations().hasHighlightColor())
                .setToolSource(request.toolSource().toolType().toolName());

        if (BinaryMessageSupport.isBinary(request)) {
            HttpRequest encodedRequest = BinaryMessageSupport.encodeBody(request);
            pair.setOriginalRequest(encodedRequest, now).setWasRequestBodyBase64Encoded(true);
        }
        pendingRequests.put(request.messageId(), pair);
        return RequestToBeSentAction.continueWith(request);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived response) {
        MonitoredHashMap<Integer, HttpRequestResponsePair> currentResponses = requestResponses;
        String extension = response.initiatingRequest().fileExtension();
        if (currentResponses == null
                || response.toolSource().isFromTool(ToolType.PROXY)
                || !capturePolicy.shouldCapture(response.initiatingRequest().url(), extension)) {
            return ResponseReceivedAction.continueWith(response);
        }

        long now = System.currentTimeMillis();
        HttpRequestResponsePair pair = pendingRequests.remove(response.messageId());
        if (pair == null) {
            pair = new HttpRequestResponsePair()
                    .setOriginalRequest(response.initiatingRequest(), now);
        }
        pair.setOriginalResponse(response, now)
                .setOriginalResponseNotes(response.annotations().notes())
                .setOriginalResponseHighlighted(response.annotations().hasHighlightColor())
                .setToolSource(response.toolSource().toolType().toolName());

        if (BinaryMessageSupport.isBinary(response, extension)) {
            HttpResponse encodedResponse = BinaryMessageSupport.encodeBody(response);
            pair.setOriginalResponse(encodedResponse, now).setWasResponseBodyBase64Encoded(true);
        }
        currentResponses.put(response.messageId(), pair);
        return ResponseReceivedAction.continueWith(response);
    }
}
