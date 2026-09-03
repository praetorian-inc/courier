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

package burp.serialization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO class representing the JSON structure for collaborator interactions
 */
public class InteractionDto {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("host")
    private String host;
    
    @JsonProperty("port")
    private int port;
    
    @JsonProperty("protocol")
    private String protocol;
    
    @JsonProperty("query")
    private String query;
    
    @JsonProperty("clientIp")
    private String clientIp;
    
    @JsonProperty("timeStamp")
    private String timeStamp;
    
    @JsonProperty("customData")
    private String customData;
    
    // Default constructor
    public InteractionDto() {}
    
    // Constructor with all fields
    public InteractionDto(String id, String type, String host, int port, String protocol, 
                         String query, String clientIp, String timeStamp, String customData) {
        this.id = id;
        this.type = type;
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.query = query;
        this.clientIp = clientIp;
        this.timeStamp = timeStamp;
        this.customData = customData;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
    
    public String getTimeStamp() {
        return timeStamp;
    }
    
    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
    
    public String getCustomData() {
        return customData;
    }
    
    public void setCustomData(String customData) {
        this.customData = customData;
    }
}
