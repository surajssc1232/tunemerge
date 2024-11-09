package com.example.tunemerge.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.tunemerge.model.YtPlaylist;
import com.example.tunemerge.repository.UserTokenRepository;
import com.example.tunemerge.repository.YtPlaylistRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistListResponse;

@Service
public class YouTubeService {

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Autowired
    private YtPlaylistRepository ytPlaylistRepository;

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
            .setMaxResults(50L);

        // Execute the API request
        PlaylistListResponse response = request.execute();
        
        // Save playlists to database
        for (Playlist playlist : response.getItems()) {
            YtPlaylist ytPlaylist = new YtPlaylist();
            ytPlaylist.setPlaylistId(playlist.getId());
            ytPlaylist.setTitle(playlist.getSnippet().getTitle());
            ytPlaylist.setDescription(playlist.getSnippet().getDescription());
            ytPlaylistRepository.save(ytPlaylist);
        }

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

    public PlaylistItemListResponse getPlaylistTracks(String playlistId) throws IOException {
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

        YouTube.PlaylistItems.List request = youtube.playlistItems()
            .list(Arrays.asList("snippet", "contentDetails"))
            .setPlaylistId(playlistId)
            .setMaxResults(50L);

        return request.execute();
    }

    public ResponseEntity<?> searchVideos(String query) throws IOException {
        var ytToken = userTokenRepository.findByProvider("YOUTUBE");
        if (ytToken == null) {
            throw new IllegalStateException("No YouTube access token found");
        }

        GoogleCredential credential = new GoogleCredential()
            .setAccessToken(ytToken.getAccessToken());

        YouTube youtube = new YouTube.Builder(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential)
            .setApplicationName("TuneMerge")
            .build();

        YouTube.Search.List request = youtube.search()
            .list(Arrays.asList("snippet"))
            .setQ(query)
            .setType(Collections.singletonList("video"))
            .setVideoCategoryId("10") // Music category
            .setMaxResults(10L);

        return ResponseEntity.ok(request.execute());
    }
} 