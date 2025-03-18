package com.example.urlshortenerbackend.config;

import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class LLMConfig {

    @Value("${llm.api-key}")
    private String apiKey;

    @Value("${llm.model-id}")
    private String modelId;

    @Value("${llm.base-url:}")
    private String baseUrl;

    @Bean
    public ArkService arkService() {
        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();

        // Create the ArkService using the available builder methods
        ArkService.Builder builder = ArkService.builder()
                .apiKey(apiKey)
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .timeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .retryTimes(2);

        // Check if baseUrl method exists, otherwise try alternative methods
        if (baseUrl != null && !baseUrl.isEmpty()) {
            try {
                // Try setting base URL
                builder.baseUrl(baseUrl);
            } catch (NoSuchMethodError e) {
                // If baseUrl method doesn't exist, log the issue
                System.err.println("Warning: baseUrl method not available in SDK version. Using default URL.");
            }
        }

        return builder.build();
    }

    @Bean
    public String modelId() {
        return modelId;
    }
}