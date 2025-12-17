package com.gradia.service;

import com.gradia.dto.*;
import com.gradia.model.EmployerProfile;
import com.gradia.model.Job;
import com.gradia.model.User;
import com.gradia.repository.EmployerProfileRepository;
import com.gradia.repository.JobRepository;
import com.gradia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {
    
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final EmployerProfileRepository employerProfileRepository;
    
    @Transactional
    public ApiResponse<JobResponse> createJob(UUID userId, JobCreateRequest request) {
        // Get user and verify role
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() != User.UserRole.EMPLOYER) {
            throw new RuntimeException("Only employers can create jobs");
        }
        
        // Get employer profile - jobs table now references employer_profile(id)
        EmployerProfile employerProfile = employerProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Employer profile not found. Please complete your company profile first."));
        
        // Verify company profile is complete
        if (employerProfile.getCompanyName() == null || employerProfile.getCompanyName().isEmpty()) {
            throw new RuntimeException("Company profile is incomplete. Please complete your company profile first.");
        }
        
        // Create job
        Job job = new Job();
        job.setEmployer(employerProfile);
        job.setJobTitle(request.getJobTitle());
        job.setDepartment(request.getDepartment());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setExperienceRequired(request.getExperienceRequired());
        job.setSkills(request.getSkills());
        job.setJobType(request.getJobType());
        job.setLocation(request.getLocation());
        job.setSalaryRangeMin(request.getSalaryRangeMin());
        job.setSalaryRangeMax(request.getSalaryRangeMax());
        job.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        job.setStatus(Job.JobStatus.DRAFT);
        job.setClosingDate(request.getClosingDate());
        job.setViewsCount(0);
        job.setApplicationsCount(0);
        
        job = jobRepository.save(job);
        
        return ApiResponse.success(toJobResponse(job), "Job created successfully");
    }
    
    @Transactional
    public ApiResponse<JobResponse> updateJob(UUID userId, UUID jobId, JobUpdateRequest request) {
        // Get user and verify role
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() != User.UserRole.EMPLOYER) {
            throw new RuntimeException("Only employers can update jobs");
        }
        
        // Get job with employer
        Job job = jobRepository.findByIdWithEmployer(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));
        
        // Verify ownership
        if (!job.getEmployer().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to update this job");
        }
        
        // Update fields (only non-null fields)
        if (request.getJobTitle() != null) {
            job.setJobTitle(request.getJobTitle());
        }
        if (request.getDepartment() != null) {
            job.setDepartment(request.getDepartment());
        }
        if (request.getDescription() != null) {
            job.setDescription(request.getDescription());
        }
        if (request.getRequirements() != null) {
            job.setRequirements(request.getRequirements());
        }
        if (request.getExperienceRequired() != null) {
            job.setExperienceRequired(request.getExperienceRequired());
        }
        if (request.getSkills() != null) {
            job.setSkills(request.getSkills());
        }
        if (request.getJobType() != null) {
            job.setJobType(request.getJobType());
        }
        if (request.getLocation() != null) {
            job.setLocation(request.getLocation());
        }
        if (request.getSalaryRangeMin() != null) {
            job.setSalaryRangeMin(request.getSalaryRangeMin());
        }
        if (request.getSalaryRangeMax() != null) {
            job.setSalaryRangeMax(request.getSalaryRangeMax());
        }
        if (request.getCurrency() != null) {
            job.setCurrency(request.getCurrency());
        }
        if (request.getClosingDate() != null) {
            job.setClosingDate(request.getClosingDate());
        }
        
        job = jobRepository.save(job);
        
        return ApiResponse.success(toJobResponse(job), "Job updated successfully");
    }
    
    @Transactional
    public ApiResponse<JobResponse> publishJob(UUID userId, UUID jobId) {
        // Get user and verify role
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() != User.UserRole.EMPLOYER) {
            throw new RuntimeException("Only employers can publish jobs");
        }
        
        // Get job with employer
        Job job = jobRepository.findByIdWithEmployer(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));
        
        // Verify ownership
        if (!job.getEmployer().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to publish this job");
        }
        
        // Publish job
        job.setStatus(Job.JobStatus.ACTIVE);
        if (job.getPostedDate() == null) {
            job.setPostedDate(LocalDateTime.now());
        }
        
        job = jobRepository.save(job);
        
        return ApiResponse.success(toJobResponse(job), "Job published successfully");
    }
    
    @Transactional
    public ApiResponse<JobResponse> unpublishJob(UUID userId, UUID jobId) {
        // Get user and verify role
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() != User.UserRole.EMPLOYER) {
            throw new RuntimeException("Only employers can unpublish jobs");
        }
        
        // Get job with employer
        Job job = jobRepository.findByIdWithEmployer(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));
        
        // Verify ownership
        if (!job.getEmployer().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to unpublish this job");
        }
        
        // Unpublish job (set to DRAFT or PAUSED)
        if (job.getStatus() == Job.JobStatus.ACTIVE) {
            job.setStatus(Job.JobStatus.PAUSED);
        }
        
        job = jobRepository.save(job);
        
        return ApiResponse.success(toJobResponse(job), "Job unpublished successfully");
    }
    
    public ApiResponse<List<JobResponse>> getEmployerJobs(UUID userId) {
        // Get user and verify role
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() != User.UserRole.EMPLOYER) {
            throw new RuntimeException("Only employers can view their jobs");
        }
        
        // Get employer profile
        EmployerProfile employerProfile = employerProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Employer profile not found"));
        
        // Get all jobs for this employer
        List<Job> jobs = jobRepository.findByEmployerIdOrderByCreatedAtDesc(employerProfile.getId());
        
        List<JobResponse> responses = jobs.stream()
            .map(this::toJobResponse)
            .collect(Collectors.toList());
        
        return ApiResponse.success(responses, "Jobs retrieved successfully");
    }
    
    public ApiResponse<JobResponse> getJobById(UUID jobId) {
        Job job = jobRepository.findByIdWithEmployer(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));
        
        return ApiResponse.success(toJobResponse(job), "Job retrieved successfully");
    }
    
    @Transactional
    public ApiResponse<Object> deleteJob(UUID userId, UUID jobId) {
        // Get user and verify role
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() != User.UserRole.EMPLOYER) {
            throw new RuntimeException("Only employers can delete jobs");
        }
        
        // Get job with employer
        Job job = jobRepository.findByIdWithEmployer(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));
        
        // Verify ownership
        if (!job.getEmployer().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this job");
        }
        
        jobRepository.delete(job);
        
        return ApiResponse.success(null, "Job deleted successfully");
    }
    
    public ApiResponse<JobSearchResponse> searchJobs(JobSearchRequest request) {
        // Parse status as String for native query
        String status = null;
        if (request.getStatus() != null && !request.getStatus().equalsIgnoreCase("ALL")) {
            try {
                // Validate status
                Job.JobStatus.valueOf(request.getStatus().toUpperCase());
                status = request.getStatus().toUpperCase();
            } catch (IllegalArgumentException e) {
                status = "ACTIVE"; // Default to ACTIVE
            }
        }
        
        // Default to ACTIVE if status is null
        if (status == null) {
            status = "ACTIVE";
        }
        
        // For native queries, we handle sorting in the SQL query itself
        // So we create a simple pageable without sort (sort is in SQL)
        Pageable pageable = PageRequest.of(
            request.getPage() != null ? request.getPage() : 0,
            request.getSize() != null ? request.getSize() : 20
        );
        
        // Search jobs
        Page<Job> jobPage;
        
        // If skills filter is provided, search with skills
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            // For multiple skills, we'll search for jobs that have at least one matching skill
            // This is a simplified approach - for exact match of all skills, we'd need a more complex query
            String firstSkill = request.getSkills().get(0);
            jobPage = jobRepository.searchJobsWithSkill(
                request.getQuery(),
                request.getLocation(),
                request.getJobType(),
                request.getExperienceRequired(),
                request.getMinSalary(),
                request.getMaxSalary(),
                request.getCurrency(),
                firstSkill,
                status,
                pageable
            );
        } else {
            jobPage = jobRepository.searchJobs(
                request.getQuery(),
                request.getLocation(),
                request.getJobType(),
                request.getExperienceRequired(),
                request.getMinSalary(),
                request.getMaxSalary(),
                request.getCurrency(),
                status,
                pageable
            );
        }
        
        // Convert to response
        List<JobResponse> jobResponses = jobPage.getContent().stream()
            .map(this::toJobResponse)
            .collect(Collectors.toList());
        
        JobSearchResponse searchResponse = JobSearchResponse.builder()
            .jobs(jobResponses)
            .totalElements(jobPage.getTotalElements())
            .totalPages(jobPage.getTotalPages())
            .currentPage(jobPage.getNumber())
            .pageSize(jobPage.getSize())
            .hasNext(jobPage.hasNext())
            .hasPrevious(jobPage.hasPrevious())
            .build();
        
        return ApiResponse.success(searchResponse, "Jobs retrieved successfully");
    }
    
    public ApiResponse<List<JobResponse>> getAllActiveJobs() {
        List<Job> jobs = jobRepository.findByStatusOrderByPostedDateDesc(Job.JobStatus.ACTIVE);
        
        List<JobResponse> responses = jobs.stream()
            .map(this::toJobResponse)
            .collect(Collectors.toList());
        
        return ApiResponse.success(responses, "Active jobs retrieved successfully");
    }
    
    private JobResponse toJobResponse(Job job) {
        return JobResponse.builder()
            .id(job.getId())
            .employerId(job.getEmployer().getId())
            .employerName(job.getEmployer().getCompanyName())
            .jobTitle(job.getJobTitle())
            .department(job.getDepartment())
            .description(job.getDescription())
            .requirements(job.getRequirements())
            .experienceRequired(job.getExperienceRequired())
            .skills(job.getSkills())
            .jobType(job.getJobType())
            .location(job.getLocation())
            .salaryRangeMin(job.getSalaryRangeMin())
            .salaryRangeMax(job.getSalaryRangeMax())
            .currency(job.getCurrency())
            .status(job.getStatus())
            .postedDate(job.getPostedDate())
            .closingDate(job.getClosingDate())
            .viewsCount(job.getViewsCount())
            .applicationsCount(job.getApplicationsCount())
            .createdAt(job.getCreatedAt())
            .updatedAt(job.getUpdatedAt())
            .build();
    }
}

