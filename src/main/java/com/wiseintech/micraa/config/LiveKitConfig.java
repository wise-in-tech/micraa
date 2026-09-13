package com.wiseintech.micraa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "livekit")
@Data
public class LiveKitConfig {
    private String url;
    private Api api;
    
    @Data
    public static class Api {
        private String key;
        private String secret;
    }
}
