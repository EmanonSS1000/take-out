package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.gcs")
@Data
public class GcsProperties {
    private String endpoint;
    //private String accessKeyId;
    //private String accessKeySecret;
    private String bucketName;
}
