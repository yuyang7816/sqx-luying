package com.sqx.common.file.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 本地文件存储时，映射 /upload/** 到运行目录下的 upload 目录。
 */
@Configuration
public class LocalUploadResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = "/upload";
        String location = "file:" + System.getProperty("user.dir").replace("\\", "/") + uploadPath + "/";
        registry.addResourceHandler(uploadPath + "/**").addResourceLocations(location);
    }
}
