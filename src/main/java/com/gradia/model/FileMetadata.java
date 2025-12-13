package com.gradia.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = true)
    @JsonIgnore
    private Profile profile;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_profile_id", nullable = true)
    @JsonIgnore
    private com.gradia.model.EmployerProfile employerProfile;
    
    @Column(name = "file_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FileType fileType;
    
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;
    
    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize; // bytes
    
    @Column(name = "mime_type", length = 100)
    private String mimeType;
    
    @Column(name = "storage_provider", length = 50)
    private String storageProvider = "LOCAL";
    
    @Column(name = "storage_bucket", length = 255)
    private String storageBucket;
    
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    @Column(name = "upload_status", length = 50)
    @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus = UploadStatus.COMPLETED;
    
    @Column(name = "is_primary")
    private Boolean isPrimary = false; // For multiple resumes, mark one as primary
    
    @Lob
    @Column(name = "file_content", columnDefinition = "BYTEA", nullable = true)
    @JsonIgnore // Don't serialize file content in JSON responses
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARBINARY)
    private byte[] fileContent; // File content stored in PostgreSQL
    
    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
    
    @Column(name = "version")
    private Integer version = 1;
    
    public enum FileType {
        PROFILE_PICTURE,
        RESUME,
        COVER_LETTER,
        CERTIFICATE,
        PORTFOLIO,
        OTHER
    }
    
    public enum UploadStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}

