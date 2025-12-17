package com.gradia.dto;

import com.gradia.model.Job;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private UUID id;
    private UUID employerId;
    private String employerName;
    private String jobTitle;
    private String department;
    private String description;
    private String requirements;
    private String experienceRequired;
    private List<String> skills;
    private String jobType;
    private String location;
    private BigDecimal salaryRangeMin;
    private BigDecimal salaryRangeMax;
    private String currency;
    private Job.JobStatus status;
    private LocalDateTime postedDate;
    private LocalDateTime closingDate;
    private Integer viewsCount;
    private Integer applicationsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

