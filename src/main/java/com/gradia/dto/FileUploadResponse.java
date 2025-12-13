package com.gradia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private UUID fileId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String message;
    
    public static FileUploadResponse success(UUID fileId, String fileName, 
                                            String filePath, Long fileSize, 
                                            String mimeType) {
        return new FileUploadResponse(fileId, fileName, filePath, fileSize, mimeType, 
                                     "File uploaded successfully");
    }
}

