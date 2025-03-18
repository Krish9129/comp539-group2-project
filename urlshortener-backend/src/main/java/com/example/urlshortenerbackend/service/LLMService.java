package com.example.urlshortenerbackend.service;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LLMService {

    private final ArkService arkService;
    private final String modelId;

    // System prompt for content summarization with keywords
    private static final String SUMMARY_SYSTEM_PROMPT =
            "You are a helpful assistant that analyzes webpage content. Your task is to:\n" +
                    "1. Identify 2-3 keywords or key phrases that best represent the main topics of the webpage\n" +
                    "2. Create a concise summary (2-3 sentences) that captures the main purpose and key information\n\n" +
                    "Format your response exactly as follows:\n" +
                    "KEYWORDS: keyword1, keyword2, keyword3\n" +
                    "SUMMARY: Your 2-3 sentence summary of the webpage content.";

    @Autowired
    public LLMService(ArkService arkService, @Qualifier("modelId") String modelId) {
        this.arkService = arkService;
        this.modelId = modelId;
    }

    /**
     * Generate keywords and a concise summary of web content
     * @param webContent The webpage content to analyze
     * @return Map containing keywords and summary
     */
    public Map<String, String> generateContentAnalysis(String webContent) {
        List<ChatMessage> messages = new ArrayList<>();

        // System message with instructions for analysis
        messages.add(ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM)
                .content(SUMMARY_SYSTEM_PROMPT)
                .build());

        // User message with the content to analyze
        messages.add(ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content("Analyze this webpage content: \n\n" + webContent)
                .build());

        // Build the request
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(modelId)
                .messages(messages)
                .maxTokens(250)  // Limit response length
                .build();

        try {
            // Get the response from the LLM
            var completion = arkService.createChatCompletion(request);

            if (completion != null &&
                    completion.getChoices() != null &&
                    !completion.getChoices().isEmpty() &&
                    completion.getChoices().get(0) != null &&
                    completion.getChoices().get(0).getMessage() != null) {

                Object content = completion.getChoices().get(0).getMessage().getContent();
                if (content != null) {
                    return parseAnalysisResponse(content.toString());
                }
            }

            // Fallback if we can't get a proper response
            Map<String, String> fallbackResult = new HashMap<>();
            fallbackResult.put("keywords", "");
            fallbackResult.put("summary", "Unable to generate summary: No response from model.");
            return fallbackResult;

        } catch (Exception e) {
            // Log error and return a generic message
            System.err.println("Error generating summary: " + e.getMessage());
            Map<String, String> errorResult = new HashMap<>();
            errorResult.put("keywords", "");
            errorResult.put("summary", "Unable to generate content summary.");
            return errorResult;
        }
    }

    /**
     * Parse the LLM response to extract keywords and summary
     * @param response The raw response from the LLM
     * @return Map containing keywords and summary
     */
    private Map<String, String> parseAnalysisResponse(String response) {
        Map<String, String> result = new HashMap<>();

        // Default values in case parsing fails
        result.put("keywords", "");
        result.put("summary", "");

        // Extract keywords using regex
        Pattern keywordsPattern = Pattern.compile("KEYWORDS:(.+?)(?=SUMMARY:|$)", Pattern.DOTALL);
        Matcher keywordsMatcher = keywordsPattern.matcher(response);
        if (keywordsMatcher.find()) {
            String keywords = keywordsMatcher.group(1).trim();
            result.put("keywords", keywords);
        }

        // Extract summary using regex
        Pattern summaryPattern = Pattern.compile("SUMMARY:(.+)$", Pattern.DOTALL);
        Matcher summaryMatcher = summaryPattern.matcher(response);
        if (summaryMatcher.find()) {
            String summary = summaryMatcher.group(1).trim();
            result.put("summary", summary);
        } else if (!result.get("keywords").isEmpty()) {
            // If we have keywords but no SUMMARY tag, assume the rest is the summary
            String[] parts = response.split("KEYWORDS:", 2);
            if (parts.length > 1) {
                String afterKeywords = parts[1].trim();
                String[] summaryParts = afterKeywords.split(result.get("keywords"), 2);
                if (summaryParts.length > 1) {
                    result.put("summary", summaryParts[1].trim());
                }
            }
        }

        return result;
    }

    /**
     * Shutdown method to properly release ArkService resources when application terminates
     */
    public void shutdown() {
        if (arkService != null) {
            try {
                arkService.shutdownExecutor();
                System.out.println("LLM Service shut down successfully");
            } catch (Exception e) {
                System.err.println("Error shutting down LLM Service: " + e.getMessage());
            }
        }
    }
}