package com.example.paper.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!"local".equalsIgnoreCase(appProperties.getStorage().getType())) {
            return; // 当前仅实现 local 存储模拟
        }

        File localDir = new File(appProperties.getStorage().getLocalDir());
        String abs = localDir.getAbsolutePath();
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + abs + "/")
                .setCachePeriod(3600);
    }
}

