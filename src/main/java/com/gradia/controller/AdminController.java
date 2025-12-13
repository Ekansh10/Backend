package com.gradia.controller;

import com.gradia.model.Profile;
import com.gradia.model.User;
import com.gradia.repository.ProfileRepository;
import com.gradia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AdminController {
    
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<User> users = userRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("total", users.size());
        response.put("users", users);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/profiles")
    public ResponseEntity<Map<String, Object>> getAllProfiles() {
        List<Profile> profiles = profileRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("total", profiles.size());
        response.put("profiles", profiles);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalUsers = userRepository.count();
        long totalProfiles = profileRepository.count();
        long candidates = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.UserRole.CANDIDATE)
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalProfiles", totalProfiles);
        stats.put("candidates", candidates);
        stats.put("usersWithoutProfiles", totalUsers - totalProfiles);
        
        return ResponseEntity.ok(stats);
    }
}

