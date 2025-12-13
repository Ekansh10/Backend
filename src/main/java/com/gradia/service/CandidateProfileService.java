package com.gradia.service;

import com.gradia.dto.*;
import com.gradia.model.Profile;
import com.gradia.model.ProfileMetadata;
import com.gradia.model.User;
import com.gradia.repository.ProfileRepository;
import com.gradia.repository.ProfileMetadataRepository;
import com.gradia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CandidateProfileService {
    
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ProfileMetadataRepository profileMetadataRepository;
    private final ProfileCompletenessService completenessService;
    
    @Transactional
    public ApiResponse<Profile> updateProfile(UUID userId, ProfileUpdateRequest request) {
        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get or create profile
        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Profile newProfile = new Profile();
                    newProfile.setUser(user);
                    newProfile.setEmail(user.getEmail());
                    return newProfile;
                });
        
        // Update basic profile fields (allow partial updates and deletions)
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
        if (request.getExperienceLevel() != null) {
            profile.setExperienceLevel(request.getExperienceLevel().isEmpty() ? null : request.getExperienceLevel());
        }
        if (request.getPreferredRole() != null) {
            profile.setPreferredRole(request.getPreferredRole().isEmpty() ? null : request.getPreferredRole());
        }
        
        Profile savedProfile = profileRepository.save(profile);
        
        // Update or create profile metadata
        // With @MapsId, we only need to set the profile and JPA will handle the ID
        ProfileMetadata metadata = profileMetadataRepository.findByProfileId(savedProfile.getId())
                .orElseGet(() -> {
                    ProfileMetadata newMetadata = new ProfileMetadata();
                    newMetadata.setProfile(savedProfile); // @MapsId will set the ID from profile
                    return newMetadata;
                });
        
        // Only update fields that are provided (allow partial updates and deletions)
        if (request.getBio() != null) {
            metadata.setBio(request.getBio().isEmpty() ? null : request.getBio());
        }
        if (request.getSkills() != null) {
            metadata.setSkills(request.getSkills().length == 0 ? null : request.getSkills());
        }
        if (request.getCertifications() != null) {
            metadata.setCertifications(request.getCertifications().length == 0 ? null : request.getCertifications());
        }
        if (request.getLanguages() != null) {
            metadata.setLanguages(request.getLanguages().length == 0 ? null : request.getLanguages());
        }
        if (request.getAvailabilityStatus() != null) {
            metadata.setAvailabilityStatus(request.getAvailabilityStatus().isEmpty() ? null : request.getAvailabilityStatus());
        }
        if (request.getSalaryExpectationMin() != null) {
            metadata.setSalaryExpectationMin(request.getSalaryExpectationMin());
        }
        if (request.getSalaryExpectationMax() != null) {
            metadata.setSalaryExpectationMax(request.getSalaryExpectationMax());
        }
        if (request.getWorkPreference() != null) {
            metadata.setWorkPreference(request.getWorkPreference().length == 0 ? null : request.getWorkPreference());
        }
        if (request.getNoticePeriod() != null) {
            metadata.setNoticePeriod(request.getNoticePeriod());
        }
        
        profileMetadataRepository.save(metadata);
        
        return ApiResponse.success(savedProfile, "Profile updated successfully");
    }
    
    // Helper methods for file operations
    public Profile getProfileForUser(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
    }
    
    @Transactional
    public Profile getProfileForUserOrCreate(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Profile newProfile = new Profile();
                    newProfile.setUser(user);
                    newProfile.setEmail(user.getEmail());
                    return profileRepository.save(newProfile);
                });
    }
    
    public Profile saveProfile(Profile profile) {
        return profileRepository.save(profile);
    }
    
    // Public method - get profile by ID
    public ApiResponse<Profile> getProfileById(UUID profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return ApiResponse.success(profile, "Profile retrieved successfully");
    }
    
    public ApiResponse<ProfileCompletenessResponse> getProfileCompleteness(UUID userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        
        ProfileCompletenessResponse completeness = completenessService.calculateCompleteness(profile);
        
        return ApiResponse.success(completeness, "Profile completeness calculated");
    }
    
    public ApiResponse<ProfileWithMetadata> getFullProfile(UUID userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        
        ProfileMetadata metadata = profileMetadataRepository.findByProfileId(profile.getId())
                .orElse(null);
        
        ProfileWithMetadata fullProfile = new ProfileWithMetadata(profile, metadata);
        
        return ApiResponse.success(fullProfile, "Profile retrieved successfully");
    }
    
    // Inner class for response
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ProfileWithMetadata {
        private Profile profile;
        private ProfileMetadata metadata;
    }
}

