package com.example.tunemerge.repository;

import com.example.tunemerge.model.Playlist;
import com.example.tunemerge.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {
    List<Track> findByPlaylist(Playlist playlist);
    Optional<Track> findBySpotifyId(String spotifyId);
    boolean existsBySpotifyIdAndPlaylist(String spotifyId, Playlist playlist);
}
