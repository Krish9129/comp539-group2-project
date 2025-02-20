package com.example.urlshortenerbackend.controller;

import com.example.urlshortenerbackend.model.UrlEntity;
import com.example.urlshortenerbackend.service.UrlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<Map<String, String>> shortenUrl(
            @RequestParam String url,
            @RequestParam(required = false) String alias) {
        try {
            String shortId = urlService.createShortUrl(url, alias);
            Map<String, String> response = new HashMap<>();
            response.put("shortId", shortId);
            response.put("shortUrl", "localhost:8080/api/" + shortId);
            return ResponseEntity.ok(response);
        } catch(IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Alias already in use. Please choose a different one.");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create short URL");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/bulk-shorten")
    public ResponseEntity<Map<String, Object>> bulkShortenUrls(@RequestBody List<String> urls) {
        try {
            Map<String, String> shortenedUrls = urlService.bulkShorten(urls);
            return ResponseEntity.ok(Map.of("shortened_urls", shortenedUrls));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create short URL(s)"));
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getLongUrl(@PathVariable String id) {
        Optional<String> longUrl = urlService.getLongUrl(id);
        if (longUrl.isPresent()) {
            // use HTTP 302 to redirect
            return ResponseEntity.status(302)
                    .header("Location", longUrl.get())
                    .build();
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "URL not found");
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUrl(@PathVariable String id) {
        try {
            urlService.deleteShortUrl(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "URL successfully deleted");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete URL");
            return ResponseEntity.badRequest().body(error);
        }
    }
}