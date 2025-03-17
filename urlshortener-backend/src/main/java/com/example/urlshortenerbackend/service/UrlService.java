package com.example.urlshortenerbackend.service;

import com.example.urlshortenerbackend.model.UrlEntity;
import com.example.urlshortenerbackend.repository.BigtableRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class UrlService {

    private final BigtableRepository bigtableRepository;

    public UrlService(BigtableRepository bigtableRepository) {
        this.bigtableRepository = bigtableRepository;
    }

    public String createShortUrl(String originalUrl, String alias, String tag) {
        return createShortUrl(originalUrl, alias, tag, null, false);
    }

    public String createShortUrl(String originalUrl, String alias, String tag, String ownerId, boolean isPrivate) {
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
        urlEntity.setOwnerId(ownerId);
        urlEntity.setPrivate(isPrivate);

        // Save to Bigtable
        bigtableRepository.saveUrl(urlEntity);
        return id;
    }

    public Map<String, String> bulkShorten(List<Map<String, String>> urls, String ownerId) {
        Map<String, String> shortenedUrls = new HashMap<>();

        for (Map<String, String> url : urls) {
            try {
                String originalUrl = url.get("url");
                String tag = url.getOrDefault("tag", "None");
                boolean isPrivate = Boolean.parseBoolean(url.getOrDefault("isPrivate", "false"));
                String shortId = createShortUrl(originalUrl, null, tag, ownerId, isPrivate);
                shortenedUrls.put(originalUrl, shortId);
            } catch (Exception e) {
                shortenedUrls.put(url.get("url"), "Error: " + e.getMessage());
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

    // 添加缺失的 getUrlById 方法
    public Optional<UrlEntity> getUrlById(String id) {
        return bigtableRepository.getUrlById(id);
    }

    public void deleteShortUrl(String id, String ownerId) {
        Optional<UrlEntity> urlEntity = bigtableRepository.getUrlById(id);
        if (urlEntity.isPresent()) {
            UrlEntity entity = urlEntity.get();

            // Check if user owns this URL
            if (entity.getOwnerId() != null && !entity.getOwnerId().equals(ownerId)) {
                throw new SecurityException("You do not have permission to delete this URL");
            }

            bigtableRepository.deleteUrl(id);
        } else {
            throw new IllegalArgumentException("URL not found");
        }
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

    public List<UrlEntity> getUrlsByTagAndOwnerId(String tag, String ownerId) {
        return bigtableRepository.getUrlsByTagAndOwnerId(tag, ownerId);
    }

    public List<UrlEntity> getUrlsByOwnerId(String ownerId) {
        return bigtableRepository.getUrlsByOwnerId(ownerId);
    }

    public Map<Integer, Long> getClicksPerHour(String shortId) {
        Map<Integer, Long> hourlyClicks = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            hourlyClicks.put(i, 0L);
        }

        List<String> timestamps = bigtableRepository.getClickTimestamps(shortId);

        for (String timestampStr : timestamps) {
            Instant clickTime = Instant.parse(timestampStr);
            int hour = clickTime.atZone(ZoneOffset.UTC).getHour();
            hourlyClicks.put(hour, hourlyClicks.get(hour) + 1);
        }
        return hourlyClicks;
    }

    public void logClickEvent(String shortId, HttpServletRequest request) {
        String timestamp = Instant.now().toString();
        String rowKey = shortId + "_" + timestamp + "_" + UUID.randomUUID();

        String ip = request.getRemoteAddr();
        String country = getCountryFromIP(ip);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer") != null ? request.getHeader("Referer") : "Direct";
        String deviceType = determineDeviceType(userAgent);
        String browser = determineBrowser(userAgent);

        bigtableRepository.saveClickEvent(rowKey, timestamp, ip, userAgent, referer, country, deviceType, browser);
    }

    private String determineDeviceType(String userAgent) {
        if (userAgent.toLowerCase().contains("mobile")) {
            return "Mobile";
        } else if (userAgent.toLowerCase().contains("tablet")) {
            return "Tablet";
        }
        return "Desktop";
    }

    private String determineBrowser(String userAgent) {
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        return "Other";
    }

    public String getCountryFromIP(String ip) {
        try {
            String apiUrl = "http://ip-api.com/json/" + ip;
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(apiUrl, String.class);

            JSONObject json = new JSONObject(response);
            return json.optString("country", "Unknown"); // Default to "Unknown" if no country found
        } catch (Exception e) {
            return "Unknown"; // Handle errors gracefully
        }
    }

    public Map<String, Object> getAnalytics(String shortId) {
        Map<Integer, Long> clicksPerHour = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            clicksPerHour.put(i, 0L);
        }

        Map<String, Long> deviceDistribution = new HashMap<>();
        Map<String, Long> browserDistribution = new HashMap<>();
        Map<String, Long> countryDistribution = new HashMap<>();

        List<Map<String, String>> clickData = bigtableRepository.getClickData(shortId);

        ZoneId localZone = ZoneId.systemDefault(); // Change to user’s preferred timezone if needed
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

        for (Map<String, String> click : clickData) {
            Instant clickTime = Instant.parse(click.get("timestamp"));
            ZonedDateTime localTime = clickTime.atZone(localZone);
            int hour = localTime.getHour(); // Get local hour
            clicksPerHour.put(hour, clicksPerHour.get(hour) + 1);

            String device = click.get("device_type");
            deviceDistribution.put(device, deviceDistribution.getOrDefault(device, 0L) + 1);

            String browser = click.get("browser");
            browserDistribution.put(browser, browserDistribution.getOrDefault(browser, 0L) + 1);

            String country = click.get("country");
            countryDistribution.put(country, countryDistribution.getOrDefault(country, 0L) + 1);
        }

        Map<String, Object> analyticsData = new HashMap<>();
        analyticsData.put("clicks_per_hour", clicksPerHour);
        analyticsData.put("device_distribution", deviceDistribution);
        analyticsData.put("browser_distribution", browserDistribution);
        analyticsData.put("country_distribution", countryDistribution);

        return analyticsData;
    }
}