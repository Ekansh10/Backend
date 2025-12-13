package com.gradia.repository;

import com.gradia.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    // Candidate profile queries
    List<FileMetadata> findByProfileId(UUID profileId);
    List<FileMetadata> findByProfileIdAndFileType(UUID profileId, FileMetadata.FileType fileType);
    Optional<FileMetadata> findByProfileIdAndFileTypeAndIsPrimary(UUID profileId, FileMetadata.FileType fileType, Boolean isPrimary);
    long countByProfileIdAndFileType(UUID profileId, FileMetadata.FileType fileType);
    
    // Employer profile queries
    List<FileMetadata> findByEmployerProfileId(UUID employerProfileId);
    List<FileMetadata> findByEmployerProfileIdAndFileType(UUID employerProfileId, FileMetadata.FileType fileType);
    Optional<FileMetadata> findByEmployerProfileIdAndFileTypeAndIsPrimary(UUID employerProfileId, FileMetadata.FileType fileType, Boolean isPrimary);
    long countByEmployerProfileIdAndFileType(UUID employerProfileId, FileMetadata.FileType fileType);
    
    // Query to fetch file with profile and user for authorization check (candidate)
    @org.springframework.data.jpa.repository.Query("SELECT fm FROM FileMetadata fm JOIN FETCH fm.profile p JOIN FETCH p.user u WHERE fm.id = :fileId")
    java.util.Optional<FileMetadata> findByIdWithProfile(UUID fileId);
    
    // Query to fetch file with employer profile and user for authorization check
    @org.springframework.data.jpa.repository.Query("SELECT fm FROM FileMetadata fm JOIN FETCH fm.employerProfile ep JOIN FETCH ep.user u WHERE fm.id = :fileId")
    java.util.Optional<FileMetadata> findByIdWithEmployerProfile(UUID fileId);
}

