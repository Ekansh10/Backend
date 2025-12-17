package com.gradia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobCreateRequest {
    @NotBlank(message = "Job title is required")
    @Size(max = 255, message = "Job title must not exceed 255 characters")
    private String jobTitle;

    private String department;

    @NotBlank(message = "Job description is required")
    @Size(min = 50, message = "Description must be at least 50 characters")
    private String description;

    @NotBlank(message = "Requirements are required")
    @Size(min = 20, message = "Requirements must be at least 20 characters")
    private String requirements;

    @NotBlank(message = "Experience required is required")
    private String experienceRequired;

    @NotNull(message = "Skills are required")
    @Size(min = 1, message = "At least one skill is required")
    private List<String> skills;

    @NotBlank(message = "Job type is required")
    private String jobType; // full-time, part-time, contract, internship, remote

    @NotBlank(message = "Location is required")
    private String location;

    private BigDecimal salaryRangeMin;
    private BigDecimal salaryRangeMax;
    private String currency = "USD";

    private LocalDateTime closingDate;
}

