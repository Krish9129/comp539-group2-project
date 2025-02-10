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
    private long createdAt;          // created time

    // analysis data
    private long clickCount;         // click count
    private String lastAccess;
}