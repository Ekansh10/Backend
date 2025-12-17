package com.gradia.controller;

import com.gradia.dto.*;
import com.gradia.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class JobSearchController {
    
    private final JobService jobService;
    
    // Public endpoint - anyone can browse/search jobs
    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<ApiResponse<JobSearchResponse>> searchJobs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String experienceRequired,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) Double minSalary,
            @RequestParam(required = false) Double maxSalary,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false, defaultValue = "postedDate") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortOrder) {
        try {
            JobSearchRequest request = new JobSearchRequest();
            request.setQuery(query);
            request.setLocation(location);
            request.setJobType(jobType);
            request.setExperienceRequired(experienceRequired);
            request.setSkills(skills);
            if (minSalary != null) {
                request.setMinSalary(java.math.BigDecimal.valueOf(minSalary));
            }
            if (maxSalary != null) {
                request.setMaxSalary(java.math.BigDecimal.valueOf(maxSalary));
            }
            request.setCurrency(currency);
            request.setStatus(status);
            request.setPage(page);
            request.setSize(size);
            request.setSortBy(sortBy);
            request.setSortOrder(sortOrder);
            
            ApiResponse<JobSearchResponse> response = jobService.searchJobs(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // Public endpoint - get all active jobs (browse)
    @PreAuthorize("permitAll()")
    @GetMapping("/browse")
    public ResponseEntity<ApiResponse<List<JobResponse>>> browseAllJobs() {
        try {
            ApiResponse<List<JobResponse>> response = jobService.getAllActiveJobs();
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

