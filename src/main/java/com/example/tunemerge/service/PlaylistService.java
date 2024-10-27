package com.example.tunemerge.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.tunemerge.model.Playlist;
import com.example.tunemerge.model.User;
import com.example.tunemerge.repository.PlaylistRepository;

@Service
public class PlaylistService extends BaseService<Playlist, Long> {
    private final PlaylistRepository playlistRepository;

    @Autowired
    public PlaylistService(PlaylistRepository repository) {
        super(repository);
        this.playlistRepository = repository;
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
}
