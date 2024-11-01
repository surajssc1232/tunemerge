package com.example.tunemerge.service;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
import com.example.tunemerge.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SpotifyService {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyService.class);

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

    private final UserService userService;

    public SpotifyService(RestTemplate restTemplate, UserService userService) {
        this.restTemplate = restTemplate;
        this.userService = userService;
    }

    public String getAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        String url = UriComponentsBuilder.fromHttpUrl(AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("scope", "user-read-private user-read-email playlist-modify-public playlist-modify-private")
                .build().toUriString();
        logger.info("Generated authorization URL: {}", url);
        return url;
    }

    public String exchangeCodeForTokens(String code) {
        logger.info("Exchanging code for tokens");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<SpotifyTokenResponse> response = restTemplate.postForEntity(TOKEN_URL, request, SpotifyTokenResponse.class);
            logger.info("Token exchange response status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                SpotifyTokenResponse tokenResponse = response.getBody();
                logger.info("Successfully received token response");
                
                HttpHeaders profileHeaders = new HttpHeaders();
                profileHeaders.setBearerAuth(tokenResponse.getAccessToken());
                HttpEntity<String> profileRequest = new HttpEntity<>("parameters", profileHeaders);
                
                ResponseEntity<Map<String, Object>> profileResponse = restTemplate.exchange(
                    BASE_URL + "/me",
                    HttpMethod.GET,
                    profileRequest,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                logger.info("User profile response status: {}", profileResponse.getStatusCode());

                if (profileResponse.getBody() != null) {
                    String spotifyId = (String) profileResponse.getBody().get("id");
                    String email = (String) profileResponse.getBody().get("email");
                    String displayName = (String) profileResponse.getBody().get("display_name");
                    logger.info("Retrieved user profile. Spotify ID: {}, Email: {}, Display Name: {}", spotifyId, email, displayName);

                    User user = userService.getUserBySpotifyId(spotifyId)
                        .orElse(new User());

                    user.setSpotifyId(spotifyId);
                    user.setEmail(email);
                    user.setDisplayName(displayName);
                    user.setAccessToken(tokenResponse.getAccessToken());
                    user.setRefreshToken(tokenResponse.getRefreshToken());
                    user.setTokenExpirationTime(System.currentTimeMillis() + (tokenResponse.getExpiresIn() * 1000));

                    userService.createUser(user);
                    logger.info("User information saved/updated in the database");
                    
                    return spotifyId;
                } else {
                    logger.error("User profile response body is null");
                    throw new RuntimeException("Failed to retrieve user profile");
                }
            } else {
                logger.error("Failed to exchange code for tokens. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to exchange code for tokens");
            }
        } catch (Exception e) {
            logger.error("Exception occurred during token exchange", e);
            throw new RuntimeException("Failed to exchange code for tokens", e);
        }
    }

    public String getAccessTokenForUser(String spotifyId) {
        User user = userService.getUserBySpotifyId(spotifyId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (System.currentTimeMillis() > user.getTokenExpirationTime()) {
            refreshAccessToken(user);
        }
        return user.getAccessToken();
    }

    private void refreshAccessToken(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", user.getRefreshToken());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<SpotifyTokenResponse> response = restTemplate.postForEntity(TOKEN_URL, request, SpotifyTokenResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            SpotifyTokenResponse tokenResponse = response.getBody();
            user.setAccessToken(tokenResponse.getAccessToken());
            user.setTokenExpirationTime(System.currentTimeMillis() + (tokenResponse.getExpiresIn() * 1000));
            if (tokenResponse.getRefreshToken() != null) {
                user.setRefreshToken(tokenResponse.getRefreshToken());
            }
            userService.updateUser(user);
        } else {
            throw new RuntimeException("Failed to refresh access token");
        }
    }

    public ResponseEntity<String> getUserPlaylists(String spotifyId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessTokenForUser(spotifyId));
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        String url = BASE_URL + "/me/playlists";
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    public ResponseEntity<String> getPlaylistTracks(String playlistId, String spotifyId) {
        HttpHeaders headers = new HttpHeaders();
        String accessToken = getAccessTokenForUser(spotifyId);
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        String url = BASE_URL + "/playlists/" + playlistId + "/tracks";
        logger.info("Fetching tracks from URL: {}", url);
        
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        logger.info("Tracks response status: {}", response.getStatusCode());
        // Removed debug logging of response body
        
        return response;
    }

    public ResponseEntity<String> getUserProfile(String spotifyId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessTokenForUser(spotifyId));
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        String url = BASE_URL + "/me";
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    public ResponseEntity<String> createPlaylist(String userId, String name, boolean isPublic, String description) {
        logger.info("Creating playlist for user ID: {}", userId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessTokenForUser(userId));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", name);
        requestBody.put("public", isPublic);
        requestBody.put("description", description);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        String url = BASE_URL + "/users/" + userId + "/playlists";

        return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    public ResponseEntity<String> addTracksToPlaylist(String playlistId, List<String> trackUris, Integer position, String spotifyId) {
        logger.info("Adding tracks to playlist: {} for user: {}", playlistId, spotifyId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessTokenForUser(spotifyId));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("uris", trackUris);
        if (position != null) {
            requestBody.put("position", position);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        String url = BASE_URL + "/playlists/" + playlistId + "/tracks";

        return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    public List<String> getTrackUrisFromResponse(String tracksJson) {
        try {
            List<String> uris = new ArrayList<>();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(tracksJson);
            JsonNode items = root.get("items");

            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    JsonNode track = item.get("track");
                    if (track != null && track.has("uri")) {
                        uris.add(track.get("uri").asText());
                    }
                }
            }
            return uris;
        } catch (Exception e) {
            logger.error("Error parsing track URIs: {}", e.getMessage());
            throw new RuntimeException("Failed to parse track URIs", e);
        }
    }

}
