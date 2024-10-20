package com.example.tunemerge.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.example.tunemerge.service.SpotifyService;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {

    private final SpotifyService spotifyService;

    @Autowired
    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    @GetMapping("/login")
    public RedirectView login() {
        String authorizationUrl = spotifyService.getAuthorizationUrl();
        return new RedirectView(authorizationUrl);
    }

    @GetMapping("/callback")
    public String callback(@RequestParam("code") String code) {
        spotifyService.exchangeCodeForTokens(code);
        return "Authentication successful!";
    }

    @GetMapping("/search")
    public ResponseEntity<String> searchTracks(@RequestParam("query") String query) {
        return spotifyService.searchTracks(query);
    }

    @GetMapping("/track/{id}")
    public ResponseEntity<String> getTrack(@PathVariable("id") String trackId) {
        return spotifyService.getTrack(trackId);
    }
}
