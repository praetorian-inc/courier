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

import burp.api.montoya.http.message.requests.HttpRequest;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class PlannerRequestTableModel extends AbstractTableModel {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final String[] COLUMN_NAMES = {
            "", "Time", "Method", "URL", "Source", "Status", "Headers", "Body"
    };

    private final List<HttpRequestResponsePair> requests = new ArrayList<>();

    @Override
    public int getRowCount() {
        return requests.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return COLUMN_NAMES[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 6, 7 -> Integer.class;
            default -> String.class;
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= requests.size()) {
            return null;
        }
        HttpRequestResponsePair pair = requests.get(rowIndex);
        HttpRequest request = pair.getOriginalRequest();
        return switch (columnIndex) {
            case 0 -> "";
            case 1 -> formatTime(pair.originalRequestTime);
            case 2 -> request == null ? "Unknown" : request.method();
            case 3 -> request == null ? "Unknown" : request.url();
            case 4 -> pair.getToolSource().isEmpty() ? "Unknown" : pair.getToolSource();
            case 5 -> pair.getOriginalResponse() == null
                    ? "—" : Short.toString(pair.getOriginalResponse().statusCode());
            case 6 -> request == null ? 0 : request.headers().size();
            case 7 -> request == null ? 0 : request.body().length();
            default -> null;
        };
    }

    private static String formatTime(long timestamp) {
        return timestamp <= 0 ? "—" : TIME_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void addRequest(HttpRequestResponsePair request) {
        requests.add(request);
        int rowIndex = requests.size() - 1;
        fireTableRowsInserted(rowIndex, rowIndex);
    }

    public void removeRequest(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < requests.size()) {
            requests.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    public void clearRequests() {
        int size = requests.size();
        if (size > 0) {
            requests.clear();
            fireTableRowsDeleted(0, size - 1);
        }
    }

    public HttpRequestResponsePair getRequestAt(int rowIndex) {
        return rowIndex >= 0 && rowIndex < requests.size() ? requests.get(rowIndex) : null;
    }

    public List<HttpRequestResponsePair> getAllRequests() {
        return List.copyOf(requests);
    }
}
