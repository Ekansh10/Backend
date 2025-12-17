package com.gradia.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobUpdateRequest {
    @Size(max = 255, message = "Job title must not exceed 255 characters")
    private String jobTitle;

    private String department;

    @Size(min = 50, message = "Description must be at least 50 characters")
    private String description;

    @Size(min = 20, message = "Requirements must be at least 20 characters")
    private String requirements;

    private String experienceRequired;

    private List<String> skills;

    private String jobType; // full-time, part-time, contract, internship, remote

    private String location;

    private BigDecimal salaryRangeMin;
    private BigDecimal salaryRangeMax;
    private String currency;

    private LocalDateTime closingDate;
}

