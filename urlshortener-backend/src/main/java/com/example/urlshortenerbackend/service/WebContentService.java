package com.example.urlshortenerbackend.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class WebContentService {

    public String fetchWebContent(String url) throws IOException {
        try {
            // Connect to the URL with a reasonable timeout and user-agent
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; URLShortenerBot/1.0)")
                    .timeout(5000)
                    .get();

            // Extract the main content
            String title = doc.title();
            String bodyText = doc.body().text();

            // Limit content length to avoid overwhelming the LLM
            int maxLength = 1000;
            String limitedText = bodyText.length() > maxLength
                    ? bodyText.substring(0, maxLength)
                    : bodyText;

            return "Title: " + title + "\n\nContent: " + limitedText;
        } catch (IOException e) {
            throw new IOException("Failed to fetch content from URL: " + url, e);
        }
    }
}