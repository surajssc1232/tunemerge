package com.example.tunemerge.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tunemerge.model.SearchResult;
import com.example.tunemerge.model.TrackInfo;
import com.example.tunemerge.model.UserToken;
import com.example.tunemerge.repository.UserTokenRepository;
import com.example.tunemerge.service.SpotifyService;
import com.example.tunemerge.service.YouTubeService;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;



@RestController
@RequestMapping("/api/youtube")
public class YouTubeController {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeController.class);

    @Value("${youtube.client.id}")
    private String clientId;

    @Value("${youtube.client.secret}")
    private String clientSecret;

    @Value("${youtube.redirect.uri}")
    private String redirectUri;

    private static final String SCOPE = "https://www.googleapis.com/auth/youtube.readonly";

    @Autowired
    private YouTubeService youTubeService;

    @Autowired
    private UserTokenRepository tokenRepository;

    @Autowired
    private SpotifyService spotifyService;

    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
        try {
            GoogleClientSecrets.Details web = new GoogleClientSecrets.Details();
            web.setClientId(clientId);
            web.setClientSecret(clientSecret);
            GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setWeb(web);

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientSecrets,
                    Collections.singleton(SCOPE))
                    .setAccessType("offline")
                    .build();

            String authUrl = flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .build();

            Map<String, String> response = new HashMap<>();
            response.put("authorizationUrl", authUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestParam("code") String code) {
        try {
            GoogleClientSecrets.Details web = new GoogleClientSecrets.Details();
            web.setClientId(clientId);
            web.setClientSecret(clientSecret);
            GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setWeb(web);

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientSecrets,
                    Collections.singleton(SCOPE))
                    .setAccessType("offline")
                    .build();

            GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(redirectUri)
                    .execute();

            // Create and save token
            UserToken userToken = new UserToken();
            userToken.setAccessToken(tokenResponse.getAccessToken());
            userToken.setRefreshToken(tokenResponse.getRefreshToken());
            userToken.setProvider("YOUTUBE");
            userToken.setCreatedAt(LocalDateTime.now());
            userToken.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));
            
            tokenRepository.save(userToken);

            // Redirect to YouTube dashboard
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/ytdashboard.html")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error during authentication: " + e.getMessage());
        }
    }

    @GetMapping("/playlists")
    public ResponseEntity<?> getUserPlaylists() {
        try {
            List<com.google.api.services.youtube.model.Playlist> playlists = youTubeService.getUserPlaylists();
            return ResponseEntity.ok(playlists);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Authentication required: " + e.getMessage());
        } catch (IOException e) {
            logger.error("Error fetching YouTube playlists: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching playlists: " + e.getMessage());
        }
    }

    @GetMapping("/playlists/{playlistId}/tracks")
    public ResponseEntity<?> getPlaylistTracks(@PathVariable String playlistId) {
        try {
            var tracks = youTubeService.getPlaylistTracks(playlistId);
            return ResponseEntity.ok(tracks);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Authentication required: " + e.getMessage());
        } catch (IOException e) {
            logger.error("Error fetching YouTube playlist tracks: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching tracks: " + e.getMessage());
        }
    }

    @GetMapping("/export-to-spotify")
    public ResponseEntity<?> exportToSpotify(
            @RequestParam String playlistId,
            @RequestParam String spotifyId) {
        try {
            logger.info("Starting export of YouTube playlist {} to Spotify", playlistId);
            
            // Get YouTube playlist tracks
            var playlistItems = youTubeService.getPlaylistTracks(playlistId);
            List<SearchResult> matchedTracks = new ArrayList<>();
            List<String> unmatchedTracks = new ArrayList<>();

            // Process each track
            for (var item : playlistItems.getItems()) {
                String videoTitle = item.getSnippet().getTitle();
                TrackInfo trackInfo = extractTrackInfo(videoTitle);
                
                SearchResult match = spotifyService.findBestMatch(
                    trackInfo.getTitle(),
                    trackInfo.getArtist(),
                    spotifyId
                );
                
                if (match != null && match.getSimilarity() > 0.6) { // Threshold for good matches
                    matchedTracks.add(match);
                } else {
                    unmatchedTracks.add(videoTitle);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("matched", matchedTracks);
            result.put("unmatched", unmatchedTracks);
            
            logger.info("Export completed. Matched: {}, Unmatched: {}", 
                matchedTracks.size(), unmatchedTracks.size());
                
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error exporting playlist to Spotify: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error exporting playlist: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchVideos(
        @RequestParam String query) {
        try {
            return youTubeService.searchVideos(query);
        } catch (Exception e) {
            logger.error("Error searching videos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error searching videos: " + e.getMessage());
        }
    }

    private TrackInfo extractTrackInfo(String videoTitle) {
        // Remove common YouTube music video suffixes
        videoTitle = videoTitle.replaceAll("(?i)(\\(Official.*?\\))|(\\[Official.*?\\])", "")
                              .replaceAll("(?i)(\\(Lyric.*?\\))|(\\[Lyric.*?\\])", "")
                              .replaceAll("(?i)(\\(Audio.*?\\))|(\\[Audio.*?\\])", "")
                              .replaceAll("(?i)(\\(Music.*?\\))|(\\[Music.*?\\])", "")
                              .trim();

        // Try to split by common separators
        String[] parts = videoTitle.split(" - |–|:|\\|", 2);
        
        if (parts.length == 2) {
            return new TrackInfo(
                parts[1].trim(), // title
                parts[0].trim()  // artist
            );
        }
        
        // If no clear separation, return the whole title as the track name
        return new TrackInfo(videoTitle.trim(), "");
    }
}