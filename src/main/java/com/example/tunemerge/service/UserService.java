package com.example.tunemerge.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.tunemerge.model.User;
import com.example.tunemerge.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User user) {
        Optional<User> existingUser = userRepository.findFirstBySpotifyId(user.getSpotifyId());
        if (existingUser.isPresent()) {
            User existing = existingUser.get();
            existing.setEmail(user.getEmail());
            existing.setDisplayName(user.getDisplayName());
            existing.setAccessToken(user.getAccessToken());
            existing.setRefreshToken(user.getRefreshToken());
            existing.setTokenExpirationTime(user.getTokenExpirationTime());
            return userRepository.save(existing);
        }
        return userRepository.save(user);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserBySpotifyId(String spotifyId) {
        return userRepository.findFirstBySpotifyId(spotifyId);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }



    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public boolean existsBySpotifyId(String spotifyId) {
        return userRepository.existsBySpotifyId(spotifyId);
    }
}
