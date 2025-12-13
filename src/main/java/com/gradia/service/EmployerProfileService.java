package com.gradia.service;

import com.gradia.dto.*;
import com.gradia.model.EmployerProfile;
import com.gradia.model.User;
import com.gradia.repository.EmployerProfileRepository;
import com.gradia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployerProfileService {
    
    private final UserRepository userRepository;
    private final EmployerProfileRepository employerProfileRepository;
    
    @Transactional
    public ApiResponse<EmployerProfile> createOrUpdateProfile(UUID userId, EmployerProfileRequest request) {
        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify user is an employer
        if (user.getRole() != User.UserRole.EMPLOYER) {
            throw new RuntimeException("Only employers can create company profiles");
        }
        
        // Get or create profile
        EmployerProfile profile = employerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    EmployerProfile newProfile = new EmployerProfile();
                    newProfile.setUser(user);
                    newProfile.setEmail(user.getEmail());
                    return newProfile;
                });
        
        // Update profile fields (allow partial updates)
        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());
        }
        if (request.getMobile() != null) {
            profile.setMobile(request.getMobile().isEmpty() ? null : request.getMobile());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation().isEmpty() ? null : request.getLocation());
        }
        if (request.getLinkedin() != null) {
            profile.setLinkedin(request.getLinkedin().isEmpty() ? null : request.getLinkedin());
        }
        if (request.getCompanyName() != null) {
            profile.setCompanyName(request.getCompanyName().isEmpty() ? null : request.getCompanyName());
        }
        if (request.getCompanyDescription() != null) {
            profile.setCompanyDescription(request.getCompanyDescription().isEmpty() ? null : request.getCompanyDescription());
        }
        if (request.getWebsite() != null) {
            profile.setWebsite(request.getWebsite().isEmpty() ? null : request.getWebsite());
        }
        
        EmployerProfile savedProfile = employerProfileRepository.save(profile);
        
        return ApiResponse.success(savedProfile, "Company profile created successfully");
    }
    
    
    public boolean isProfileComplete(UUID userId) {
        return employerProfileRepository.findByUserId(userId)
                .map(profile -> profile.getCompanyName() != null && 
                               !profile.getCompanyName().trim().isEmpty() &&
                               profile.getFullName() != null &&
                               !profile.getFullName().trim().isEmpty() &&
                               profile.getMobile() != null &&
                               !profile.getMobile().trim().isEmpty() &&
                               profile.getWebsite() != null &&
                               !profile.getWebsite().trim().isEmpty())
                .orElse(false);
    }
    
    // Helper methods for file operations
    public EmployerProfile getProfileForUserOrCreate(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return employerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    EmployerProfile newProfile = new EmployerProfile();
                    newProfile.setUser(user);
                    newProfile.setEmail(user.getEmail());
                    return employerProfileRepository.save(newProfile);
                });
    }
    
    public EmployerProfile saveProfile(EmployerProfile profile) {
        return employerProfileRepository.save(profile);
    }
    
    // Public method - get profile by ID
    public ApiResponse<EmployerProfile> getProfileById(UUID profileId) {
        EmployerProfile profile = employerProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return ApiResponse.success(profile, "Profile retrieved successfully");
    }
}
