package com.sky.config;

import com.sky.properties.GcsProperties;
import com.sky.utils.GcsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置類，用於創建Gcs對象
 */
@Configuration
@Slf4j
public class GcsConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GcsUtil gusUtil(GcsProperties gcsProperties){
        log.info("開始創建google雲文件上傳工具類對象: {}",gcsProperties);
        return new GcsUtil(gcsProperties.getEndpoint(),
                gcsProperties.getBucketName());
    }
}
