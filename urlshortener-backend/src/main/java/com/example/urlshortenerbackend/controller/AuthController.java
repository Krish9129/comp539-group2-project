package com.example.urlshortenerbackend.controller;

import com.example.urlshortenerbackend.model.UserEntity;
import com.example.urlshortenerbackend.model.UrlEntity;
import com.example.urlshortenerbackend.service.UserService;
import com.example.urlshortenerbackend.service.UrlService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private UrlService urlService;

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String provider = getProvider(authentication);
        String providerId = oauth2User.getAttribute("sub");

        // Try to find user with provider and providerId
        Optional<UserEntity> userOptional = userService.getUserByProviderAndId(provider, providerId);

        if (userOptional.isPresent()) {
            return ResponseEntity.ok(userOptional.get());
        } else {
            // If user not found but authenticated, create a new user record
            UserEntity newUser = new UserEntity();
            newUser.setProvider(provider);
            newUser.setProviderId(providerId);
            newUser.setEmail(oauth2User.getAttribute("email"));
            newUser.setName(oauth2User.getAttribute("name"));
            newUser.setPictureUrl(oauth2User.getAttribute("picture")); // add pictureURL
            newUser.setRole("USER");
            newUser.setLastLogin(java.time.Instant.now().toString());

            // Store the new user
            userService.saveUser(newUser);

            return ResponseEntity.ok(newUser);
        }
    }

    @GetMapping("/urls")
    public ResponseEntity<?> getUserUrls(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String ownerId = getOwnerId(authentication);
        List<UrlEntity> urls = urlService.getUrlsByOwnerId(ownerId);

        return ResponseEntity.ok(urls);
    }

    @GetMapping("/urls/tag/{tag}")
    public ResponseEntity<?> getUserUrlsByTag(
            @PathVariable String tag,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String ownerId = getOwnerId(authentication);
        List<UrlEntity> urls = urlService.getUrlsByTagAndOwnerId(tag, ownerId);

        return ResponseEntity.ok(urls);
    }

    // Helper methods
    private String getProvider(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            return ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        }
        return null;
    }

    private String getOwnerId(Authentication authentication) {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String provider = getProvider(authentication);
        String email = oauth2User.getAttribute("email");
        return provider + "#" + email;
    }
}