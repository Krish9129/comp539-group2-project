package com.example.urlshortenerbackend.repository;

import com.example.urlshortenerbackend.model.UrlEntity;
import com.example.urlshortenerbackend.model.UserEntity;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.Query;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.cloud.bigtable.data.v2.models.Filters;
import com.google.api.gax.rpc.ServerStream;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

@Repository
public class BigtableRepository {

    private static final String TABLE_NAME = "team2_url_shortener";

    // Column Family: short_urls
    private static final String CF_SHORT_URLS = "short_urls";
    private static final String COL_ORIGINAL_URL = "original_url";
    private static final String COL_OWNER_ID = "owner_id";

    // Column Family: metadata
    private static final String CF_METADATA = "metadata";
    private static final String COL_CLICK_COUNT = "click_count";
    private static final String COL_LAST_ACCESS = "last_access";
    private static final String COL_TAG = "tag";
    private static final String COL_IS_PRIVATE = "is_private";

    // Column Family: user_data (for user records)
    private static final String CF_USER_DATA = "user_data";
    private static final String COL_EMAIL = "email";
    private static final String COL_NAME = "name";
    private static final String COL_PROVIDER = "provider";
    private static final String COL_PROVIDER_ID = "provider_id";
    private static final String COL_PICTURE = "picture";
    private static final String COL_ROLE = "role";
    private static final String COL_LAST_LOGIN = "last_login";

    private final BigtableDataClient bigtableClient;

    public BigtableRepository(BigtableDataClient bigtableClient) {
        this.bigtableClient = bigtableClient;
    }

    // User related methods
    public void saveUser(UserEntity userEntity) {
        // Ensure the ID is constructed correctly
        if (userEntity.getId() == null || userEntity.getId().isEmpty()) {
            // Construct ID if not already set
            userEntity.setId("user#" + userEntity.getProvider() + "#" + userEntity.getProviderId());
        }

        try {
            // Create a mutation to save all user fields
            RowMutation rowMutation = RowMutation.create(TABLE_NAME, userEntity.getId());

            // Add all non-null fields to the mutation
            if (userEntity.getEmail() != null) {
                rowMutation.setCell("user_data", "email", userEntity.getEmail());
            }

            if (userEntity.getName() != null) {
                rowMutation.setCell("user_data", "name", userEntity.getName());
            }

            if (userEntity.getProvider() != null) {
                rowMutation.setCell("user_data", "provider", userEntity.getProvider());
            }

            if (userEntity.getProviderId() != null) {
                rowMutation.setCell("user_data", "provider_id", userEntity.getProviderId());
            }

            if (userEntity.getPictureUrl() != null) {
                rowMutation.setCell("user_data", "picture", userEntity.getPictureUrl());
            }

            if (userEntity.getRole() != null) {
                rowMutation.setCell("user_data", "role", userEntity.getRole());
            } else {
                rowMutation.setCell("user_data", "role", "USER"); // Default role
            }

            if (userEntity.getLastLogin() != null) {
                rowMutation.setCell("user_data", "last_login", userEntity.getLastLogin());
            } else {
                rowMutation.setCell("user_data", "last_login", java.time.Instant.now().toString());
            }

            // Execute the mutation
            bigtableClient.mutateRow(rowMutation);

            System.out.println("User saved successfully: " + userEntity.getId());
        } catch (Exception e) {
            System.err.println("Error saving user: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save user", e);
        }
    }

    // URL related methods with owner support
    public void saveUrl(UrlEntity urlEntity) {
        RowMutation rowMutation = RowMutation.create(TABLE_NAME, urlEntity.getId())
                .setCell(CF_SHORT_URLS, COL_ORIGINAL_URL, urlEntity.getOriginalUrl())
                .setCell(CF_SHORT_URLS, COL_TAG, urlEntity.getTag())
                .setCell(CF_METADATA, COL_CLICK_COUNT, String.valueOf(0))
                .setCell(CF_METADATA, COL_LAST_ACCESS,
                        Instant.ofEpochSecond(urlEntity.getCreatedAt()).toString());

        if (urlEntity.getOwnerId() != null) {
            rowMutation.setCell(CF_SHORT_URLS, COL_OWNER_ID, urlEntity.getOwnerId());
        }

        rowMutation.setCell(CF_METADATA, COL_IS_PRIVATE, String.valueOf(urlEntity.isPrivate()));

        bigtableClient.mutateRow(rowMutation);
    }

    public Optional<UrlEntity> getUrlById(String id) {
        Row row = bigtableClient.readRow(TABLE_NAME, id);
        if (row == null) return Optional.empty();

        UrlEntity entity = buildUrlEntityFromRow(row);
        return Optional.of(entity);
    }

