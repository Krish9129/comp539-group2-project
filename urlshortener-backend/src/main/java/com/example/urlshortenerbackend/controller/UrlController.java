package com.example.urlshortenerbackend.controller;

import com.example.urlshortenerbackend.model.UrlEntity;
import com.example.urlshortenerbackend.service.UrlService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

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
            @RequestParam(required = false) String alias,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false, defaultValue = "false") boolean isPrivate,
            Authentication authentication) {

        try {
            String ownerId = null;

            // Get owner ID if authenticated
            if (authentication != null) {
                ownerId = getOwnerId(authentication);
            } else if (isPrivate) {
                // If URL is private, authentication is required
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Authentication required for private URLs"));
            }

            String shortId = urlService.createShortUrl(url, alias, tag, ownerId, isPrivate);
            Map<String, String> response = new HashMap<>();
            response.put("shortId", shortId);
            response.put("shortUrl", "localhost:8080/api/" + shortId);
            response.put("isPrivate", String.valueOf(isPrivate));

            if (ownerId != null) {
                response.put("ownerId", ownerId);
            }

            return ResponseEntity.ok(response);
        } catch(IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create short URL: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/bulk-shorten")
    public ResponseEntity<Map<String, Object>> bulkShortenUrls(
            @RequestBody List<Map<String, String>> urls,
            Authentication authentication) {
        try {
            String ownerId = null;
            if (authentication != null) {
                ownerId = getOwnerId(authentication);
            }

            Map<String, String> shortenedUrls = urlService.bulkShorten(urls, ownerId);
            return ResponseEntity.ok(Map.of("shortened_urls", shortenedUrls));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create short URL(s): " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLongUrl(@PathVariable String id, Authentication authentication) {
        Optional<UrlEntity> urlEntityOpt = urlService.getUrlById(id);

        // Check if URL exists
        if (urlEntityOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "URL not found");
            return ResponseEntity.notFound().build();
        }

        UrlEntity urlEntity = urlEntityOpt.get();

        // Check if URL is private and validate ownership
        if (urlEntity.isPrivate()) {
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required for private URLs"));
            }

            String ownerId = getOwnerId(authentication);
            if (!ownerId.equals(urlEntity.getOwnerId())) {
                return ResponseEntity.status(403).body(Map.of("error", "You don't have permission to access this URL"));
            }
        }

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

    @GetMapping("/{shortId}/qr")
    public ResponseEntity<?> getQrCode(@PathVariable String shortId, Authentication authentication) {
        try {
            // Check if the URL exists and if it's private
            Optional<UrlEntity> urlEntityOpt = urlService.getUrlById(shortId);
            if (urlEntityOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            UrlEntity urlEntity = urlEntityOpt.get();

            // Handle private URLs
            if (urlEntity.isPrivate()) {
                if (authentication == null) {
                    return ResponseEntity.status(401).body(Map.of("error", "Authentication required for private URLs"));
                }

                String ownerId = getOwnerId(authentication);
                if (!ownerId.equals(urlEntity.getOwnerId())) {
                    return ResponseEntity.status(403).body(Map.of("error", "You don't have permission to access this URL"));
                }
            }

            String shortUrl = "http://localhost:8080/api/" + shortId; // Base URL + short ID
            byte[] qrCode = urlService.generateQrCode(shortUrl, 300, 300);

            // Return as an image
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG) // Ensure proper content type
                    .body(qrCode);

        } catch (Exception e) {
            // Return error as JSON
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "Failed to generate QR Code"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUrl(@PathVariable String id, Authentication authentication) {
        try {
            // If user is not authenticated, deny access
            if (authentication == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }

            String ownerId = getOwnerId(authentication);
            urlService.deleteShortUrl(id, ownerId); // 传递两个参数

            Map<String, String> response = new HashMap<>();
            response.put("message", "URL successfully deleted");
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(403).body(error);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete URL: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/urls")
    public ResponseEntity<?> getUrlsByTag(@RequestParam String tag, Authentication authentication) {
        // If user is authenticated, show their private URLs for the tag
        if (authentication != null) {
            String ownerId = getOwnerId(authentication);
            List<UrlEntity> urls = urlService.getUrlsByTagAndOwnerId(tag, ownerId);
            return ResponseEntity.ok(urls);
        } else {
            // Only show public URLs for unauthenticated users
            List<UrlEntity> urls = urlService.getUrlsByTag(tag);
            if (urls.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "No URLs found for this tag"));
            }
            return ResponseEntity.ok(urls);
        }
    }

    // 添加缺失的 getOwnerId 方法
    private String getOwnerId(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            String providerId = oauth2User.getAttribute("sub");
            return provider + "#" + providerId;
        }
        return null;
    }
}