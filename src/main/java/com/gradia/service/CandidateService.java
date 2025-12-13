package com.gradia.service;

import com.gradia.dto.*;
import com.gradia.model.Profile;
import com.gradia.model.User;
import com.gradia.repository.ProfileRepository;
import com.gradia.repository.UserRepository;
import com.gradia.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CandidateService {
    
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    @Transactional
    public AuthResponse register(CandidateRegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.UserRole.CANDIDATE);
        user.setIsActive(true);
        user.setEmailVerified(false);
        
        user = userRepository.save(user);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId().toString());
        
        // Create response
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
        userInfo.setId(user.getId().toString());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRole().name());
        userInfo.setProfileComplete(false);
        
        return new AuthResponse(token, "Registration successful. Please complete your profile.", userInfo);
    }
    
    public AuthResponse login(CandidateLoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }
        
        // Check if user is active
        if (!user.getIsActive()) {
            throw new RuntimeException("Account is deactivated");
        }
        
        // Check if user is a candidate
        if (user.getRole() != User.UserRole.CANDIDATE) {
            throw new RuntimeException("Invalid user role");
        }
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId().toString());
        
        // Check if profile exists
        Optional<Profile> profileOpt = profileRepository.findByUserId(user.getId());
        boolean profileComplete = profileOpt.isPresent();
        
        // Create response
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
        userInfo.setId(user.getId().toString());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRole().name());
        userInfo.setProfileComplete(profileComplete);
        
        String message = profileComplete 
                ? "Login successful" 
                : "Login successful. Please complete your profile.";
        
        return new AuthResponse(token, message, userInfo);
    }
    
    @Transactional
    public ApiResponse<Profile> createProfile(UUID userId, CandidateProfileRequest request) {
        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if profile already exists
        Optional<Profile> existingProfile = profileRepository.findByUserId(userId);
        
        Profile profile;
        if (existingProfile.isPresent()) {
            // Update existing profile
            profile = existingProfile.get();
        } else {
            // Create new profile
            profile = new Profile();
            profile.setUser(user);
            profile.setEmail(user.getEmail());
        }
        
        // Update profile fields (only if provided, allow partial updates)
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            profile.setFullName(request.getFullName());
        }
        profile.setEmail(user.getEmail()); // Always set email from user
        if (request.getMobile() != null) {
            profile.setMobile(request.getMobile().isEmpty() ? null : request.getMobile());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation().isEmpty() ? null : request.getLocation());
        }
        if (request.getLinkedin() != null) {
            profile.setLinkedin(request.getLinkedin().isEmpty() ? null : request.getLinkedin());
        }
        if (request.getProfilePicture() != null) {
            profile.setProfilePicture(request.getProfilePicture().isEmpty() ? null : request.getProfilePicture());
        }
        if (request.getResumeUrl() != null) {
            profile.setResumeUrl(request.getResumeUrl().isEmpty() ? null : request.getResumeUrl());
        }
        if (request.getExperienceLevel() != null) {
            profile.setExperienceLevel(request.getExperienceLevel().isEmpty() ? null : request.getExperienceLevel());
        }
        if (request.getPreferredRole() != null) {
            profile.setPreferredRole(request.getPreferredRole().isEmpty() ? null : request.getPreferredRole());
        }
        
        profile = profileRepository.save(profile);
        
        return ApiResponse.success(profile, "Profile created successfully");
    }
    
    
    // Helper method for profile access
    public Profile getProfileForUser(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
    }
    
    public Profile saveProfile(Profile profile) {
        return profileRepository.save(profile);
    }
}

