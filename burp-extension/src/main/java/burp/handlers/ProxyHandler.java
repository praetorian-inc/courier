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
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.InterceptedResponse;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;
import burp.api.montoya.proxy.http.ProxyResponseHandler;
import burp.api.montoya.proxy.http.ProxyResponseReceivedAction;
import burp.api.montoya.proxy.http.ProxyResponseToBeSentAction;
import burp.controller.ConfigurationController;
import burp.controller.ConnectionController;
import burp.controller.LogController;
import burp.model.HttpRequestResponsePair;
import burp.utils.BoundedExpiringMap;
import burp.utils.MonitoredHashMap;

import java.util.Map;

public class ProxyHandler implements ProxyRequestHandler, ProxyResponseHandler {
    private final CapturePolicy capturePolicy;
    private final MontoyaApi api;
    private final Map<Integer, HttpRequestResponsePair> interceptedRequests = new BoundedExpiringMap<>();
    private final Map<Integer, HttpRequestResponsePair> pendingRequests = new BoundedExpiringMap<>();
    private final Map<Integer, HttpRequestResponsePair> interceptedResponses = new BoundedExpiringMap<>();
    private volatile MonitoredHashMap<Integer, HttpRequestResponsePair> requestResponses;

    public ProxyHandler(ConfigurationController configuration, MontoyaApi api, LogController logger,
            ConnectionController connection) {
        this.api = api;
        this.capturePolicy = new CapturePolicy(configuration, api, connection);
    }

    public void setRequestResponses(MonitoredHashMap<Integer, HttpRequestResponsePair> requestResponses) {
        MonitoredHashMap<Integer, HttpRequestResponsePair> previous = this.requestResponses;
        this.requestResponses = requestResponses;
        if (requestResponses == null) {
            if (previous != null) {
                pendingRequests.forEach(previous::putIfAbsent);
            }
            interceptedRequests.clear();
            pendingRequests.clear();
            interceptedResponses.clear();
        }
    }

    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest request) {
        if (!capturePolicy.shouldCapture(request.url(), request.fileExtension())) {
            return ProxyRequestReceivedAction.continueWith(request);
        }

        long now = System.currentTimeMillis();
        HttpRequestResponsePair pair = new HttpRequestResponsePair()
                .setOriginalRequest(request, now)
                .setOriginalRequestNotes(request.annotations().notes())
                .setOriginalRequestHighlighted(request.annotations().hasHighlightColor());
        pair.wasRequestIntercepted = api.proxy().isInterceptEnabled();
        interceptedRequests.put(request.messageId(), pair);
        return ProxyRequestReceivedAction.continueWith(request);
    }

    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest request) {
        MonitoredHashMap<Integer, HttpRequestResponsePair> destination = requestResponses;
        if (destination == null || !capturePolicy.shouldCapture(request.url(), request.fileExtension())) {
            interceptedRequests.remove(request.messageId());
            return ProxyRequestToBeSentAction.continueWith(request);
        }

        long now = System.currentTimeMillis();
        HttpRequestResponsePair pair = interceptedRequests.remove(request.messageId());
        if (pair == null) {
            pair = new HttpRequestResponsePair().setOriginalRequest(request, now);
        }

        HttpRequest originalRequest = pair.getOriginalRequest();
        if (originalRequest != null && !BinaryMessageSupport.requestsEqual(originalRequest, request)) {
            pair.setModifiedRequest(request, now)
                    .setModifiedRequestNotes(request.annotations().notes())
                    .setModifiedRequestHighlighted(request.annotations().hasHighlightColor());
            pair.wasRequestModified = true;
        }

        if (BinaryMessageSupport.isBinary(request)) {
            if (pair.wasRequestModified) {
                pair.setModifiedRequest(BinaryMessageSupport.encodeBody(request), now);
                pair.wasModifiedRequestBodyBase64Encoded = true;
            }
            if (originalRequest != null) {
                pair.setOriginalRequest(BinaryMessageSupport.encodeBody(originalRequest), pair.originalRequestTime)
                        .setWasRequestBodyBase64Encoded(true);
            }
        }

        pendingRequests.put(request.messageId(), pair);
        return ProxyRequestToBeSentAction.continueWith(request);
    }

    @Override
    public ProxyResponseReceivedAction handleResponseReceived(InterceptedResponse response) {
        String extension = response.initiatingRequest().fileExtension();
        if (!capturePolicy.shouldCapture(response.initiatingRequest().url(), extension)) {
            return ProxyResponseReceivedAction.continueWith(response);
        }

        long now = System.currentTimeMillis();
        HttpRequestResponsePair pair = new HttpRequestResponsePair()
                .setOriginalResponse(response, now)
                .setOriginalResponseNotes(response.annotations().notes())
                .setOriginalResponseHighlighted(response.annotations().hasHighlightColor());
        pair.wasResponseIntercepted = api.proxy().isInterceptEnabled();
        interceptedResponses.put(response.messageId(), pair);
        return ProxyResponseReceivedAction.continueWith(response);
    }

    @Override
    public ProxyResponseToBeSentAction handleResponseToBeSent(InterceptedResponse response) {
        MonitoredHashMap<Integer, HttpRequestResponsePair> destination = requestResponses;
        String extension = response.initiatingRequest().fileExtension();
        if (destination == null || !capturePolicy.shouldCapture(response.initiatingRequest().url(), extension)) {
            pendingRequests.remove(response.messageId());
            interceptedResponses.remove(response.messageId());
            return ProxyResponseToBeSentAction.continueWith(response);
        }

        long now = System.currentTimeMillis();
        HttpRequestResponsePair pair = pendingRequests.remove(response.messageId());
        HttpRequestResponsePair receivedPair = interceptedResponses.remove(response.messageId());
        if (pair == null) {
            pair = new HttpRequestResponsePair();
        }
        if (receivedPair != null && receivedPair.getOriginalResponse() != null) {
            pair.setOriginalResponse(receivedPair.getOriginalResponse(), receivedPair.originalResponseTime);
            pair.wasResponseIntercepted = receivedPair.wasResponseIntercepted;
        } else if (pair.getOriginalResponse() == null) {
            pair.setOriginalResponse(response, now);
        }

        HttpResponse originalResponse = pair.getOriginalResponse();
        if (originalResponse != null && !BinaryMessageSupport.responsesEqual(originalResponse, response)) {
            pair.setModifiedResponse(response, now)
                    .setModifiedResponseNotes(response.annotations().notes())
                    .setModifiedResponseHighlighted(response.annotations().hasHighlightColor());
            pair.wasResponseModified = true;
        }

        if (BinaryMessageSupport.isBinary(response, extension)) {
            if (pair.wasResponseModified) {
                pair.setModifiedResponse(BinaryMessageSupport.encodeBody(response), now);
                pair.wasModifiedResponseBodyBase64Encoded = true;
            }
            if (originalResponse != null) {
                pair.setOriginalResponse(BinaryMessageSupport.encodeBody(originalResponse), pair.originalResponseTime)
                        .setWasResponseBodyBase64Encoded(true);
            }
        }

        destination.put(response.messageId(), pair);
        return ProxyResponseToBeSentAction.continueWith(response);
    }
}
