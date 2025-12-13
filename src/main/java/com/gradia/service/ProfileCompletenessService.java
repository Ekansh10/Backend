package com.gradia.service;

import com.gradia.dto.ProfileCompletenessResponse;
import com.gradia.model.Profile;
import com.gradia.model.ProfileMetadata;
import com.gradia.model.FileMetadata;
import com.gradia.repository.ProfileMetadataRepository;
import com.gradia.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileCompletenessService {
    
    private final ProfileMetadataRepository profileMetadataRepository;
    private final FileMetadataRepository fileMetadataRepository;
    
    // Define point weights for each field category
    private static final int BASIC_FIELDS_TOTAL = 40;  // 8 fields × 5 points each
    private static final int METADATA_FIELDS_TOTAL = 30; // 5 fields × 6 points each
    private static final int RESUME_POINTS = 30;
    private static final int MAX_SCORE = 100; // BASIC_FIELDS_TOTAL + METADATA_FIELDS_TOTAL + RESUME_POINTS
    
    public ProfileCompletenessResponse calculateCompleteness(Profile profile) {
        List<String> completedFields = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();
        int score = 0;
        
        // Basic profile fields (40 points total - 8 fields × 5 points each)
        if (isFieldFilled(profile.getFullName())) {
            completedFields.add("fullName");
            score += 5;
        } else {
            missingFields.add("fullName");
        }
        
        if (isFieldFilled(profile.getEmail())) {
            completedFields.add("email");
            score += 5;
        } else {
            missingFields.add("email");
        }
        
        if (isFieldFilled(profile.getMobile())) {
            completedFields.add("mobile");
            score += 5;
        } else {
            missingFields.add("mobile");
        }
        
        if (isFieldFilled(profile.getLocation())) {
            completedFields.add("location");
            score += 5;
        } else {
            missingFields.add("location");
        }
        
        if (isFieldFilled(profile.getLinkedin())) {
            completedFields.add("linkedin");
            score += 5;
        } else {
            missingFields.add("linkedin");
        }
        
        if (isFieldFilled(profile.getExperienceLevel())) {
            completedFields.add("experienceLevel");
            score += 5;
        } else {
            missingFields.add("experienceLevel");
        }
        
        if (isFieldFilled(profile.getPreferredRole())) {
            completedFields.add("preferredRole");
            score += 5;
        } else {
            missingFields.add("preferredRole");
        }
        
        // Check profile picture - can be from profile field or file_metadata
        boolean hasProfilePicture = isFieldFilled(profile.getProfilePicture());
        if (!hasProfilePicture) {
            // Check if there's a profile picture file uploaded
            List<FileMetadata> pictures = fileMetadataRepository
                .findByProfileIdAndFileType(profile.getId(), FileMetadata.FileType.PROFILE_PICTURE);
            hasProfilePicture = !pictures.isEmpty();
        }
        
        if (hasProfilePicture) {
            completedFields.add("profilePicture");
            score += 5;
        } else {
            missingFields.add("profilePicture");
        }
        
        // Profile metadata fields (30 points total - 5 fields × 6 points each)
        Optional<ProfileMetadata> metadataOpt = profileMetadataRepository.findByProfileId(profile.getId());
        if (metadataOpt.isPresent()) {
            ProfileMetadata metadata = metadataOpt.get();
            
            if (isFieldFilled(metadata.getBio())) {
                completedFields.add("bio");
                score += 6;
            } else {
                missingFields.add("bio");
            }
            
            if (hasArrayContent(metadata.getSkills())) {
                completedFields.add("skills");
                score += 6;
            } else {
                missingFields.add("skills");
            }
            
            if (hasArrayContent(metadata.getLanguages())) {
                completedFields.add("languages");
                score += 6;
            } else {
                missingFields.add("languages");
            }
            
            if (hasArrayContent(metadata.getWorkPreference())) {
                completedFields.add("workPreference");
                score += 6;
            } else {
                missingFields.add("workPreference");
            }
            
            if (isFieldFilled(metadata.getAvailabilityStatus())) {
                completedFields.add("availabilityStatus");
                score += 6;
            } else {
                missingFields.add("availabilityStatus");
            }
        } else {
            // No metadata exists - all metadata fields are missing
            missingFields.add("bio");
            missingFields.add("skills");
            missingFields.add("languages");
            missingFields.add("workPreference");
            missingFields.add("availabilityStatus");
        }
        
        // Resume upload (30 points)
        List<FileMetadata> resumes = fileMetadataRepository
            .findByProfileIdAndFileType(profile.getId(), FileMetadata.FileType.RESUME);
        if (!resumes.isEmpty()) {
            completedFields.add("resume");
            score += 30;
        } else {
            missingFields.add("resume");
        }
        
        // Ensure score is between 0 and 100
        if (score > MAX_SCORE) {
            score = MAX_SCORE;
        }
        if (score < 0) {
            score = 0;
        }
        
        // Calculate percentage (score is already out of 100, so it's the percentage)
        int percentage = score;
        
        // Determine level
        String level;
        if (percentage >= 90) {
            level = "COMPLETE";
        } else if (percentage >= 70) {
            level = "GOOD";
        } else if (percentage >= 50) {
            level = "BASIC";
        } else {
            level = "INCOMPLETE";
        }
        
        return ProfileCompletenessResponse.of(percentage, level, missingFields, completedFields);
    }
    
    private boolean isFieldFilled(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    private boolean hasArrayContent(String[] array) {
        return array != null && array.length > 0;
    }
}
