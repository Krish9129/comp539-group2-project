package com.example.urlshortenerbackend.config;

import com.example.urlshortenerbackend.service.LLMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

@Configuration
public class ApplicationShutdownConfig {

    @Autowired
    private LLMService llmService;

    @PreDestroy
    public void onShutdown() {
        // Shutdown the LLM service to release resources
        if (llmService != null) {
            llmService.shutdown();
        }
    }
}