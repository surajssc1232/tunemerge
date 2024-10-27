package com.example.tunemerge.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @GetMapping("/")
    public String index() {
        logger.info("Index page requested");
        return "forward:/index.html";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        logger.info("Dashboard page requested");
        return "forward:/dashboard.html";
    }
}
