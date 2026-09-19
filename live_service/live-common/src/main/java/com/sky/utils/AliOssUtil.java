package com.sky.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 是否已经配置了真实的 OSS 参数
     * <p>
     * 项目里的 application-dev.yml 默认是占位符（your-access-key-id 之类），
     * 没有阿里云账号的情况下直接调用 upload() 一定会失败，
     * 调用方先问一句，就能改走本地存储，不必非要买 OSS 才能跑通上传功能。
     */
    public boolean isConfigured() {
        return isRealValue(endpoint) && isRealValue(accessKeyId)
                && isRealValue(accessKeySecret) && isRealValue(bucketName);
    }

    private boolean isRealValue(String value) {
        return value != null && !value.trim().isEmpty() && !value.startsWith("your-");
    }

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            log.error("上传到 OSS 被拒绝：code={}, message={}, requestId={}",
                    oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId());
            // 这里必须抛出去。原来的实现只是打印异常、然后照样返回一个
            // 「看起来正常」的文件地址，接口返回成功但图片根本不存在，
            // 前端拿到的是一个永远 404 的链接
            throw new RuntimeException("上传到 OSS 失败：" + oe.getErrorMessage(), oe);
        } catch (ClientException ce) {
            log.error("连接 OSS 失败：{}", ce.getMessage());
            throw new RuntimeException("连接 OSS 失败：" + ce.getMessage(), ce);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);

        log.info("文件上传到:{}", stringBuilder.toString());

        return stringBuilder.toString();
    }
}
