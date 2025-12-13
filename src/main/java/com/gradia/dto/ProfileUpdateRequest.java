package com.gradia.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfileUpdateRequest {
    // All fields are optional to allow partial updates and deletions
    private String fullName;
    
    private String mobile;
    private String location;
    private String linkedin;
    private String experienceLevel;
    private String preferredRole;
    
    // Profile metadata fields
    private String bio;
    private String[] skills;
    private String[] certifications;
    private String[] languages;
    private String availabilityStatus;
    private BigDecimal salaryExpectationMin;
    private BigDecimal salaryExpectationMax;
    private String[] workPreference; // remote, hybrid, on-site
    private Integer noticePeriod;
}

