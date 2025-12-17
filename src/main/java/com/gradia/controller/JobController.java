package com.gradia.controller;

import com.gradia.dto.*;
import com.gradia.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employers/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYER')")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class JobController {
    
    private final JobService jobService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody JobCreateRequest request,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            ApiResponse<JobResponse> response = jobService.createJob(userId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable UUID jobId,
            @Valid @RequestBody JobUpdateRequest request,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            ApiResponse<JobResponse> response = jobService.updateJob(userId, jobId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<JobResponse>>> getEmployerJobs(
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            ApiResponse<List<JobResponse>> response = jobService.getEmployerJobs(userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // Public endpoint - anyone can view a job by ID (for candidates browsing jobs)
    @PreAuthorize("permitAll()")
    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobResponse>> getJobById(
            @PathVariable UUID jobId) {
        try {
            ApiResponse<JobResponse> response = jobService.getJobById(jobId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{jobId}/publish")
    public ResponseEntity<ApiResponse<JobResponse>> publishJob(
            @PathVariable UUID jobId,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            ApiResponse<JobResponse> response = jobService.publishJob(userId, jobId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{jobId}/unpublish")
    public ResponseEntity<ApiResponse<JobResponse>> unpublishJob(
            @PathVariable UUID jobId,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            ApiResponse<JobResponse> response = jobService.unpublishJob(userId, jobId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Object>> deleteJob(
            @PathVariable UUID jobId,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            ApiResponse<Object> response = jobService.deleteJob(userId, jobId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

