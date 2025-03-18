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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    // getUrlById
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

    /**
     * Gets analytics data for a short URL for a specific date.
     *
     * @param shortId The ID of the shortened URL
     * @param dateStr Date in ISO format (yyyy-MM-dd). If null, returns data for the current day.
     * @return Map containing analytics data organized by categories with default values for common types
     */
    public Map<String, Object> getAnalytics(String shortId, String dateStr) {
        // Initialize counters for analytics with default values of 0
        Map<Integer, Long> clicksPerHour = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            clicksPerHour.put(i, 0L);
        }

        // Initialize device distribution with common device types
        Map<String, Long> deviceDistribution = new HashMap<>();
        deviceDistribution.put("Desktop", 0L);
        deviceDistribution.put("Mobile", 0L);
        deviceDistribution.put("Tablet", 0L);

        // Initialize browser distribution with common browsers
        Map<String, Long> browserDistribution = new HashMap<>();
        browserDistribution.put("Chrome", 0L);
        browserDistribution.put("Firefox", 0L);
        browserDistribution.put("Safari", 0L);
        browserDistribution.put("Edge", 0L);
        browserDistribution.put("Internet Explorer", 0L);
        browserDistribution.put("Other", 0L);

        // Initialize country distribution with most common countries
        // We'll add actual data to this map, but won't pre-populate with too many countries
        Map<String, Long> countryDistribution = new HashMap<>();

        // Add a few major countries that are commonly seen in analytics
        countryDistribution.put("United States", 0L);
        countryDistribution.put("China", 0L);
        countryDistribution.put("India", 0L);
        countryDistribution.put("United Kingdom", 0L);
        countryDistribution.put("Germany", 0L);
        countryDistribution.put("Unknown", 0L);

        // Parse the date parameter or use current date if not provided
        LocalDate targetDate;
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                targetDate = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                // If date format is invalid, default to current date
                targetDate = LocalDate.now();
            }
        } else {
            targetDate = LocalDate.now();
        }

        // Get user's local timezone
        ZoneId localZone = ZoneId.systemDefault();

        // Calculate start and end time for the requested date in user's timezone
        ZonedDateTime startOfDay = targetDate.atStartOfDay(localZone);
        ZonedDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        // Convert to Instant for comparison with stored timestamps
        Instant startInstant = startOfDay.toInstant();
        Instant endInstant = endOfDay.toInstant();

        // Get all click data for the shortId
        List<Map<String, String>> allClickData = bigtableRepository.getClickData(shortId);

        // Filter and process click data for the specified date
        for (Map<String, String> click : allClickData) {
            Instant clickTime = Instant.parse(click.get("timestamp"));

            // Skip if the click is not within the target date
            if (clickTime.isBefore(startInstant) || clickTime.isAfter(endInstant)) {
                continue;
            }

            // Process click data for the specified date
            ZonedDateTime localTime = clickTime.atZone(localZone);
            int hour = localTime.getHour();
            clicksPerHour.put(hour, clicksPerHour.get(hour) + 1);

            // Process device type
            String device = click.get("device_type");
            deviceDistribution.put(device, deviceDistribution.getOrDefault(device, 0L) + 1);

            // Process browser with special handling for "Other" category
            String browser = click.get("browser");
            if (browserDistribution.containsKey(browser)) {
                browserDistribution.put(browser, browserDistribution.get(browser) + 1);
            } else {
                // Increment "Other" for any browser not in our predefined list
                browserDistribution.put("Other", browserDistribution.get("Other") + 1);
            }

            // Process country with special handling for unknown countries
            String country = click.get("country");
            if (country == null || country.isEmpty() || country.equals("Unknown")) {
                countryDistribution.put("Unknown", countryDistribution.getOrDefault("Unknown", 0L) + 1);
            } else {
                // Add country data to the map, even if it wasn't in our initial list
                countryDistribution.put(country, countryDistribution.getOrDefault(country, 0L) + 1);
            }
        }

        // Prepare response data
        Map<String, Object> analyticsData = new HashMap<>();
        analyticsData.put("date", targetDate.toString());
        analyticsData.put("clicks_per_hour", clicksPerHour);
        analyticsData.put("device_distribution", deviceDistribution);
        analyticsData.put("browser_distribution", browserDistribution);
        analyticsData.put("country_distribution", countryDistribution);
        analyticsData.put("total_clicks", clicksPerHour.values().stream().mapToLong(Long::longValue).sum());

        return analyticsData;
    }

    private String determineDeviceType(String userAgent) {
        if (userAgent.toLowerCase().contains("mobile")) {
            return "Mobile";
        } else if (userAgent.toLowerCase().contains("tablet")) {
            return "Tablet";
        }
        return "Desktop";
    }

    /**
     * Determines the browser more accurately from user agent string.
     * Checks for browser identifiers in a specific order to handle modern browsers.
     *
     * @param userAgent The user agent string from the HTTP request
     * @return The identified browser name
     */
    private String determineBrowser(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }

        // Convert to lowercase for case-insensitive matching
        String ua = userAgent.toLowerCase();

        // Check for browsers in order of specificity (most specific first)
        if (ua.contains("edg/") || ua.contains("edge/")) {
            return "Edge";
        }else if (ua.contains("firefox/")) {
            return "Firefox";
        } else if (ua.contains("safari/") && ua.contains("chrome/") && !ua.contains("chromium/")) {
            return "Chrome";
        } else if (ua.contains("safari/") && !ua.contains("chrome/")) {
            return "Safari";
        } else if (ua.contains("trident/") || ua.contains("msie ")) {
            return "Internet Explorer";
        }

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
}