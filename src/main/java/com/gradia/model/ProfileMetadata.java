package com.gradia.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "profile_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileMetadata {
    @Id
    @Column(name = "profile_id")
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "profile_id", nullable = false)
    @JsonIgnore
    private Profile profile;
    
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;
    
    @Column(name = "skills", columnDefinition = "TEXT[]")
    private String[] skills;
    
    @Column(name = "certifications", columnDefinition = "TEXT[]")
    private String[] certifications;
    
    @Column(name = "languages", columnDefinition = "TEXT[]")
    private String[] languages;
    
    @Column(name = "availability_status", length = 50)
    private String availabilityStatus;
    
    @Column(name = "salary_expectation_min")
    private java.math.BigDecimal salaryExpectationMin;
    
    @Column(name = "salary_expectation_max")
    private java.math.BigDecimal salaryExpectationMax;
    
    @Column(name = "work_preference", columnDefinition = "TEXT[]")
    private String[] workPreference; // remote, hybrid, on-site
    
    @Column(name = "notice_period")
    private Integer noticePeriod; // days
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

