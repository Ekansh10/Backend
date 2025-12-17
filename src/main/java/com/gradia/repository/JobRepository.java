package com.gradia.repository;

import com.gradia.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    
    // Find all jobs by employer profile ID
    List<Job> findByEmployerIdOrderByCreatedAtDesc(UUID employerProfileId);
    
    // Find jobs by status
    List<Job> findByStatusOrderByPostedDateDesc(Job.JobStatus status);
    
    // Find job by ID with employer
    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.employer WHERE j.id = :jobId")
    Optional<Job> findByIdWithEmployer(@Param("jobId") UUID jobId);
    
    // Count jobs by employer
    long countByEmployerId(UUID employerProfileId);
    
    // Count active jobs by employer
    long countByEmployerIdAndStatus(UUID employerProfileId, Job.JobStatus status);
    
    // Search jobs with filters
    // Note: Skills array search uses native PostgreSQL array functions via Hibernate
    // Using native query with explicit ORDER BY using database column names
    @Query(value = "SELECT DISTINCT j.* FROM jobs j " +
           "LEFT JOIN employer_profile e ON j.employer_profile_id = e.id " +
           "WHERE (:status IS NULL OR j.status = CAST(:status AS VARCHAR)) " +
           "AND (:query IS NULL OR " +
           "     LOWER(j.job_title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(j.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(j.location) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(e.company_name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     EXISTS (SELECT 1 FROM unnest(j.skills) AS skill WHERE LOWER(skill) LIKE LOWER(CONCAT('%', :query, '%')))) " +
           "AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "AND (:jobType IS NULL OR j.job_type = :jobType) " +
           "AND (:experienceRequired IS NULL OR j.experience_required = :experienceRequired) " +
           "AND (:minSalary IS NULL OR j.salary_range_max >= :minSalary) " +
           "AND (:maxSalary IS NULL OR j.salary_range_min <= :maxSalary) " +
           "AND (:currency IS NULL OR j.currency = :currency) " +
           "ORDER BY j.posted_date DESC NULLS LAST, j.created_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(DISTINCT j.id) FROM jobs j " +
           "LEFT JOIN employer_profile e ON j.employer_profile_id = e.id " +
           "WHERE (:status IS NULL OR j.status = CAST(:status AS VARCHAR)) " +
           "AND (:query IS NULL OR " +
           "     LOWER(j.job_title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(j.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(j.location) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(e.company_name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     EXISTS (SELECT 1 FROM unnest(j.skills) AS skill WHERE LOWER(skill) LIKE LOWER(CONCAT('%', :query, '%')))) " +
           "AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "AND (:jobType IS NULL OR j.job_type = :jobType) " +
           "AND (:experienceRequired IS NULL OR j.experience_required = :experienceRequired) " +
           "AND (:minSalary IS NULL OR j.salary_range_max >= :minSalary) " +
           "AND (:maxSalary IS NULL OR j.salary_range_min <= :maxSalary) " +
           "AND (:currency IS NULL OR j.currency = :currency)")
    Page<Job> searchJobs(
        @Param("query") String query,
        @Param("location") String location,
        @Param("jobType") String jobType,
        @Param("experienceRequired") String experienceRequired,
        @Param("minSalary") BigDecimal minSalary,
        @Param("maxSalary") BigDecimal maxSalary,
        @Param("currency") String currency,
        @Param("status") String status,
        Pageable pageable
    );
    
    // Search jobs with skills filter
    @Query(value = "SELECT DISTINCT j.* FROM jobs j " +
           "LEFT JOIN employer_profile e ON j.employer_profile_id = e.id " +
           "WHERE (:status IS NULL OR j.status = CAST(:status AS VARCHAR)) " +
           "AND (:query IS NULL OR " +
           "     LOWER(j.job_title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(j.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(j.location) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(e.company_name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     EXISTS (SELECT 1 FROM unnest(j.skills) AS skill WHERE LOWER(skill) LIKE LOWER(CONCAT('%', :query, '%')))) " +
           "AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "AND (:jobType IS NULL OR j.job_type = :jobType) " +
           "AND (:experienceRequired IS NULL OR j.experience_required = :experienceRequired) " +
           "AND (:minSalary IS NULL OR j.salary_range_max >= :minSalary) " +
           "AND (:maxSalary IS NULL OR j.salary_range_min <= :maxSalary) " +
           "AND (:currency IS NULL OR j.currency = :currency) " +
           "AND (:skill IS NULL OR EXISTS (SELECT 1 FROM unnest(j.skills) AS skill_item WHERE LOWER(skill_item) = LOWER(:skill))) " +
           "ORDER BY j.posted_date DESC NULLS LAST, j.created_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(DISTINCT j.id) FROM jobs j " +
           "LEFT JOIN employer_profile e ON j.employer_profile_id = e.id " +
           "WHERE (:status IS NULL OR j.status = CAST(:status AS VARCHAR)) " +
           "AND (:query IS NULL OR " +
           "     LOWER(j.job_title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(j.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(j.location) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(e.company_name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     EXISTS (SELECT 1 FROM unnest(j.skills) AS skill WHERE LOWER(skill) LIKE LOWER(CONCAT('%', :query, '%')))) " +
           "AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "AND (:jobType IS NULL OR j.job_type = :jobType) " +
           "AND (:experienceRequired IS NULL OR j.experience_required = :experienceRequired) " +
           "AND (:minSalary IS NULL OR j.salary_range_max >= :minSalary) " +
           "AND (:maxSalary IS NULL OR j.salary_range_min <= :maxSalary) " +
           "AND (:currency IS NULL OR j.currency = :currency) " +
           "AND (:skill IS NULL OR EXISTS (SELECT 1 FROM unnest(j.skills) AS skill_item WHERE LOWER(skill_item) = LOWER(:skill)))")
    Page<Job> searchJobsWithSkill(
        @Param("query") String query,
        @Param("location") String location,
        @Param("jobType") String jobType,
        @Param("experienceRequired") String experienceRequired,
        @Param("minSalary") BigDecimal minSalary,
        @Param("maxSalary") BigDecimal maxSalary,
        @Param("currency") String currency,
        @Param("skill") String skill,
        @Param("status") String status,
        Pageable pageable
    );
}

