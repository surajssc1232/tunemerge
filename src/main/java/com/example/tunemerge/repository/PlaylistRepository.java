package com.example.tunemerge.repository;

import com.example.tunemerge.model.Playlist;
import com.example.tunemerge.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUser(User user);
    Optional<Playlist> findBySpotifyId(String spotifyId);
    boolean existsBySpotifyId(String spotifyId);
}
