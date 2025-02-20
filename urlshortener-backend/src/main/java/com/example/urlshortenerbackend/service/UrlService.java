package com.example.urlshortenerbackend.service;

import com.example.urlshortenerbackend.model.UrlEntity;
import com.example.urlshortenerbackend.repository.BigtableRepository;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

@Service
public class UrlService {

    private final BigtableRepository bigtableRepository;

    public UrlService(BigtableRepository bigtableRepository) {
        this.bigtableRepository = bigtableRepository;
    }

    public String createShortUrl(String originalUrl, String alias, String tag) {
        String id;

        // If alias is provided, check if it's unique
        if (alias != null && !alias.isEmpty()) {
            if (bigtableRepository.shortIdExists(alias)) {
                throw new IllegalArgumentException("Alias already in use. Please choose a different one.");
            }
            id = alias;
        } else {
            // Generate unique short ID
            do {
                id = generateShortId(originalUrl);
            } while (bigtableRepository.shortIdExists(id)); // Keep generating until unique
        }

        long createdAt = Instant.now().getEpochSecond();
        String finalTag = (tag != null && !tag.isEmpty()) ? tag : "None";

        UrlEntity urlEntity = new UrlEntity();
        urlEntity.setId(id);
        urlEntity.setOriginalUrl(originalUrl);
        urlEntity.setCreatedAt(createdAt);
        urlEntity.setClickCount(0);
        urlEntity.setLastAccess(Instant.now().toString());
        urlEntity.setTag(finalTag);

        // Save to Bigtable
        bigtableRepository.saveUrl(urlEntity);
        return id;
    }

    public Map<String, String> bulkShorten(List<String> urls) {
        Map<String, String> shortenedUrls = new HashMap<>();

        for (String url : urls) {
            try {
                String shortId = createShortUrl(url, null, null); // No custom alias
                shortenedUrls.put(url, shortId);
            } catch (Exception e) {
                shortenedUrls.put(url, "Error generating short URL");
            }
        }
        return shortenedUrls;
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

    public byte[] generateQrCode(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

    public List<UrlEntity> getUrlsByTag(String tag) {
        return bigtableRepository.getUrlsByTag(tag);
    }
}