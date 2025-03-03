package com.example.urlshortenerbackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UrlEntity {
    // basic info
    private String id;                // shortURL ID
    private String originalUrl;       // original URL
    private long createdAt;           // created time
    private String tag;               // tag for each URL
    private String ownerId;           // owner of the URL (format: provider#providerId)
    private boolean isPrivate;        // whether URL is private

    // analysis data
    private long clickCount;         // click count
    private String lastAccess;


    // Constructor with all fields
    public UrlEntity(String id, String originalUrl, long createdAt, String tag,
                     String ownerId, boolean isPrivate, long clickCount, String lastAccess) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.tag = tag;
        this.ownerId = ownerId;
        this.isPrivate = isPrivate;
        this.clickCount = clickCount;
        this.lastAccess = lastAccess;
    }

    // Constructor without ownerId and isPrivate (for backward compatibility)
    public UrlEntity(String id, String originalUrl, long createdAt, String tag,
                     long clickCount, String lastAccess) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.tag = tag;
        this.clickCount = clickCount;
        this.lastAccess = lastAccess;
        this.ownerId = null;
        this.isPrivate = false;
    }

    // Getter and Setter for id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Getter and Setter for originalUrl
    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    // Getter and Setter for createdAt
    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // Getter and Setter for clickCount
    public long getClickCount() {
        return clickCount;
    }

    public void setClickCount(long clickCount) {
        this.clickCount = clickCount;
    }

    // Getter and Setter for lastAccess
    public String getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(String lastAccess) {
        this.lastAccess = lastAccess;
    }

    // Getter and Setter for tag
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    // Getter and Setter for ownerId
    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    // Getter and Setter for isPrivate
    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}