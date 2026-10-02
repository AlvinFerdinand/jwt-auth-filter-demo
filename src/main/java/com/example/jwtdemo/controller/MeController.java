package com.example.jwtdemo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** A protected endpoint to demonstrate the filter actually guarding something. */
@RestController
public class MeController {

    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication authentication) {
        return Map.of(
            "username", authentication.getName(),
            "authorities", authentication.getAuthorities()
        );
    }
}
