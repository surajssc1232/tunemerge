package com.example.tunemerge.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
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

import com.example.tunemerge.model.User;
import com.example.tunemerge.service.PlaylistService;
import com.example.tunemerge.service.SpotifyService;
import com.example.tunemerge.service.UserService;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {

    private final SpotifyService spotifyService;
    private final UserService userService;
    private final PlaylistService playlistService;
    private static final Logger logger = LoggerFactory.getLogger(SpotifyController.class);

    @Autowired
    public SpotifyController(SpotifyService spotifyService, UserService userService, PlaylistService playlistService) {
        this.spotifyService = spotifyService;
        this.userService = userService;
        this.playlistService = playlistService;
    }

    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login() {
        logger.info("Initiating Spotify login process");
        String authorizationUrl = spotifyService.getAuthorizationUrl();
        logger.info("Generated authorization URL: {}", authorizationUrl);
        Map<String, String> response = new HashMap<>();
        response.put("authorizationUrl", authorizationUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/callback")
    public ModelAndView callback(@RequestParam("code") String code, @RequestParam("state") String state) {
        logger.info("Received callback with code: {} and state: {}", code, state);
        try {
            String spotifyId = spotifyService.exchangeCodeForTokens(code);
            logger.info("Tokens exchanged successfully, Spotify ID: {}", spotifyId);
            return new ModelAndView("redirect:/dashboard.html?spotifyId=" + spotifyId);
        } catch (Exception e) {
            logger.error("Error during token exchange", e);
            return new ModelAndView("redirect:/error?message=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/me/playlists")
    public ResponseEntity<String> getUserPlaylists(@RequestParam String spotifyId) {
        logger.info("Getting playlists for Spotify ID: {}", spotifyId);
        return spotifyService.getUserPlaylists(spotifyId);
    }

    

    @GetMapping("/me")
    public ResponseEntity<String> getUserProfile(@RequestParam String spotifyId) {
        logger.info("Getting user profile for Spotify ID: {}", spotifyId);
        return spotifyService.getUserProfile(spotifyId);
    }

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

    @GetMapping("/me/playlists/{playlistId}/tracks")
    public ResponseEntity<String> getPlaylistTracks(
            @PathVariable("playlistId") String playlistId,
            @RequestParam String spotifyId) {
        logger.info("Getting tracks for playlist ID: {} and Spotify ID: {}", playlistId, spotifyId);
        return spotifyService.getPlaylistTracks(playlistId, spotifyId);
    }
}
