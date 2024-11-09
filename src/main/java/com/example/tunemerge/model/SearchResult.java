package com.example.tunemerge.model;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class SearchResult {
    private String id;        // Spotify track ID
    private String name;      // Track name
    private String artist;    // Artist name
    private double similarity; // Similarity score between YouTube title and Spotify track
    
    // Constructor without similarity score
    public SearchResult(String id, String name, String artist) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.similarity = 0.0;
    }
} 