package com.gradia.service;

import com.gradia.dto.*;
import com.gradia.model.EmployerProfile;
import com.gradia.model.User;
import com.gradia.repository.EmployerProfileRepository;
import com.gradia.repository.UserRepository;
import com.gradia.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployerService {
    
    private final UserRepository userRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    @Transactional
    public AuthResponse register(EmployerRegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.UserRole.EMPLOYER);
        user.setIsActive(true);
        user.setEmailVerified(false);
        
        user = userRepository.save(user);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId().toString());
        
        // Create response
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
        userInfo.setId(user.getId().toString());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRole().name());
        userInfo.setProfileComplete(false); // Profile not created yet
        
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setMessage("Registration successful. Please complete your company profile.");
        response.setUser(userInfo);
        
        return response;
    }
    
    public AuthResponse login(EmployerLoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        // Check if user is an employer
        if (user.getRole() != User.UserRole.EMPLOYER) {
            throw new RuntimeException("Invalid account type. Please use employer login.");
        }
        
        // Check if user is active
        if (!user.getIsActive()) {
            throw new RuntimeException("Account is deactivated");
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }
        
        // Check if employer profile exists and is complete
        Optional<com.gradia.model.EmployerProfile> profileOpt = employerProfileRepository.findByUserId(user.getId());
        boolean profileComplete = profileOpt.isPresent() && 
                profileOpt.get().getCompanyName() != null && 
                !profileOpt.get().getCompanyName().trim().isEmpty() &&
                profileOpt.get().getFullName() != null &&
                !profileOpt.get().getFullName().trim().isEmpty() &&
                profileOpt.get().getMobile() != null &&
                !profileOpt.get().getMobile().trim().isEmpty() &&
                profileOpt.get().getWebsite() != null &&
                !profileOpt.get().getWebsite().trim().isEmpty();
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId().toString());
        
        // Create response
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
        userInfo.setId(user.getId().toString());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRole().name());
        userInfo.setProfileComplete(profileComplete);
        
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setMessage(profileComplete ? "Login successful" : "Login successful. Please complete your company profile.");
        response.setUser(userInfo);
        
        return response;
    }
    
}

