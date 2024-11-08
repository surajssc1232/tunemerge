package com.example.tunemerge.controller;

import com.example.tunemerge.repository.UserTokenRepository;
import com.example.tunemerge.model.Playlist;
import com.example.tunemerge.repository.UserTokenRepository;
import com.example.tunemerge.model.UserToken;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.tunemerge.service.YouTubeService;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



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

    private static final String SCOPE = "https://www.googleapis.com/auth/youtube";

    @Autowired
    private YouTubeService youTubeService;

    @Autowired
    private UserTokenRepository tokenRepository;

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

    @PostMapping("/playlists/{playlistId}/export/youtube")
    public ResponseEntity<?> exportToYoutube(@PathVariable String playlistId) {
        try {
            com.google.api.services.youtube.model.Playlist createdPlaylist = youTubeService.createPlaylist(playlistId);
            return ResponseEntity.ok(Map.of(
                "message", "Playlist exported successfully to YouTube!",
                "playlistId", createdPlaylist.getId(),
                "playlistUrl", "https://www.youtube.com/playlist?list=" + createdPlaylist.getId()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Authentication required: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error exporting playlist to YouTube: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error exporting playlist: " + e.getMessage()));
        }
    }
}