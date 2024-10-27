package com.example.tunemerge.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.tunemerge.model.Playlist;
import com.example.tunemerge.model.User;
import com.example.tunemerge.repository.PlaylistRepository;

@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;

    @Autowired
    public PlaylistService(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    public Playlist createPlaylist(Playlist playlist) {
        return playlistRepository.save(playlist);
    }

    public Optional<Playlist> getPlaylistById(Long id) {
        return playlistRepository.findById(id);
    }

    public Optional<Playlist> getPlaylistBySpotifyId(String spotifyId) {
        return playlistRepository.findBySpotifyId(spotifyId);
    }

    public List<Playlist> getPlaylistsByUser(User user) {
        return playlistRepository.findByUser(user);
    }

   

    public Playlist updatePlaylist(Playlist playlist) {
        return playlistRepository.save(playlist);
    }

    public void deletePlaylist(Long id) {
        playlistRepository.deleteById(id);
    }

    public boolean existsBySpotifyId(String spotifyId) {
        return playlistRepository.existsBySpotifyId(spotifyId);
    }
}
