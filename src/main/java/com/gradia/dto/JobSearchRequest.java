package com.gradia.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class JobSearchRequest {
    // Search text - searches in title, description, company name, skills
    private String query;
    
    // Location filter
    private String location;
    
    // Job type filter (full-time, part-time, contract, internship, remote)
    private String jobType;
    
    // Experience level filter
    private String experienceRequired;
    
    // Skills filter (array of skills)
    private List<String> skills;
    
    // Salary range filters
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String currency;
    
    // Status filter (default: ACTIVE only)
    private String status = "ACTIVE"; // ACTIVE, DRAFT, PAUSED, CLOSED, or ALL
    
    // Pagination
    private Integer page = 0;
    private Integer size = 20;
    
    // Sorting
    private String sortBy = "postedDate"; // postedDate, relevance, salary
    private String sortOrder = "DESC"; // ASC, DESC
}

