package com.sky.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件存储
 * <p>
 * <b>为什么需要它：</b>项目原来的图片上传只有一条路——传到阿里云 OSS，
 * 而配置文件里是 your-access-key-id 这样的占位符。
 * 结果是「新增服务项目 → 上传封面图」这一步必然失败，
 * 而上传接口的旧实现还会在失败的情况下返回一个假的地址，
 * 前端以为传成功了，图片实际是 404。
 * <p>
 * 这里提供一个零依赖的兜底：没配 OSS 就把文件存到本机磁盘，
 * 通过 Spring MVC 的静态资源映射对外提供访问
 * （映射在 WebMvcConfiguration 里，路径 /uploads/**）。
 * <p>
 * 目录默认放在 D 盘，避免占满系统盘；对应配置项 sky.local-upload.dir。
 */
@Component
@Slf4j
public class LocalFileStore {

    @Value("${sky.local-upload.dir:D:/temp/life-service-uploads}")
    private String dir;

    @Value("${sky.local-upload.url-prefix:http://localhost:8081/uploads/}")
    private String urlPrefix;

    /**
     * 保存文件，返回可访问的 URL
     */
    public String save(byte[] bytes, String objectName) {
        try {
            Path root = Paths.get(dir).toAbsolutePath().normalize();
            Files.createDirectories(root);

            Path target = root.resolve(objectName).normalize();
            // 防止 objectName 里带 ../ 之类的相对路径跳出上传目录。
            // 文件名由服务端生成（UUID + 后缀），正常不会有问题，
            // 但多校验一次的成本极低，泄露磁盘文件的风险不值得冒
            if (!target.startsWith(root)) {
                throw new IllegalArgumentException("非法的文件名：" + objectName);
            }

            Files.write(target, bytes);
            String url = urlPrefix + objectName;
            log.info("文件已保存到本地：{} -> {}", target, url);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("保存文件到本地失败：" + e.getMessage(), e);
        }
    }

    /**
     * 上传根目录的绝对路径，供静态资源映射使用
     */
    public String getAbsoluteDir() {
        return Paths.get(dir).toAbsolutePath().normalize().toString();
    }
}
