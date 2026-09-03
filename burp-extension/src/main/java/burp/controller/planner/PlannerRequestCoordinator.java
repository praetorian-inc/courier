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
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.controller.LogController;
import burp.model.HttpRequestResponsePair;
import burp.model.PlannerRequestTableModel;
import burp.utils.SafeLogFormatter;
import burp.view.PlannerView;

import javax.swing.SwingUtilities;
import java.util.List;

public final class PlannerRequestCoordinator {
    private final PlannerView view;
    private final LogController logger;
    private final PlannerPreviewPresenter preview;

    public PlannerRequestCoordinator(PlannerView view, LogController logger,
            PlannerPreviewPresenter preview) {
        this.view = view;
        this.logger = logger;
        this.preview = preview;
    }

    public void add(HttpRequestResponsePair request) {
        SwingUtilities.invokeLater(() -> {
            PlannerRequestTableModel tableModel = view.getTableModel();
            if (tableModel != null) {
                tableModel.addRequest(request);
            }
        });
        HttpRequest original = request.getOriginalRequest();
        logger.logInfo("Added planner request from "
                + (original == null ? "unknown-origin" : SafeLogFormatter.origin(original.url())));
    }

    public void add(HttpRequest request, long timestamp) {
        add(new HttpRequestResponsePair().setOriginalRequest(request, timestamp));
    }

    public void add(HttpRequest request, HttpResponse response, long timestamp) {
        add(new HttpRequestResponsePair()
                .setOriginalRequest(request, timestamp)
                .setOriginalResponse(response, timestamp));
    }

    public void select(HttpRequestResponsePair request) {
        if (request == null) {
            preview.clear();
            return;
        }
        SwingUtilities.invokeLater(() -> {
            preview.select(request);
            HttpRequest original = request.getOriginalRequest();
            logger.logInfo("Planner request previewed from "
                    + (original == null ? "unknown-origin" : SafeLogFormatter.origin(original.url())));
        });
    }

    public void deselect() {
        view.clearRequestSelection();
        preview.clear();
        logger.logInfo("Planner requests deselected");
    }

    public void clearSelection() {
        view.clearRequestSelection();
        preview.clear();
    }

    public void remove(int index) {
        PlannerRequestTableModel tableModel = view.getTableModel();
        if (tableModel == null) {
            return;
        }
        HttpRequestResponsePair removed = tableModel.getRequestAt(index);
        if (removed == null) {
            return;
        }
        boolean wasSelected = preview.isSelected(removed);
        tableModel.removeRequest(index);
        if (wasSelected) {
            preview.clear();
        }
        HttpRequest request = removed.getOriginalRequest();
        logger.logDebug("Removed planner request from "
                + (request == null ? "unknown-origin" : SafeLogFormatter.origin(request.url())));
    }

    public void clear() {
        preview.clear();
        PlannerRequestTableModel tableModel = view.getTableModel();
        if (tableModel != null) {
            tableModel.clearRequests();
        }
        logger.logDebug("Planner requests cleared");
    }

    public List<HttpRequestResponsePair> getRequests() {
        PlannerRequestTableModel tableModel = view.getTableModel();
        return tableModel == null ? List.of() : tableModel.getAllRequests();
    }
}
