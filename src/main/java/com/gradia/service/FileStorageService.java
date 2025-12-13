package com.gradia.service;

import com.gradia.model.EmployerProfile;
import com.gradia.model.FileMetadata;
import com.gradia.model.Profile;
import com.gradia.repository.EmployerProfileRepository;
import com.gradia.repository.FileMetadataRepository;
import com.gradia.repository.ProfileRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    
    private final FileMetadataRepository fileMetadataRepository;
    private final ProfileRepository profileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final EntityManager entityManager;
    
    // Upload file for candidate profile
    @Transactional
    public FileMetadata uploadFile(Profile profile, MultipartFile file, 
                                   FileMetadata.FileType fileType, 
                                   boolean isPrimary) throws IOException {
        return uploadFileInternal(profile.getId(), null, file, fileType, isPrimary, profile.getId().toString());
    }
    
    // Upload file for employer profile
    @Transactional
    public FileMetadata uploadFile(EmployerProfile employerProfile, MultipartFile file, 
                                   FileMetadata.FileType fileType, 
                                   boolean isPrimary) throws IOException {
        return uploadFileInternal(null, employerProfile.getId(), file, fileType, isPrimary, employerProfile.getId().toString());
    }
    
    // Internal method to handle file upload for both profile types
    @Transactional
    private FileMetadata uploadFileInternal(UUID profileId, UUID employerProfileId, MultipartFile file, 
                                           FileMetadata.FileType fileType, 
                                           boolean isPrimary, String profileIdString) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        
        // Validate file type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("File name is null");
        }
        
        String extension = getFileExtension(originalFilename);
        if (!isValidFileType(fileType, extension)) {
            throw new RuntimeException("Invalid file type for " + fileType);
        }
        
        // Validate file size (max 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new RuntimeException("File size exceeds maximum limit of 10MB");
        }
        
        // Read file content into byte array
        byte[] fileContent = file.getBytes();
        
        // If this is primary, FIRST unset existing primary files BEFORE creating new one
        // This prevents unique constraint violation on idx_file_metadata_unique_primary
        if (isPrimary) {
            List<FileMetadata> existingPrimaryFiles;
            if (profileId != null) {
                existingPrimaryFiles = fileMetadataRepository
                    .findByProfileIdAndFileType(profileId, fileType)
                    .stream()
                    .filter(f -> Boolean.TRUE.equals(f.getIsPrimary()))
                    .toList();
            } else {
                existingPrimaryFiles = fileMetadataRepository
                    .findByEmployerProfileIdAndFileType(employerProfileId, fileType)
                    .stream()
                    .filter(f -> Boolean.TRUE.equals(f.getIsPrimary()))
                    .toList();
            }
            
            if (!existingPrimaryFiles.isEmpty()) {
                existingPrimaryFiles.forEach(f -> f.setIsPrimary(false));
                fileMetadataRepository.saveAll(existingPrimaryFiles);
                entityManager.flush(); // Ensure changes are committed before insert
            }
        }
        
        // Generate file path for reference (not used for storage, but kept for compatibility)
        String filePath = "database://" + profileIdString + "/" + UUID.randomUUID() + "." + extension;
        
        // Create file metadata with content stored in database
        FileMetadata fileMetadata = new FileMetadata();
        if (profileId != null) {
            // Use getReference to get a managed entity proxy without loading from DB
            Profile profile = entityManager.getReference(Profile.class, profileId);
            fileMetadata.setProfile(profile);
        } else {
            // Use getReference to get a managed entity proxy without loading from DB
            EmployerProfile employerProfile = entityManager.getReference(EmployerProfile.class, employerProfileId);
            fileMetadata.setEmployerProfile(employerProfile);
        }
        fileMetadata.setFileType(fileType);
        fileMetadata.setFileName(originalFilename);
        fileMetadata.setFilePath(filePath);
        fileMetadata.setFileSize(file.getSize());
        fileMetadata.setMimeType(file.getContentType());
        fileMetadata.setStorageProvider("DATABASE");
        fileMetadata.setFileContent(fileContent); // Store file content in PostgreSQL
        fileMetadata.setIsPrimary(isPrimary);
        fileMetadata.setUploadStatus(FileMetadata.UploadStatus.COMPLETED);
        
        return fileMetadataRepository.save(fileMetadata);
    }
    
    public List<FileMetadata> getFilesByProfileAndType(UUID profileId, FileMetadata.FileType fileType) {
        return fileMetadataRepository.findByProfileIdAndFileType(profileId, fileType);
    }
    
    public List<FileMetadata> getAllFilesByProfile(UUID profileId) {
        return fileMetadataRepository.findByProfileId(profileId);
    }
    
    @Transactional
    public void deleteFile(UUID fileId, UUID profileId) {
        // Fetch file with profile to avoid lazy loading issues
        FileMetadata fileMetadata = fileMetadataRepository.findByIdWithProfile(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));
        
        // Verify the file belongs to the specified profile
        UUID fileProfileId = fileMetadata.getProfile().getId();
        if (fileProfileId == null || !fileProfileId.equals(profileId)) {
            throw new RuntimeException("Unauthorized to delete this file");
        }
        
        // Delete metadata (file content is automatically deleted with the record)
        fileMetadataRepository.delete(fileMetadata);
    }
    
    public byte[] getFileContent(UUID fileId, UUID userId) {
        // Fetch file with profile to avoid lazy loading issues
        FileMetadata fileMetadata = fileMetadataRepository.findByIdWithProfile(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));
        
        // Check authorization using user ID (get user from profile)
        UUID fileOwnerUserId = fileMetadata.getProfile().getUser().getId();
        if (fileOwnerUserId == null || !fileOwnerUserId.equals(userId)) {
            throw new RuntimeException("Unauthorized to access this file");
        }
        
        if (fileMetadata.getFileContent() == null) {
            throw new RuntimeException("File content not found");
        }
        
        return fileMetadata.getFileContent();
    }
    
    public FileMetadata getFileMetadata(UUID fileId) {
        return fileMetadataRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));
    }
    
    @Transactional
    public void setPrimaryFile(UUID fileId, UUID profileId) {
        // Fetch file with profile to avoid lazy loading issues
        FileMetadata fileMetadata = fileMetadataRepository.findByIdWithProfile(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));
        
        // Verify the file belongs to the specified profile
        UUID fileProfileId = fileMetadata.getProfile().getId();
        if (fileProfileId == null || !fileProfileId.equals(profileId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        // If already primary, no need to do anything
        if (Boolean.TRUE.equals(fileMetadata.getIsPrimary())) {
            return;
        }
        
        // Unset all OTHER primary files of same type (exclude current file)
        List<FileMetadata> existingPrimaryFiles = fileMetadataRepository
            .findByProfileIdAndFileType(profileId, fileMetadata.getFileType())
            .stream()
            .filter(f -> !f.getId().equals(fileId)) // Exclude current file
            .filter(f -> Boolean.TRUE.equals(f.getIsPrimary())) // Only primary files
            .toList();
        
        if (!existingPrimaryFiles.isEmpty()) {
            existingPrimaryFiles.forEach(f -> f.setIsPrimary(false));
            fileMetadataRepository.saveAll(existingPrimaryFiles);
            entityManager.flush(); // Ensure changes are committed before update
        }
        
        // Now set this file as primary
        fileMetadata.setIsPrimary(true);
        fileMetadataRepository.save(fileMetadata);
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }
    
    private boolean isValidFileType(FileMetadata.FileType fileType, String extension) {
        return switch (fileType) {
            case PROFILE_PICTURE -> List.of("jpg", "jpeg", "png", "gif", "webp").contains(extension);
            case RESUME -> List.of("pdf", "doc", "docx", "txt").contains(extension);
            case COVER_LETTER -> List.of("pdf", "doc", "docx", "txt").contains(extension);
            case CERTIFICATE -> List.of("pdf", "jpg", "jpeg", "png").contains(extension);
            case PORTFOLIO -> List.of("pdf", "zip", "rar").contains(extension);
            case OTHER -> true; // Accept any file type
        };
    }
}

