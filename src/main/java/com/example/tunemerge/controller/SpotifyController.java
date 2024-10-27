package com.example.tunemerge.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.tunemerge.service.SpotifyService;

@Controller
@RequestMapping("/api/spotify")
public class SpotifyController {

    private final SpotifyService spotifyService;

    @Autowired
    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    @GetMapping("/login")
    @ResponseBody
    public ResponseEntity<Map<String, String>> login() {
        String authorizationUrl = spotifyService.getAuthorizationUrl();
        Map<String, String> response = new HashMap<>();
        response.put("authorizationUrl", authorizationUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/callback")
    public String callback(@RequestParam("code") String code) {
        spotifyService.exchangeCodeForTokens(code);
        return "redirect:/dashboard.html";
    }

    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<String> searchTracks(@RequestParam("query") String query) {
        return spotifyService.searchTracks(query);
    }

    @GetMapping("/track/{id}")
    @ResponseBody
    public ResponseEntity<String> getTrack(@PathVariable("id") String trackId) {
        return spotifyService.getTrack(trackId);
    }

    @GetMapping("/me/playlists")
    @ResponseBody
    public ResponseEntity<String> getUserPlaylists() {
        return spotifyService.getUserPlaylists();
    }

    @GetMapping("/me/playlists/{playlistId}/tracks")
    @ResponseBody
    public ResponseEntity<String> getPlaylistTracks(@PathVariable("playlistId") String playlistId) {
        return spotifyService.getPlaylistTracks(playlistId);
    }

    @GetMapping("/me")
    @ResponseBody
    public ResponseEntity<String> getUserProfile() {
        return spotifyService.getUserProfile();
    }

    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "SpotifyController is working";
    }
}
