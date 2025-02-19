package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
//import jdk.internal.event.Event;

import javax.xml.transform.Source;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LineWebhookDTO {
    private String destination;
    private List<Event> events;

    public String getDestination(){
        return destination;
    }
    public void setDestination(String destination){
        this.destination = destination;
    }
    public List<Event> getEvents() {
        return events;
    }
    public void setEvents(List<Event> events) {
        this.events = events;
    }

    // 內部類別 - 事件
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {
        private String type;
        private String replyToken;
        private Message message;
        private Source source;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getReplyToken() { return replyToken; }
        public void setReplyToken(String replyToken) { this.replyToken = replyToken; }

        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }

        public Source getSource() { return source; }
        public void setSource(Source source) { this.source = source; }
    }

    // 內部類別 - 訊息
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String type;
        private String id;
        private String text;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    // 內部類別 - 訊息來源
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Source {
        private String userId;
        private String type;

        @JsonProperty("userId") // 確保 JSON 解析時對應 `userId`
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }


}
