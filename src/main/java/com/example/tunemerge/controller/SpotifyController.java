package com.example.tunemerge.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.example.tunemerge.model.SearchResult;
import com.example.tunemerge.model.User;
import com.example.tunemerge.service.PlaylistService;
import com.example.tunemerge.service.SpotifyService;
import com.example.tunemerge.service.TrackService;
import com.example.tunemerge.service.UserService;
import com.example.tunemerge.service.YouTubeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SpotifyService spotifyService;
    private final UserService userService;
    private final PlaylistService playlistService;
    private final TrackService trackService;
    private static final Logger logger = LoggerFactory.getLogger(SpotifyController.class);

    @Autowired
    private YouTubeService youTubeService;

    @Autowired
    public SpotifyController(SpotifyService spotifyService, UserService userService, PlaylistService playlistService, TrackService trackService, YouTubeService youTubeService) {
        this.spotifyService = spotifyService;
        this.userService = userService;
        this.playlistService = playlistService;
        this.trackService = trackService;
        this.youTubeService = youTubeService;
    }

    // sends the user to spotify by getting the authorization url
    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login() {
        logger.info("Initiating Spotify login process");
        String authorizationUrl = spotifyService.getAuthorizationUrl();
        logger.info("Generated authorization URL: {}", authorizationUrl);
        Map<String, String> response = new HashMap<>();
        response.put("authorizationUrl", authorizationUrl);
        return ResponseEntity.ok(response);
    }

    // spotify send the user back to this endpoint with the auth code
    @GetMapping("/callback")
    public ModelAndView callback(@RequestParam("code") String code, @RequestParam("state") String state) {
        logger.info("Received callback with code: {} and state: {}", code, state);
        try {
            String spotifyId = spotifyService.exchangeCodeForTokens(code);
            logger.info("Tokens exchanged successfully, Spotify ID: {}", spotifyId);
            
            // Return a page that sends message to opener and closes itself
            return new ModelAndView("redirect:/auth-success.html?spotifyId=" + spotifyId);
        } catch (Exception e) {
            logger.error("Error during token exchange", e);
            return new ModelAndView("redirect:/error?message=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
    // this endpoint gets the user's playlists from spotify
    @GetMapping("/me/playlists")
    public ResponseEntity<String> getUserPlaylists(@RequestParam String spotifyId) {
        logger.info("Getting playlists for Spotify ID: {}", spotifyId);
        return spotifyService.getUserPlaylists(spotifyId);
    }

    
    // this is called after the user logs in and gets thier profile
    @GetMapping("/me")
    public ResponseEntity<String> getUserProfile(@RequestParam String spotifyId) {
        logger.info("Getting user profile for Spotify ID: {}", spotifyId);
        return spotifyService.getUserProfile(spotifyId);
    }

    // this is the dashboard that the user sees after they log in
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(@RequestParam String spotifyId) {
        logger.info("Accessing dashboard for Spotify ID: {}", spotifyId);
        Map<String, Object> dashboardData = new HashMap<>();
        
        try {
            ResponseEntity<String> userProfileResponse = spotifyService.getUserProfile(spotifyId);
            ResponseEntity<String> userPlaylistsResponse = spotifyService.getUserPlaylists(spotifyId);
            
            // Store the playlists in the database
            Optional<User> userOpt = userService.getUserBySpotifyId(spotifyId);
            if (userOpt.isPresent()) {
                playlistService.savePlaylistsFromSpotifyResponse(userPlaylistsResponse.getBody(), userOpt.get());
            }
            
            dashboardData.put("userProfile", userProfileResponse.getBody());
            dashboardData.put("userPlaylists", userPlaylistsResponse.getBody());
            dashboardData.put("spotifyId", spotifyId);
            
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            logger.error("Error fetching dashboard data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch dashboard data"));
        }
    }

    @GetMapping("/error")
    public ResponseEntity<String> error(@RequestParam(required = false) String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred: " + (message != null ? message : "Unknown error"));
    }

    // this endpoint get the tracks for a playlist with a given playlistId
    @GetMapping("/me/playlists/{playlistId}/tracks")
    public ResponseEntity<String> getPlaylistTracks(
            @PathVariable("playlistId") String playlistId,
            @RequestParam String spotifyId) {
        logger.info("Getting tracks for playlist ID: {} and Spotify ID: {}", playlistId, spotifyId);
        return spotifyService.getPlaylistTracks(playlistId, spotifyId);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchTracks(
        @RequestParam String query,
        @RequestParam String spotifyId) {
        try {
            return spotifyService.searchTracks(query, spotifyId);
        } catch (Exception e) {
            logger.error("Error searching tracks: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error searching tracks: " + e.getMessage());
        }
    }

    @GetMapping("/export-to-youtube")
    public ResponseEntity<?> exportToYoutube(
            @RequestParam String playlistId,
            @RequestParam(required = false) String spotifyId,
            @RequestParam(required = false) String youtubeToken) {
        try {
            logger.info("Starting export process for playlist {} with spotifyId {}", playlistId, spotifyId);
            
            // Check if user is authenticated with Spotify
            if (spotifyId == null || spotifyId.isEmpty()) {
                logger.info("User not authenticated with Spotify, returning authorization URL");
                String authUrl = spotifyService.getAuthorizationUrl();
                Map<String, String> response = new HashMap<>();
                response.put("needsAuth", "true");
                response.put("authUrl", authUrl);
                return ResponseEntity.ok(response);
            }

            // Validate user exists and has valid tokens
            Optional<User> userOpt = userService.getUserBySpotifyId(spotifyId);
            if (userOpt.isEmpty()) {
                logger.error("User not found for Spotify ID: {}", spotifyId);
                Map<String, String> response = new HashMap<>();
                response.put("needsAuth", "true");
                response.put("authUrl", spotifyService.getAuthorizationUrl());
                return ResponseEntity.ok(response);
            }

            User user = userOpt.get();
            // Check if token is expired
            if (System.currentTimeMillis() > user.getTokenExpirationTime()) {
                logger.info("Token expired, refreshing...");
                spotifyService.refreshAccessToken(user);
            }

            logger.info("Starting export of YouTube playlist {} to Spotify", playlistId);
            
            // Get YouTube playlist tracks instead of Spotify
            ResponseEntity<String> playlistTracksResponse = youTubeService.getPlaylistItems(playlistId);
            if (playlistTracksResponse == null || playlistTracksResponse.getBody() == null) {
                logger.error("Failed to fetch playlist tracks from YouTube");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch playlist tracks"));
            }

            List<SearchResult> matchedTracks = new ArrayList<>();
            List<String> unmatchedTracks = new ArrayList<>();
            
            // Parse YouTube response
            JsonNode playlistItems = objectMapper.readTree(playlistTracksResponse.getBody()).get("items");
            logger.info("Found {} tracks to process", playlistItems.size());

            for (JsonNode item : playlistItems) {
                JsonNode snippet = item.get("snippet");
                if (snippet == null || snippet.isNull()) {
                    logger.warn("Skipping null snippet in playlist item");
                    continue;
                }
                
                String title = snippet.get("title").asText();
                logger.info("Processing YouTube track: {}", title);
                
                try {
                    // Search this track on Spotify
                    ResponseEntity<String> spotifySearchResponse = spotifyService.searchTracks(title, spotifyId);
                    JsonNode searchResults = objectMapper.readTree(spotifySearchResponse.getBody());
                    JsonNode tracks = searchResults.get("tracks").get("items");
                    
                    if (tracks.size() > 0) {
                        JsonNode bestMatch = tracks.get(0);
                        String trackId = bestMatch.get("id").asText();
                        String trackName = bestMatch.get("name").asText();
                        String artistName = bestMatch.get("artists").get(0).get("name").asText();
                        
                        SearchResult match = new SearchResult(trackId, trackName, artistName, 0.8);
                        matchedTracks.add(match);
                        logger.info("Found Spotify match for: {} (Track ID: {})", title, trackId);
                    } else {
                        logger.warn("No Spotify match found for: {}", title);
                        unmatchedTracks.add(title);
                    }
                } catch (Exception e) {
                    logger.error("Error processing track {}: {}", title, e.getMessage());
                    unmatchedTracks.add(title + " (Error: " + e.getMessage() + ")");
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("matched", matchedTracks);
            result.put("unmatched", unmatchedTracks);
            logger.info("Export completed. Matched: {}, Unmatched: {}", matchedTracks.size(), unmatchedTracks.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error exporting playlist to YouTube: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
