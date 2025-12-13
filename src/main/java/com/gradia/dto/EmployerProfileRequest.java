package com.gradia.dto;

import lombok.Data;

@Data
public class EmployerProfileRequest {
    // Contact person details
    private String fullName; // Contact Person Name - required
    
    private String mobile; // Required
    
    // Company details
    private String companyName; // Required
    private String companyDescription; // Optional
    private String website; // Required
    
    // Optional fields
    private String location;
    private String linkedin; // LinkedIn Company Page
}

