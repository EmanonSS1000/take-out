package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.line.bot")
@Data
public class LineProperties {
    private String channelID;
    private String channelToken; // 對應配置中的 channel-token
    private String channelSecret; // LINE channel secret
    //private String channelAccessToken; // LINE channel access token
    //private String webhookUrl; // LINE webhook 接收 URL
    //private String lineNotifyUrl; // LINE Notify API URL
    //private String lineNotifyToken; // 用來發送 LINE Notify 訊息的 toke
}
