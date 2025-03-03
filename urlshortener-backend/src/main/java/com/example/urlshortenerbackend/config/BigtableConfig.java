package com.example.urlshortenerbackend.config;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class BigtableConfig {

    @Value("${spring.cloud.gcp.bigtable.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.bigtable.instance-id}")
    private String instanceId;

    @Bean
    public BigtableDataClient bigtableDataClient() throws IOException {
        // use ClassPathResource to load recourse file
        Resource resource = new ClassPathResource("team2-service-account-key.json");

        GoogleCredentials credentials = GoogleCredentials.fromStream(
                resource.getInputStream()
        );

        BigtableDataSettings settings = BigtableDataSettings.newBuilder()
                .setProjectId(projectId)
                .setInstanceId(instanceId)
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        return BigtableDataClient.create(settings);
    }
}