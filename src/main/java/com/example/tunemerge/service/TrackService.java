package com.example.tunemerge.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.tunemerge.model.Playlist;
import com.example.tunemerge.model.Track;
import com.example.tunemerge.repository.TrackRepository;

@Service
public class TrackService {

    private final TrackRepository trackRepository;

    @Autowired
    public TrackService(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
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
}
