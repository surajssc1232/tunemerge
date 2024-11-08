package com.example.tunemerge.repository;

import com.example.tunemerge.model.YtPlaylist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YtPlaylistRepository extends JpaRepository<YtPlaylist, String> {
    // Add any custom queries if needed
} 