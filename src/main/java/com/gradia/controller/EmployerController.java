package com.gradia.controller;

import com.gradia.dto.*;
import com.gradia.model.EmployerProfile;
import com.gradia.model.FileMetadata;
import com.gradia.service.EmployerService;
import com.gradia.service.EmployerProfileService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/employers")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class EmployerController {
    
    private final EmployerService employerService;
    private final EmployerProfileService employerProfileService;
    private final FileStorageService fileStorageService;
    
    // Public endpoint - no authentication required
    @PreAuthorize("permitAll()")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody EmployerRegisterRequest request) {
        try {
            AuthResponse response = employerService.register(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Registration successful"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // Public endpoint - no authentication required
    @PreAuthorize("permitAll()")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody EmployerLoginRequest request) {
        try {
            AuthResponse response = employerService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PreAuthorize("hasRole('EMPLOYER')")
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<EmployerProfile>> createProfile(
            @Valid @RequestBody EmployerProfileRequest request,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            ApiResponse<EmployerProfile> response = employerProfileService.createOrUpdateProfile(userId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PreAuthorize("hasRole('EMPLOYER')")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<EmployerProfile>> updateProfile(
            @Valid @RequestBody EmployerProfileRequest request,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            ApiResponse<EmployerProfile> response = employerProfileService.createOrUpdateProfile(userId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // Public endpoint - no authentication required
    @PreAuthorize("permitAll()")
    @GetMapping("/profile/{profileId}")
    public ResponseEntity<ApiResponse<EmployerProfile>> getProfile(@PathVariable UUID profileId) {
        try {
            ApiResponse<EmployerProfile> response = employerProfileService.getProfileById(profileId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PreAuthorize("hasRole('EMPLOYER')")
    @PostMapping(value = "/profile/upload/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadCompanyLogo(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            
            // Get or create profile
            EmployerProfile profile = employerProfileService.getProfileForUserOrCreate(userId);
            
            // Upload file (using employer profile overload)
            FileMetadata fileMetadata = fileStorageService.uploadFile(
                profile, file, FileMetadata.FileType.PROFILE_PICTURE, true);
            
            // Update profile with logo URL (store file ID)
            profile.setProfilePicture(fileMetadata.getId().toString());
            employerProfileService.saveProfile(profile);
            
            FileUploadResponse response = FileUploadResponse.success(
                fileMetadata.getId(),
                fileMetadata.getFileName(),
                fileMetadata.getFilePath(),
                fileMetadata.getFileSize(),
                fileMetadata.getMimeType()
            );
            
            return ResponseEntity.ok(ApiResponse.success(response, "Company logo uploaded successfully"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("File upload failed: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PreAuthorize("hasRole('EMPLOYER')")
    @GetMapping("/profile/complete")
    public ResponseEntity<ApiResponse<Boolean>> checkProfileComplete(Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            boolean isComplete = employerProfileService.isProfileComplete(userId);
            return ResponseEntity.ok(ApiResponse.success(isComplete, 
                isComplete ? "Profile is complete" : "Profile is incomplete"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

