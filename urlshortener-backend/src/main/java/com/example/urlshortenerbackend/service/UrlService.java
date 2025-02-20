package com.example.urlshortenerbackend.service;

import com.example.urlshortenerbackend.model.UrlEntity;
import com.example.urlshortenerbackend.repository.BigtableRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class UrlService {

    private final BigtableRepository bigtableRepository;

    public UrlService(BigtableRepository bigtableRepository) {
        this.bigtableRepository = bigtableRepository;
    }

//    public String createShortUrl(String originalUrl) {
//        String id = generateShortId(originalUrl);
//        long createdAt = Instant.now().getEpochSecond();
//
//        UrlEntity urlEntity = new UrlEntity();
//        urlEntity.setId(id);
//        urlEntity.setOriginalUrl(originalUrl);
//        urlEntity.setCreatedAt(createdAt);
//        urlEntity.setClickCount(0);
//        urlEntity.setLastAccess(Instant.now().toString());
//
//        bigtableRepository.saveUrl(urlEntity);
//        return id;
//    }

    public String createShortUrl(String originalUrl, String alias) {
        String id = (alias != null && !alias.isEmpty()) ? alias : generateShortId(originalUrl);

        // Check if the alias already exists
        if (bigtableRepository.aliasExists(id)) {
            throw new IllegalArgumentException("Alias already in use. Please choose a different one.");
        }

        long createdAt = Instant.now().getEpochSecond();

        UrlEntity urlEntity = new UrlEntity();
        urlEntity.setId(id);
        urlEntity.setOriginalUrl(originalUrl);
        urlEntity.setCreatedAt(createdAt);
        urlEntity.setClickCount(0);
        urlEntity.setLastAccess(Instant.now().toString());

        // Save to Bigtable
        bigtableRepository.saveUrl(urlEntity);
        return id;
    }


    public Optional<String> getLongUrl(String id) {
        Optional<UrlEntity> urlEntity = bigtableRepository.getUrlById(id);
        if (urlEntity.isPresent()) {
            bigtableRepository.incrementClickCount(id);
            return Optional.of(urlEntity.get().getOriginalUrl());
        }
        return Optional.empty();
    }

    public void deleteShortUrl(String id) {
        bigtableRepository.deleteUrl(id);
    }

    private String generateShortId(String url) {
        return Integer.toHexString(url.hashCode());
    }
}