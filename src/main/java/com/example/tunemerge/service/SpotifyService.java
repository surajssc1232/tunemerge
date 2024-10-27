package com.example.tunemerge.service;

import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.tunemerge.model.SpotifyTokenResponse;

@Service
public class SpotifyService {

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    @Value("${spotify.redirect.uri}")
    private String redirectUri;

    @Value("${app.base-url}")
    private String baseUrl;

    private final String BASE_URL = "https://api.spotify.com/v1";
    private final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private final RestTemplate restTemplate;

    private String accessToken;
    private String refreshToken;
    private long tokenExpirationTime;

    public SpotifyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        return UriComponentsBuilder.fromHttpUrl(AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)  // Use the redirectUri from properties
                .queryParam("state", state)
                .queryParam("scope", "user-read-private user-read-email playlist-modify-public playlist-modify-private")
                .build().toUriString();
    }

    public void exchangeCodeForTokens(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<SpotifyTokenResponse> response = restTemplate.postForEntity(TOKEN_URL, request, SpotifyTokenResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            SpotifyTokenResponse tokenResponse = response.getBody();
            this.accessToken = tokenResponse.getAccessToken();
            this.refreshToken = tokenResponse.getRefreshToken();
            this.tokenExpirationTime = System.currentTimeMillis() + (tokenResponse.getExpiresIn() * 1000);
        } else {
            throw new RuntimeException("Failed to exchange code for tokens");
        }
    }

    public String getAccessToken() {
        if (accessToken == null || System.currentTimeMillis() > tokenExpirationTime) {
            refreshAccessToken();
        }
        return accessToken;
    }

    private void refreshAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<SpotifyTokenResponse> response = restTemplate.postForEntity(TOKEN_URL, request, SpotifyTokenResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            SpotifyTokenResponse tokenResponse = response.getBody();
            this.accessToken = tokenResponse.getAccessToken();
            this.tokenExpirationTime = System.currentTimeMillis() + (tokenResponse.getExpiresIn() * 1000);
            if (tokenResponse.getRefreshToken() != null) {
                this.refreshToken = tokenResponse.getRefreshToken();
            }
        } else {
            throw new RuntimeException("Failed to refresh access token");
        }
    }

   

    

    public ResponseEntity<String> getUserPlaylists() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        String url = BASE_URL + "/me/playlists";
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    public ResponseEntity<String> getPlaylistTracks(String playlistId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        String url = BASE_URL + "/playlists/" + playlistId + "/tracks";
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    public ResponseEntity<String> getUserProfile() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        String url = BASE_URL + "/me";
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    public boolean isAuthenticated() {
        return accessToken != null && System.currentTimeMillis() < tokenExpirationTime;
    }

    // Add more methods for other Spotify API endpoints as needed
}
