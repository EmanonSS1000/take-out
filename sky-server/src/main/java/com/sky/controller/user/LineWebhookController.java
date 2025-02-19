package com.sky.controller.user;

import com.sky.dto.LineWebhookDTO;
import com.sky.properties.LineProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
@Slf4j
public class LineWebhookController {

    @Autowired
    private LineProperties lineProperties;

    @PostMapping
    public ResponseEntity<String> receiveWebhook(@RequestBody LineWebhookDTO request){
        try{
            log.info("收到 Line Webhook: {}", request);
            // 解析 JSON，取得 userId
            List<LineWebhookDTO.Event> events = request.getEvents();
            if (events != null && !events.isEmpty()) {
                LineWebhookDTO.Event event = events.get(0);
                String replyToken = event.getReplyToken();
                LineWebhookDTO.Message message = event.getMessage();
                LineWebhookDTO.Source source = event.getSource();
                if (replyToken != null && message != null) {
                    String userMessage = message.getText();
                    String userId = source.getUserId();

                    log.info("收到 UserID: {}", userId);
                    log.info("收到使用者訊息: {}", userMessage);
                }
            }
            return ResponseEntity.ok("OK");
        } catch (Exception e){
            log.info("Webhook 錯誤: {}", e);
            // 解析 JSON，取得 userId
            return ResponseEntity.status(500).body("Internal Server Error");
        }

    }

}
