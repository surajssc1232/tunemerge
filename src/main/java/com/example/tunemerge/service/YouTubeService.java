package com.example.tunemerge.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

@Service
public class YouTubeService {

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Autowired
    private YtPlaylistRepository ytPlaylistRepository;

    @Autowired
    private RestTemplate restTemplate;

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

    public SearchResult findBestMatch(String trackName, String artistName) throws IOException {
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

        String query = trackName + " " + artistName;
        YouTube.Search.List request = youtube.search()
            .list(Arrays.asList("snippet"))
            .setQ(query)
            .setType(Collections.singletonList("video"))
            .setVideoCategoryId("10")
            .setMaxResults(1L);

        SearchListResponse response = request.execute();
        
        if (!response.getItems().isEmpty()) {
            SearchResult result = response.getItems().get(0);
            double similarity = calculateSimilarity(
                (trackName + " " + artistName).toLowerCase(),
                result.getSnippet().getTitle().toLowerCase()
            );
            return result;
        }
        return null;
    }

    private double calculateSimilarity(String s1, String s2) {
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        return 1.0 - ((double) distance / maxLength);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }

    public ResponseEntity<String> getPlaylistItems(String playlistId) {
        String url = String.format("https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&playlistId=%s&maxResults=50", playlistId);
        return restTemplate.exchange(url, HttpMethod.GET, createHttpEntity(), String.class);
    }

    private HttpEntity<?> createHttpEntity() {
        var ytToken = userTokenRepository.findByProvider("YOUTUBE");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ytToken.getAccessToken());
        return new HttpEntity<>(headers);
    }
} 