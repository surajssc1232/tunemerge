package com.example.tunemerge.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.tunemerge.model.Playlist;
import com.example.tunemerge.model.Track;
import com.example.tunemerge.repository.TrackRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TrackService {
    private static final Logger logger = LoggerFactory.getLogger(TrackService.class);
    private final TrackRepository trackRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public TrackService(TrackRepository trackRepository, ObjectMapper objectMapper) {
        this.trackRepository = trackRepository;
        this.objectMapper = objectMapper;
    }

    public Track createTrack(Track track) {
        return trackRepository.save(track);
    }

    public Optional<Track> getTrackById(Long id) {
        return trackRepository.findById(id);
    }

    public Optional<Track> getTrackBySpotifyId(String spotifyId) {
        return trackRepository.findBySpotifyId(spotifyId);
    }

    public List<Track> getTracksByPlaylist(Playlist playlist) {
        return trackRepository.findByPlaylist(playlist);
    }

    public Track updateTrack(Track track) {
        return trackRepository.save(track);
    }

    public void deleteTrack(Long id) {
        trackRepository.deleteById(id);
    }

    public boolean existsBySpotifyIdAndPlaylist(String spotifyId, Playlist playlist) {
        return trackRepository.existsBySpotifyIdAndPlaylist(spotifyId, playlist);
    }

    public void saveTracksFromSpotifyResponse(String tracksJson, Playlist playlist) {
        try {
            JsonNode root = objectMapper.readTree(tracksJson);
            JsonNode items = root.get("items");

            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    JsonNode trackNode = item.get("track");
                    if (trackNode == null) {
                        continue;
                    }

                    String spotifyId = trackNode.get("id").asText();
                    
                    // Skip if track already exists in this playlist
                    if (existsBySpotifyIdAndPlaylist(spotifyId, playlist)) {
                        logger.debug("Track {} already exists in playlist {}", spotifyId, playlist.getId());
                        continue;
                    }

                    Track track = new Track();
                    track.setSpotifyId(spotifyId);
                    track.setName(trackNode.get("name").asText());
                    
                    // Get first artist name
                    JsonNode artists = trackNode.get("artists");
                    if (artists != null && artists.isArray() && artists.size() > 0) {
                        track.setArtist(artists.get(0).get("name").asText());
                    } else {
                        track.setArtist("Unknown Artist");
                    }

                    // Get album name
                    JsonNode album = trackNode.get("album");
                    if (album != null) {
                        track.setAlbum(album.get("name").asText());
                    } else {
                        track.setAlbum("Unknown Album");
                    }

                    track.setPlaylist(playlist);

                    try {
                        trackRepository.save(track);
                        logger.info("Saved track: {} with Spotify ID: {} to playlist: {}", 
                            track.getName(), track.getSpotifyId(), playlist.getName());
                    } catch (Exception e) {
                        logger.error("Error saving track with Spotify ID {}: {}", spotifyId, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing Spotify tracks JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to parse Spotify tracks response", e);
        }
    }
}
