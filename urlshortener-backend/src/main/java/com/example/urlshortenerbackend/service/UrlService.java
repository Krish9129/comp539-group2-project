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
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;

@Service
public class UrlService {

    private final BigtableRepository bigtableRepository;

    // Define a constant for CST timezone (US Central Time)
    private static final ZoneId CST_ZONE = ZoneId.of("America/Chicago");

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
        // Store timestamp in CST timezone
        ZonedDateTime cstNow = ZonedDateTime.now(CST_ZONE);
        String timestamp = cstNow.toString();
        String rowKey = shortId + "_" + timestamp + "_" + UUID.randomUUID();

        // get the real IP
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // process multiple IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        String country = getCountryFromIP(ip);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer") != null ? request.getHeader("Referer") : "Direct";
        String deviceType = determineDeviceType(userAgent);
        String browser = determineBrowser(userAgent);

        bigtableRepository.saveClickEvent(rowKey, timestamp, ip, userAgent, referer, country, deviceType, browser);
    }

    /**
     * Gets analytics data for a short URL based on specified time range.
     *
     * @param shortId The ID of the shortened URL
     * @param dateStr Date in ISO format (yyyy-MM-dd) for daily analytics. If null, returns data for the current day.
     * @param timeRange The time range for analytics: "daily", "weekly", or "monthly"
     * @return Map containing analytics data organized by the specified time range
     */
    public Map<String, Object> getAnalytics(String shortId, String dateStr, String timeRange) {
        // Default to daily if timeRange is not specified
        if (timeRange == null || timeRange.isEmpty()) {
            timeRange = "daily";
        }

        switch (timeRange.toLowerCase()) {
            case "weekly":
                return getWeeklyAnalytics(shortId);
            case "monthly":
                return getMonthlyAnalytics(shortId);
            case "daily":
            default:
                return getDailyAnalytics(shortId, dateStr);
        }
    }

    /**
     * Original daily analytics method, renamed to be consistent with new structure.
     */
    private Map<String, Object> getDailyAnalytics(String shortId, String dateStr) {
        // This is your existing implementation from getAnalytics
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
        Map<String, Long> countryDistribution = new HashMap<>();
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
                // If date format is invalid, default to current date in CST
                targetDate = LocalDate.now(CST_ZONE);
            }
        } else {
            targetDate = LocalDate.now(CST_ZONE);
        }

        // Calculate start and end time for the requested date in CST
        ZonedDateTime startOfDay = targetDate.atStartOfDay(CST_ZONE);
        ZonedDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        // Get all click data for the shortId
        List<Map<String, String>> allClickData = bigtableRepository.getClickData(shortId);

        // Filter and process click data for the specified date
        for (Map<String, String> click : allClickData) {
            // Parse timestamp from CST format
            ZonedDateTime clickTime;
            try {
                // Parse the timestamp and ensure it's interpreted as CST
                String timestampStr = click.get("timestamp");

                // If timestamp ends with Z (UTC marker), we need to convert it to CST
                if (timestampStr.endsWith("Z")) {
                    // Parse as UTC instant and convert to CST
                    Instant instant = Instant.parse(timestampStr);
                    clickTime = instant.atZone(CST_ZONE);
                } else {
                    // Try to parse directly - it might already be in a zoned format
                    try {
                        clickTime = ZonedDateTime.parse(timestampStr);
                    } catch (DateTimeParseException e) {
                        // If it's not a zoned format, try parsing with offset
                        try {
                            OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestampStr);
                            clickTime = offsetDateTime.atZoneSameInstant(CST_ZONE);
                        } catch (DateTimeParseException ex) {
                            // Last resort - treat as local date time in CST
                            LocalDateTime localDateTime = LocalDateTime.parse(timestampStr,
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            clickTime = localDateTime.atZone(CST_ZONE);
                        }
                    }
                }
            } catch (Exception e) {
                // Skip this click if timestamp cannot be parsed
                continue;
            }

            // Skip if the click is not within the target date
            if (clickTime.isBefore(startOfDay) || clickTime.isAfter(endOfDay)) {
                continue;
            }

            // Process click data for the specified date
            int hour = clickTime.getHour();
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
        analyticsData.put("timezone", "CST (America/Chicago)");
        analyticsData.put("clicks_per_hour", clicksPerHour);
        analyticsData.put("device_distribution", deviceDistribution);
        analyticsData.put("browser_distribution", browserDistribution);
        analyticsData.put("country_distribution", countryDistribution);
        analyticsData.put("total_clicks", clicksPerHour.values().stream().mapToLong(Long::longValue).sum());

        return analyticsData;
    }

    /**
     * Gets weekly analytics data for a short URL for the past 12 weeks.
     *
     * @param shortId The ID of the shortened URL
     * @return Map containing analytics data organized by weeks
     */
    private Map<String, Object> getWeeklyAnalytics(String shortId) {
        // Get current date in CST
        ZonedDateTime currentDate = ZonedDateTime.now(CST_ZONE);

        // Create a map to store clicks per week
        Map<String, Long> clicksPerWeek = new LinkedHashMap<>();

        // Initialize data for past 12 weeks (including current week)
        for (int i = 11; i >= 0; i--) {
            ZonedDateTime weekStart = currentDate.minusWeeks(i).with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
            LocalDate weekDate = weekStart.toLocalDate();
            String weekLabel = weekDate.toString(); // Use the Sunday date as week label
            clicksPerWeek.put(weekLabel, 0L);
        }

        // Create distributions for aggregated data
        Map<String, Long> deviceDistribution = new HashMap<>();
        deviceDistribution.put("Desktop", 0L);
        deviceDistribution.put("Mobile", 0L);
        deviceDistribution.put("Tablet", 0L);

        Map<String, Long> browserDistribution = new HashMap<>();
        browserDistribution.put("Chrome", 0L);
        browserDistribution.put("Firefox", 0L);
        browserDistribution.put("Safari", 0L);
        browserDistribution.put("Edge", 0L);
        browserDistribution.put("Internet Explorer", 0L);
        browserDistribution.put("Other", 0L);

        Map<String, Long> countryDistribution = new HashMap<>();
        countryDistribution.put("United States", 0L);
        countryDistribution.put("China", 0L);
        countryDistribution.put("India", 0L);
        countryDistribution.put("United Kingdom", 0L);
        countryDistribution.put("Germany", 0L);
        countryDistribution.put("Unknown", 0L);

        // Calculate start of the period (12 weeks ago)
        ZonedDateTime startOfPeriod = currentDate.minusWeeks(12)
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .truncatedTo(ChronoUnit.DAYS);

        // Get all click data for the shortId
        List<Map<String, String>> allClickData = bigtableRepository.getClickData(shortId);

        // Process each click
        for (Map<String, String> click : allClickData) {
            ZonedDateTime clickTime;
            try {
                String timestampStr = click.get("timestamp");

                if (timestampStr.endsWith("Z")) {
                    Instant instant = Instant.parse(timestampStr);
                    clickTime = instant.atZone(CST_ZONE);
                } else {
                    try {
                        clickTime = ZonedDateTime.parse(timestampStr);
                    } catch (DateTimeParseException e) {
                        try {
                            OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestampStr);
                            clickTime = offsetDateTime.atZoneSameInstant(CST_ZONE);
                        } catch (DateTimeParseException ex) {
                            LocalDateTime localDateTime = LocalDateTime.parse(timestampStr,
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            clickTime = localDateTime.atZone(CST_ZONE);
                        }
                    }
                }
            } catch (Exception e) {
                continue; // Skip if timestamp can't be parsed
            }

            // Skip if the click is before the start of our 12-week period
            if (clickTime.isBefore(startOfPeriod)) {
                continue;
            }

            // Find which week this click belongs to
            ZonedDateTime weekStart = clickTime.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                    .truncatedTo(ChronoUnit.DAYS);
            String weekLabel = weekStart.toLocalDate().toString();

            // Increment the count for this week
            if (clicksPerWeek.containsKey(weekLabel)) {
                clicksPerWeek.put(weekLabel, clicksPerWeek.get(weekLabel) + 1);
            }

            // Aggregate distribution data
            String device = click.get("device_type");
            deviceDistribution.put(device, deviceDistribution.getOrDefault(device, 0L) + 1);

            String browser = click.get("browser");
            if (browserDistribution.containsKey(browser)) {
                browserDistribution.put(browser, browserDistribution.get(browser) + 1);
            } else {
                browserDistribution.put("Other", browserDistribution.get("Other") + 1);
            }

            String country = click.get("country");
            if (country == null || country.isEmpty() || country.equals("Unknown")) {
                countryDistribution.put("Unknown", countryDistribution.getOrDefault("Unknown", 0L) + 1);
            } else {
                countryDistribution.put(country, countryDistribution.getOrDefault(country, 0L) + 1);
            }
        }

        // Prepare response data
        Map<String, Object> analyticsData = new HashMap<>();
        analyticsData.put("time_range", "weekly");
        analyticsData.put("timezone", "CST (America/Chicago)");
        analyticsData.put("clicks_per_week", clicksPerWeek);
        analyticsData.put("device_distribution", deviceDistribution);
        analyticsData.put("browser_distribution", browserDistribution);
        analyticsData.put("country_distribution", countryDistribution);
        analyticsData.put("total_clicks", clicksPerWeek.values().stream().mapToLong(Long::longValue).sum());

        return analyticsData;
    }

    /**
     * Gets monthly analytics data for a short URL for the past 12 months.
     *
     * @param shortId The ID of the shortened URL
     * @return Map containing analytics data organized by months
     */
    private Map<String, Object> getMonthlyAnalytics(String shortId) {
        // Get current date in CST
        ZonedDateTime currentDate = ZonedDateTime.now(CST_ZONE);

        // Create a map to store clicks per month
        Map<String, Long> clicksPerMonth = new LinkedHashMap<>();

        // Initialize data for past 12 months (including current month)
        for (int i = 11; i >= 0; i--) {
            YearMonth yearMonth = YearMonth.from(currentDate.minusMonths(i));
            String monthLabel = yearMonth.toString(); // Format: yyyy-MM
            clicksPerMonth.put(monthLabel, 0L);
        }

        // Create distributions for aggregated data
        Map<String, Long> deviceDistribution = new HashMap<>();
        deviceDistribution.put("Desktop", 0L);
        deviceDistribution.put("Mobile", 0L);
        deviceDistribution.put("Tablet", 0L);

        Map<String, Long> browserDistribution = new HashMap<>();
        browserDistribution.put("Chrome", 0L);
        browserDistribution.put("Firefox", 0L);
        browserDistribution.put("Safari", 0L);
        browserDistribution.put("Edge", 0L);
        browserDistribution.put("Internet Explorer", 0L);
        browserDistribution.put("Other", 0L);

        Map<String, Long> countryDistribution = new HashMap<>();
        countryDistribution.put("United States", 0L);
        countryDistribution.put("China", 0L);
        countryDistribution.put("India", 0L);
        countryDistribution.put("United Kingdom", 0L);
        countryDistribution.put("Germany", 0L);
        countryDistribution.put("Unknown", 0L);

        // Calculate start of the period (12 months ago, first day of that month)
        ZonedDateTime startOfPeriod = currentDate.minusMonths(12)
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);

        // Get all click data for the shortId
        List<Map<String, String>> allClickData = bigtableRepository.getClickData(shortId);

        // Process each click
        for (Map<String, String> click : allClickData) {
            ZonedDateTime clickTime;
            try {
                String timestampStr = click.get("timestamp");

                if (timestampStr.endsWith("Z")) {
                    Instant instant = Instant.parse(timestampStr);
                    clickTime = instant.atZone(CST_ZONE);
                } else {
                    try {
                        clickTime = ZonedDateTime.parse(timestampStr);
                    } catch (DateTimeParseException e) {
                        try {
                            OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestampStr);
                            clickTime = offsetDateTime.atZoneSameInstant(CST_ZONE);
                        } catch (DateTimeParseException ex) {
                            LocalDateTime localDateTime = LocalDateTime.parse(timestampStr,
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            clickTime = localDateTime.atZone(CST_ZONE);
                        }
                    }
                }
            } catch (Exception e) {
                continue; // Skip if timestamp can't be parsed
            }

            // Skip if the click is before the start of our 12-month period
            if (clickTime.isBefore(startOfPeriod)) {
                continue;
            }

            // Find which month this click belongs to
            YearMonth yearMonth = YearMonth.from(clickTime);
            String monthLabel = yearMonth.toString();

            // Increment the count for this month
            if (clicksPerMonth.containsKey(monthLabel)) {
                clicksPerMonth.put(monthLabel, clicksPerMonth.get(monthLabel) + 1);
            }

            // Aggregate distribution data
            String device = click.get("device_type");
            deviceDistribution.put(device, deviceDistribution.getOrDefault(device, 0L) + 1);

            String browser = click.get("browser");
            if (browserDistribution.containsKey(browser)) {
                browserDistribution.put(browser, browserDistribution.get(browser) + 1);
            } else {
                browserDistribution.put("Other", browserDistribution.get("Other") + 1);
            }

            String country = click.get("country");
            if (country == null || country.isEmpty() || country.equals("Unknown")) {
                countryDistribution.put("Unknown", countryDistribution.getOrDefault("Unknown", 0L) + 1);
            } else {
                countryDistribution.put(country, countryDistribution.getOrDefault(country, 0L) + 1);
            }
        }

        // Prepare response data
        Map<String, Object> analyticsData = new HashMap<>();
        analyticsData.put("time_range", "monthly");
        analyticsData.put("timezone", "CST (America/Chicago)");
        analyticsData.put("clicks_per_month", clicksPerMonth);
        analyticsData.put("device_distribution", deviceDistribution);
        analyticsData.put("browser_distribution", browserDistribution);
        analyticsData.put("country_distribution", countryDistribution);
        analyticsData.put("total_clicks", clicksPerMonth.values().stream().mapToLong(Long::longValue).sum());

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