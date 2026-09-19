package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import com.sky.utils.LocalFileStore;
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
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;
    @Autowired
    private LocalFileStore localFileStore;

    /**
     * 文件上传
     * <p>
     * 配了阿里云 OSS 就传到 OSS；没配（配置文件里还是占位符）
     * 就退回到本地磁盘存储，保证开发环境也能把上传跑通。
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传：{}",file);

        try {
            //原始文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                log.warn("上传的文件没有扩展名：{}", originalFilename);
                return Result.error(MessageConstant.UPLOAD_FAILED);
            }
            //截取原始文件名的后缀   dfdfdf.png
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            //构造新文件名称
            String objectName = UUID.randomUUID().toString() + extension;

            //文件的请求路径
            byte[] bytes = file.getBytes();
            String filePath;
            if (aliOssUtil.isConfigured()) {
                filePath = aliOssUtil.upload(bytes, objectName);
            } else {
                log.warn("未配置阿里云 OSS（access-key-id 仍是占位符），本次上传改用本地存储");
                filePath = localFileStore.save(bytes, objectName);
            }
            return Result.success(filePath);
        } catch (Exception e) {
            // 原来只捕获 IOException，而 OSS 失败抛的是 RuntimeException，
            // 会直接冒到全局异常处理器变成 500。这里统一兜住，
            // 返回业务上更好处理的「文件上传失败」
            log.error("文件上传失败：{}", e.getMessage(), e);
        }

        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
