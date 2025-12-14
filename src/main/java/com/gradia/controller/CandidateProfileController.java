package com.gradia.controller;

import com.gradia.dto.*;
import com.gradia.model.FileMetadata;
import com.gradia.model.Profile;
import com.gradia.service.CandidateProfileService;
import com.gradia.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/candidates/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class CandidateProfileController {
    
    private final CandidateProfileService profileService;
    private final FileStorageService fileStorageService;
    
    @PutMapping
    public ResponseEntity<ApiResponse<Profile>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            ApiResponse<Profile> response = profileService.updateProfile(userId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/completeness")
    public ResponseEntity<ApiResponse<ProfileCompletenessResponse>> getCompleteness(
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            ApiResponse<ProfileCompletenessResponse> response = 
                profileService.getProfileCompleteness(userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/full")
    public ResponseEntity<ApiResponse<CandidateProfileService.ProfileWithMetadata>> getFullProfile(
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            ApiResponse<CandidateProfileService.ProfileWithMetadata> response = 
                profileService.getFullProfile(userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // Public endpoint - no authentication required (override class-level @PreAuthorize)
    @PreAuthorize("permitAll()")
    @GetMapping("/{profileId}")
    public ResponseEntity<ApiResponse<Profile>> getProfile(@PathVariable UUID profileId) {
        try {
            ApiResponse<Profile> response = profileService.getProfileById(profileId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping(value = "/upload/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            
            // Get or create profile if it doesn't exist (allow resume upload during profile creation)
            Profile profile = profileService.getProfileForUserOrCreate(userId);
            
            // Upload file
            FileMetadata fileMetadata = fileStorageService.uploadFile(
                profile, file, FileMetadata.FileType.RESUME, isPrimary);
            
            FileUploadResponse response = FileUploadResponse.success(
                fileMetadata.getId(),
                fileMetadata.getFileName(),
                fileMetadata.getFilePath(),
                fileMetadata.getFileSize(),
                fileMetadata.getMimeType()
            );
            
            return ResponseEntity.ok(ApiResponse.success(response, "Resume uploaded successfully"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("File upload failed: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping(value = "/upload/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            
            // Get or create profile if it doesn't exist (allow picture upload during profile creation)
            Profile profile = profileService.getProfileForUserOrCreate(userId);
            
            // Upload file
            FileMetadata fileMetadata = fileStorageService.uploadFile(
                profile, file, FileMetadata.FileType.PROFILE_PICTURE, true);
            
            // Update profile with picture file ID (stored in database)
            profile.setProfilePicture(fileMetadata.getId().toString());
            profileService.saveProfile(profile);
            
            FileUploadResponse response = FileUploadResponse.success(
                fileMetadata.getId(),
                fileMetadata.getFileName(),
                fileMetadata.getFilePath(),
                fileMetadata.getFileSize(),
                fileMetadata.getMimeType()
            );
            
            return ResponseEntity.ok(ApiResponse.success(response, "Profile picture uploaded successfully"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("File upload failed: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/resumes")
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> getResumes(
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            Profile profile = profileService.getProfileForUser(userId);
            
            List<FileMetadata> resumes = fileStorageService.getFilesByProfileAndType(
                profile.getId(), FileMetadata.FileType.RESUME);
            
            List<FileUploadResponse> response = resumes.stream()
                .map(fm -> FileUploadResponse.success(
                    fm.getId(), fm.getFileName(), fm.getFilePath(), 
                    fm.getFileSize(), fm.getMimeType()))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(response, "Resumes retrieved successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/resumes/{fileId}/download")
    public ResponseEntity<byte[]> downloadResume(
            @PathVariable UUID fileId,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            byte[] fileContent = fileStorageService.getFileContent(fileId, userId);
            
            FileMetadata fileMetadata = fileStorageService.getFileMetadata(fileId);
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileMetadata.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(fileMetadata.getMimeType()))
                    .body(fileContent);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/picture/download")
    public ResponseEntity<byte[]> downloadProfilePicture(
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            Profile profile = profileService.getProfileForUser(userId);
            
            // Find profile picture file
            List<FileMetadata> pictures = fileStorageService.getFilesByProfileAndType(
                profile.getId(), FileMetadata.FileType.PROFILE_PICTURE);
            
            if (pictures.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            FileMetadata picture = pictures.get(0);
            byte[] fileContent = fileStorageService.getFileContent(picture.getId(), userId);
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "inline; filename=\"" + picture.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(picture.getMimeType()))
                    .body(fileContent);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/resumes/{fileId}")
    public ResponseEntity<ApiResponse<Object>> deleteResume(
            @PathVariable UUID fileId,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            // Get profile to get profile ID (not user ID)
            Profile profile = profileService.getProfileForUser(userId);
            fileStorageService.deleteFile(fileId, profile.getId());
            return ResponseEntity.ok(ApiResponse.success(null, "Resume deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/resumes/{fileId}/primary")
    public ResponseEntity<ApiResponse<Object>> setPrimaryResume(
            @PathVariable UUID fileId,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            // Get profile to get profile ID (not user ID)
            Profile profile = profileService.getProfileForUser(userId);
            fileStorageService.setPrimaryFile(fileId, profile.getId());
            return ResponseEntity.ok(ApiResponse.success(null, "Primary resume set successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

