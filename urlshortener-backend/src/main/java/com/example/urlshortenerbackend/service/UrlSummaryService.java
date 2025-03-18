package com.example.urlshortenerbackend.service;

import com.example.urlshortenerbackend.model.UrlEntity;
import com.example.urlshortenerbackend.repository.BigtableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UrlSummaryService {

    private final BigtableRepository bigtableRepository;
    private final WebContentService webContentService;
    private final LLMService llmService;

    @Autowired
    public UrlSummaryService(BigtableRepository bigtableRepository,
                             WebContentService webContentService,
                             LLMService llmService) {
        this.bigtableRepository = bigtableRepository;
        this.webContentService = webContentService;
        this.llmService = llmService;
    }

    /**
     * Get a summary and keywords for a URL, either from cache or by generating new ones
     */
    public Map<String, String> getUrlSummary(String shortId) {
        Map<String, String> result = new HashMap<>();

        // Check if we already have a summary cached
        Optional<Map<String, String>> cachedAnalysis = bigtableRepository.getAnalysisForUrl(shortId);
        if (cachedAnalysis.isPresent() && !cachedAnalysis.get().isEmpty()) {
            return cachedAnalysis.get();
        }

        // No cached analysis, need to generate one
        Optional<UrlEntity> urlEntityOpt = bigtableRepository.getUrlById(shortId);
        if (urlEntityOpt.isEmpty()) {
            result.put("error", "URL not found");
            return result;
        }

        UrlEntity urlEntity = urlEntityOpt.get();
        String originalUrl = urlEntity.getOriginalUrl();

        try {
            // Fetch web content
            String webContent = webContentService.fetchWebContent(originalUrl);

            // Generate analysis using LLM
            Map<String, String> analysis = llmService.generateContentAnalysis(webContent);

            // Save the analysis to cache
            bigtableRepository.saveAnalysisForUrl(shortId, analysis);

            return analysis;

        } catch (IOException e) {
            result.put("error", "Failed to fetch webpage: " + e.getMessage());
            return result;
        } catch (Exception e) {
            result.put("error", "Failed to generate analysis: " + e.getMessage());
            return result;
        }
    }
}