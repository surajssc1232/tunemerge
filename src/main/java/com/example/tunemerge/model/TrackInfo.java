package com.example.tunemerge.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackInfo {
    private String title;    // The title of the track
    private String artist;   // The artist name
} 