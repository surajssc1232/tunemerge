package com.example.tunemerge.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.Playlist;
import com.example.tunemerge.repository.UserTokenRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

@Service
public class YouTubeService {

    @Autowired
    private UserTokenRepository userTokenRepository;

    public List<Playlist> getUserPlaylists() throws IOException {
        // Get the most recent YouTube token
        var ytToken = userTokenRepository.findByProvider("YOUTUBE");
        if (ytToken == null) {
            throw new IllegalStateException("No YouTube access token found. Please authenticate first.");
        }

        // Create credentials with the access token
        GoogleCredential credential = new GoogleCredential().setAccessToken(ytToken.getAccessToken());

        // Create YouTube service
        YouTube youtube = new YouTube.Builder(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential)
            .setApplicationName("TuneMerge")
            .build();

        // Call the YouTube API to get playlists
        YouTube.Playlists.List request = youtube.playlists()
            .list(Arrays.asList("snippet", "contentDetails"))
            .setMine(true)
            .setMaxResults(50L); // Adjust this number as needed

        // Execute the API request
        PlaylistListResponse response = request.execute();
        return response.getItems();
    }

    // Helper method to get playlist details if needed
    public Playlist getPlaylistById(String playlistId) throws IOException {
        var ytToken = userTokenRepository.findByProvider("YOUTUBE");
        if (ytToken == null) {
            throw new IllegalStateException("No YouTube access token found. Please authenticate first.");
        }

        GoogleCredential credential = new GoogleCredential().setAccessToken(ytToken.getAccessToken());

        YouTube youtube = new YouTube.Builder(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential)
            .setApplicationName("TuneMerge")
            .build();

        YouTube.Playlists.List request = youtube.playlists()
            .list(Arrays.asList("snippet", "contentDetails"))
            .setId(Collections.singletonList(playlistId));

        PlaylistListResponse response = request.execute();
        return response.getItems().get(0);
    }
} 