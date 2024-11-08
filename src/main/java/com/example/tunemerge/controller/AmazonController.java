package com.example.tunemerge.controller;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

@RestController
@RequestMapping("/api/amazon")
public class AmazonController {
    private static final Logger logger = LoggerFactory.getLogger(AmazonController.class);

    

    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login() {
        logger.info("Initiating Amazon login process");
        String authorizationUrl = amazonService.getAuthorizationUrl();
        logger.info("Generated Amazon authorization URL: {}", authorizationUrl);
        Map<String, String> response = new HashMap<>();
        response.put("authorizationUrl", authorizationUrl);
        return ResponseEntity.ok(response);
    }
    
}