    public List<UrlEntity> getUrlsByOwnerId(String ownerId) {
        List<UrlEntity> userUrls = new ArrayList<>();
        ServerStream<Row> rows = bigtableClient.readRows(Query.create(TABLE_NAME));

        for (Row row : rows) {
            String rowKey = row.getKey().toStringUtf8();
            // Skip user records
            if (rowKey.startsWith("user#")) {
                continue;
            }

            // Skip URLs without owner or with different owner
            if (row.getCells(CF_SHORT_URLS, COL_OWNER_ID).isEmpty()) {
                continue;
            }

            String urlOwnerId = row.getCells(CF_SHORT_URLS, COL_OWNER_ID).get(0).getValue().toStringUtf8();
            if (urlOwnerId.equals(ownerId)) {
                userUrls.add(buildUrlEntityFromRow(row));
            }
        }

        return userUrls;
    }

    public void incrementClickCount(String id) {
        Row row = bigtableClient.readRow(TABLE_NAME, id);
        if (row != null) {
            long currentCount = Long.parseLong(row.getCells(CF_METADATA, COL_CLICK_COUNT)
                    .get(0).getValue().toStringUtf8());

            RowMutation rowMutation = RowMutation.create(TABLE_NAME, id)
                    .setCell(CF_METADATA, COL_CLICK_COUNT, String.valueOf(currentCount + 1))
                    .setCell(CF_METADATA, COL_LAST_ACCESS, Instant.now().toString());

            bigtableClient.mutateRow(rowMutation);
        }
    }

    public void saveClickEvent(String rowKey, String timestamp, String ip, String userAgent, String referer, String country, String deviceType, String browser) {
        RowMutation rowMutation = RowMutation.create(TABLE_NAME, rowKey)
                .setCell("click_events", "timestamp", timestamp)
                .setCell("click_events", "ip_address", ip)
                .setCell("click_events", "user_agent", userAgent)
                .setCell("click_events", "referer", referer)
                .setCell("click_events", "country", country)
                .setCell("click_events", "device_type", deviceType)
                .setCell("click_events", "browser", browser);

        bigtableClient.mutateRow(rowMutation);
    }

    public List<String> getClickTimestamps(String shortId) {
        List<String> timestamps = new ArrayList<>();

        // Create a query with filters
        Query query = Query.create(TABLE_NAME)
                .filter(
                        Filters.FILTERS.chain()
                                .filter(Filters.FILTERS.family().exactMatch("click_events"))
                                .filter(Filters.FILTERS.qualifier().exactMatch("timestamp"))
                );

        ServerStream<Row> rows = bigtableClient.readRows(query);

        for (Row row : rows) {
            String rowKey = row.getKey().toStringUtf8();
            if (rowKey.startsWith(shortId)) {
                String timestamp = row.getCells("click_events", "timestamp").get(0).getValue().toStringUtf8();
                timestamps.add(timestamp);
            }
        }

        return timestamps;
    }

    public List<Map<String, String>> getClickData(String shortId) {
        List<Map<String, String>> clickEvents = new ArrayList<>();

        Query query = Query.create(TABLE_NAME)
                .filter(
                      Filters.FILTERS.family().exactMatch("click_events")
                );

        ServerStream<Row> rows = bigtableClient.readRows(query);

        for (Row row : rows) {
            String rowKey = row.getKey().toStringUtf8();
            if (!rowKey.startsWith(shortId)) continue;

            Map<String, String> clickData = new HashMap<>();
            clickData.put("timestamp", row.getCells("click_events", "timestamp").get(0).getValue().toStringUtf8());
            clickData.put("device_type", row.getCells("click_events", "device_type").get(0).getValue().toStringUtf8());
            clickData.put("browser", row.getCells("click_events", "browser").get(0).getValue().toStringUtf8());
            clickData.put("country", row.getCells("click_events", "country").get(0).getValue().toStringUtf8());

            clickEvents.add(clickData);
        }

        return clickEvents;
    }

    public List<UrlEntity> getUrlsByTag(String tag) {
        List<UrlEntity> matchingUrls = new ArrayList<>();
        ServerStream<Row> rows = bigtableClient.readRows(Query.create(TABLE_NAME));

        for (Row row : rows) {
            String rowKey = row.getKey().toStringUtf8();
            // Skip user records
            if (rowKey.startsWith("user#")) {
                continue;
            }

            // Check if the column family and column exist for this row
            if (row.getCells(CF_SHORT_URLS, COL_TAG).isEmpty()) {
                continue;
            }

            String rowTag = row.getCells(CF_SHORT_URLS, COL_TAG).get(0).getValue().toStringUtf8();

            // Check if the other required columns exist
            if (rowTag.equals(tag) &&
                    !row.getCells(CF_SHORT_URLS, COL_ORIGINAL_URL).isEmpty() &&
                    !row.getCells(CF_METADATA, COL_LAST_ACCESS).isEmpty() &&
                    !row.getCells(CF_METADATA, COL_CLICK_COUNT).isEmpty()) {

                // All required columns exist, safe to proceed
                UrlEntity urlEntity = buildUrlEntityFromRow(row);

                // Skip private URLs unless requested by owner
                if (urlEntity.isPrivate()) {
                    continue;
                }

                matchingUrls.add(urlEntity);
            }
        }
        return matchingUrls;
    }

