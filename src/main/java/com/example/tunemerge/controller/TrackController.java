package com.example.tunemerge.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tunemerge.model.Track;
import com.example.tunemerge.service.PlaylistService;
import com.example.tunemerge.service.TrackService;

@RestController
@RequestMapping("/api/tracks")
public class TrackController {

    private final TrackService trackService;
    private final PlaylistService playlistService;

    @Autowired
    public TrackController(TrackService trackService, PlaylistService playlistService) {
        this.trackService = trackService;
        this.playlistService = playlistService;
    }

    @PostMapping
    public ResponseEntity<Track> createTrack(@RequestBody Track track) {
        Track createdTrack = trackService.createTrack(track);
        return new ResponseEntity<>(createdTrack, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Track> getTrackById(@PathVariable Long id) {
        return trackService.getTrackById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/spotify/{spotifyId}")
    public ResponseEntity<Track> getTrackBySpotifyId(@PathVariable String spotifyId) {
        return trackService.getTrackBySpotifyId(spotifyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/playlist/{playlistId}")
    public ResponseEntity<List<Track>> getTracksByPlaylist(@PathVariable Long playlistId) {
        return playlistService.getPlaylistById(playlistId)
                .map(playlist -> ResponseEntity.ok(trackService.getTracksByPlaylist(playlist)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Track>> getAllTracks() {
        List<Track> tracks = trackService.getAllTracks();
        return ResponseEntity.ok(tracks);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Track> updateTrack(@PathVariable Long id, @RequestBody Track track) {
        if (!trackService.getTrackById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        track.setId(id);
        Track updatedTrack = trackService.updateTrack(track);
        return ResponseEntity.ok(updatedTrack);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrack(@PathVariable Long id) {
        if (!trackService.getTrackById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        trackService.deleteTrack(id);
        return ResponseEntity.noContent().build();
    }
}
