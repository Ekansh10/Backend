package com.gradia.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CandidateProfileRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    private String mobile;
    private String location;
    private String linkedin;
    private String profilePicture;
    private String resumeUrl;
    private String experienceLevel;
    private String preferredRole;
}