    public List<UrlEntity> getUrlsByTagAndOwnerId(String tag, String ownerId) {
        List<UrlEntity> matchingUrls = new ArrayList<>();
        ServerStream<Row> rows = bigtableClient.readRows(Query.create(TABLE_NAME));

        for (Row row : rows) {
            String rowKey = row.getKey().toStringUtf8();
            // Skip user records
            if (rowKey.startsWith("user#")) {
                continue;
            }

            // Skip records without tag or owner
            if (row.getCells(CF_SHORT_URLS, COL_TAG).isEmpty() ||
                    row.getCells(CF_SHORT_URLS, COL_OWNER_ID).isEmpty()) {
                continue;
            }

            String rowTag = row.getCells(CF_SHORT_URLS, COL_TAG).get(0).getValue().toStringUtf8();
            String rowOwnerId = row.getCells(CF_SHORT_URLS, COL_OWNER_ID).get(0).getValue().toStringUtf8();

            // Check matching tag and owner
            if (rowTag.equals(tag) && rowOwnerId.equals(ownerId) &&
                    !row.getCells(CF_SHORT_URLS, COL_ORIGINAL_URL).isEmpty() &&
                    !row.getCells(CF_METADATA, COL_LAST_ACCESS).isEmpty() &&
                    !row.getCells(CF_METADATA, COL_CLICK_COUNT).isEmpty()) {

                // All required columns exist, safe to proceed
                matchingUrls.add(buildUrlEntityFromRow(row));
            }
        }
        return matchingUrls;
    }

    public void deleteUrl(String id) {
        bigtableClient.mutateRow(RowMutation.create(TABLE_NAME, id).deleteRow());
    }

    public boolean shortIdExists(String shortId) {
        return getUrlById(shortId).isPresent(); // Returns true if ID exists in the database
    }

    // Helper method to build UrlEntity from Row
    private UrlEntity buildUrlEntityFromRow(Row row) {
        String id = row.getKey().toStringUtf8();
        String originalUrl = row.getCells(CF_SHORT_URLS, COL_ORIGINAL_URL)
                .get(0).getValue().toStringUtf8();
        long clickCount = Long.parseLong(row.getCells(CF_METADATA, COL_CLICK_COUNT)
                .get(0).getValue().toStringUtf8());
        String lastAccess = row.getCells(CF_METADATA, COL_LAST_ACCESS)
                .get(0).getValue().toStringUtf8();
        String tag = row.getCells(CF_SHORT_URLS, COL_TAG).get(0).getValue().toStringUtf8();

        // Get owner ID if exists
        String ownerId = null;
        if (!row.getCells(CF_SHORT_URLS, COL_OWNER_ID).isEmpty()) {
            ownerId = row.getCells(CF_SHORT_URLS, COL_OWNER_ID).get(0).getValue().toStringUtf8();
        }

        // Get privacy flag if exists
        boolean isPrivate = false;
        if (!row.getCells(CF_METADATA, COL_IS_PRIVATE).isEmpty()) {
            isPrivate = Boolean.parseBoolean(row.getCells(CF_METADATA, COL_IS_PRIVATE)
                    .get(0).getValue().toStringUtf8());
        }

        UrlEntity entity = new UrlEntity();
        entity.setId(id);
        entity.setOriginalUrl(originalUrl);
        entity.setCreatedAt(Instant.parse(lastAccess).getEpochSecond());
        entity.setClickCount(clickCount);
        entity.setLastAccess(lastAccess);
        entity.setTag(tag);
        entity.setOwnerId(ownerId);
        entity.setPrivate(isPrivate);

        return entity;
    }

    public Optional<UserEntity> getUserByProviderAndId(String provider, String providerId) {
        // Construct the row key as it appears in Bigtable
        String rowKey = "user#" + provider + "#" + providerId;

        try {
            // Read the row directly using the constructed key
            Row row = bigtableClient.readRow(TABLE_NAME, rowKey);

            if (row == null) {
                return Optional.empty();
            }

            // Build user entity from row data
            UserEntity user = buildUserEntityFromRow(row, provider, providerId);
            return Optional.of(user);
        } catch (Exception e) {
            // Log error and return empty
            System.err.println("Error retrieving user by provider and ID: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    // Helper method to build UserEntity from Row
    private UserEntity buildUserEntityFromRow(Row row, String provider, String providerId) {
        String rowKey = row.getKey().toStringUtf8();
        UserEntity user = new UserEntity();
        user.setId(rowKey);
        user.setProvider(provider);
        user.setProviderId(providerId);

        // Extract fields using helper function
        user.setEmail(getCellValueAsString(row, "user_data", "email"));
        user.setName(getCellValueAsString(row, "user_data", "name"));
        user.setPictureUrl(getCellValueAsString(row, "user_data", "picture"));

        // Get role with default value
        String role = getCellValueAsString(row, "user_data", "role");
        user.setRole(role != null ? role : "USER");

        user.setLastLogin(getCellValueAsString(row, "user_data", "last_login"));

        return user;
    }

    // Helper method to safely get cell value as string
    private String getCellValueAsString(Row row, String columnFamily, String qualifier) {
        if (row.getCells(columnFamily, qualifier).isEmpty()) {
            return null;
        }
        return row.getCells(columnFamily, qualifier).get(0).getValue().toStringUtf8();
    }
}