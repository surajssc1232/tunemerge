package com.example.tunemerge.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tunemerge.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findFirstBySpotifyId(String spotifyId);
    Optional<User> findByEmail(String email);
    boolean existsBySpotifyId(String spotifyId);
}
