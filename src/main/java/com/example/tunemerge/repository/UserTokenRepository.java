package com.example.tunemerge.repository;

import com.example.tunemerge.model.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    UserToken findByProvider(String provider);
} 