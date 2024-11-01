package com.example.tunemerge.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.tunemerge.model.Playlist;
import com.example.tunemerge.model.User;
import com.example.tunemerge.repository.PlaylistRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PlaylistService extends BaseService<Playlist, Long> {
    private static final Logger logger = LoggerFactory.getLogger(PlaylistService.class);
    private final PlaylistRepository playlistRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public PlaylistService(PlaylistRepository repository, ObjectMapper objectMapper) {
        super(repository);
        this.playlistRepository = repository;
        this.objectMapper = objectMapper;
    }

    public Optional<Playlist> getPlaylistBySpotifyId(String spotifyId) {
        return playlistRepository.findBySpotifyId(spotifyId);
    }

    public List<Playlist> getPlaylistsByUser(User user) {
        return playlistRepository.findByUser(user);
    }

    public boolean existsBySpotifyId(String spotifyId) {
        return playlistRepository.existsBySpotifyId(spotifyId);
    }

    public List<Playlist> savePlaylistsFromSpotifyResponse(String playlistsJson, User user) {
        try {
            JsonNode root = objectMapper.readTree(playlistsJson);
            JsonNode items = root.get("items");
            List<Playlist> savedPlaylists = new ArrayList<>();

            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    String spotifyId = item.get("id").asText();
                    
                    // Check if playlist already exists
                    Optional<Playlist> existingPlaylist = getPlaylistBySpotifyId(spotifyId);
                    if (existingPlaylist.isPresent()) {
                        logger.debug("Playlist with Spotify ID {} already exists", spotifyId);
                        continue;
                    }

                    Playlist playlist = new Playlist();
                    playlist.setSpotifyId(spotifyId);
                    playlist.setName(item.get("name").asText());
                    playlist.setUser(user);

                    try {
                        Playlist savedPlaylist = playlistRepository.save(playlist);
                        savedPlaylists.add(savedPlaylist);
                        logger.info("Saved playlist: {} with Spotify ID: {}", playlist.getName(), playlist.getSpotifyId());
                    } catch (Exception e) {
                        logger.error("Error saving playlist with Spotify ID {}: {}", spotifyId, e.getMessage());
                    }
                }
            }

            return savedPlaylists;
        } catch (IOException e) {
            logger.error("Error parsing Spotify playlists JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to parse Spotify playlists response", e);
        }
    }
}
