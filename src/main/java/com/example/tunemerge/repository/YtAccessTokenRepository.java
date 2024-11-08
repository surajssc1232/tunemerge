package com.example.tunemerge.repository;

import com.example.tunemerge.model.YtAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface YtAccessTokenRepository extends JpaRepository<YtAccessToken, Long> {
    YtAccessToken findByProvider(String provider);
} 