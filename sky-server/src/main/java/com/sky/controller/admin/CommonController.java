package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.GcsUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用街口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    @Autowired
    private GcsUtil gcsUtil;

    /**
     * 文件上傳
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上傳")
    public Result<String> upload(MultipartFile file){
        log.info("文件上傳: {}",file);

        try {
            //原始文件名
            String originalFilename = file.getOriginalFilename();
            //擷取原始文件名的后綴
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            //構造新文件名稱
            String objectName = UUID.randomUUID().toString() + extension;

            //文件請求路徑
            String filePath = gcsUtil.upload(file.getBytes(), objectName);
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上傳失敗: {}", e);
        }


        return Result.error(MessageConstant.UPLOAD_FAILED);
    }

}
