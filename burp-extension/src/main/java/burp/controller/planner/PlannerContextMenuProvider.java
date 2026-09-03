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

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.controller.LogController;

import javax.swing.JMenuItem;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

public final class PlannerContextMenuProvider implements ContextMenuItemsProvider {
    private final PlannerRequestCoordinator controller;
    private final LogController logger;

    public PlannerContextMenuProvider(PlannerRequestCoordinator controller, LogController logger) {
        this.controller = controller;
        this.logger = logger;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        if (event.selectedRequestResponses() == null || event.selectedRequestResponses().isEmpty()) {
            return List.of();
        }

        JMenuItem sendToPlanner = new JMenuItem("Send to Courier Planner");
        sendToPlanner.addActionListener(ignored -> {
            event.selectedRequestResponses().forEach(requestResponse -> {
                HttpRequest request = requestResponse.request();
                if (request == null) {
                    return;
                }
                long sentAt = captureTimestamp(requestResponse);
                if (requestResponse.response() == null) {
                    controller.add(request, sentAt);
                } else {
                    controller.add(request, requestResponse.response(), sentAt);
                }
            });
            logger.logInfo("Sent " + event.selectedRequestResponses().size()
                    + " request(s) to Courier Planner");
        });
        List<Component> items = new ArrayList<>();
        items.add(sendToPlanner);
        return items;
    }

    static long captureTimestamp(burp.api.montoya.http.message.HttpRequestResponse requestResponse) {
        if (requestResponse.timingData().isPresent()) {
            return requestResponse.timingData().get().timeRequestSent().toInstant().toEpochMilli();
        }
        if (requestResponse.response() != null) {
            String dateHeader = requestResponse.response().headerValue("Date");
            if (dateHeader != null && !dateHeader.isBlank()) {
                try {
                    return java.time.ZonedDateTime.parse(dateHeader,
                            java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                            .toInstant().toEpochMilli();
                } catch (Exception ignored) {
                }
            }
        }
        return 0L;
    }
}
