package com.gradia.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_profile_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private EmployerProfile employer; // References employer_profile table

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "department")
    private String department;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "experience_required")
    private String experienceRequired;

    @Column(name = "skills", columnDefinition = "TEXT[]")
    private List<String> skills;

    @Column(name = "job_type")
    private String jobType; // full-time, part-time, contract, internship, remote

    @Column(name = "location")
    private String location;

    @Column(name = "salary_range_min", precision = 10, scale = 2)
    private BigDecimal salaryRangeMin;

    @Column(name = "salary_range_max", precision = 10, scale = 2)
    private BigDecimal salaryRangeMax;

    @Column(name = "currency", length = 10)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private JobStatus status = JobStatus.DRAFT;

    @Column(name = "posted_date")
    private LocalDateTime postedDate;

    @Column(name = "closing_date")
    private LocalDateTime closingDate;

    @Column(name = "views_count")
    private Integer viewsCount = 0;

    @Column(name = "applications_count")
    private Integer applicationsCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Integer version = 1;

    public enum JobStatus {
        DRAFT,
        ACTIVE,
        CLOSED,
        PAUSED
    }
}

