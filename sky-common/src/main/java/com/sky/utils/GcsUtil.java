package com.sky.utils;

import com.google.cloud.storage.*;
import com.sky.properties.GcsProperties;  // 引入 GcsProperties 類
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
@Component
public class GcsUtil {

    private final String endpoint;
    private final String bucketName;

    // 如果需要通過 GcsProperties 自動注入配置，您可以在構造函數中傳入 GcsProperties
    @Autowired
    public GcsUtil(GcsProperties gcsProperties) {
        this.endpoint = gcsProperties.getEndpoint();
        this.bucketName = gcsProperties.getBucketName();
    }

    /**
     * 文件上傳
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {
        // 使用 GCS 客戶端
        Storage storage = StorageOptions.newBuilder().setProjectId(bucketName).build().getService();

        try {
            BlobId blobId = BlobId.of(bucketName, objectName);

            // 創建 Blob 信息（GCS 的對象）
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            // 將文件內容上傳到 GCS
            Blob blob = storage.create(blobInfo, new ByteArrayInputStream(bytes));

            // 文件訪問路徑規則：gs://bucket-name/object-name
            String fileUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);

            log.info("文件已上傳到: {}", fileUrl);

            return fileUrl;
        } catch (Exception e) {
            // 打印錯誤詳細信息
            log.error("文件上傳到 GCS 時發生錯誤，存儲桶: {}, 文件名: {}", bucketName, objectName, e);
            e.printStackTrace();
            return null;
        }
    }
}

