package com.example.urlshortenerbackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlEntity {
    // basic info
    private String id;                // shortURL ID
    private String originalUrl;       // original URL
    private long createdAt;           // created time
    private String tag;               // tag for each URL

    // analysis data
    private long clickCount;         // click count
    private String lastAccess;


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
    public String getTag() { return tag;}

    public void setTag(String tag) { this.tag = tag;}
}