package com.gradia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCompletenessResponse {
    private Integer score; // 0-100
    private String level; // INCOMPLETE, BASIC, GOOD, COMPLETE
    private List<String> missingFields;
    private List<String> completedFields;
    
    public static ProfileCompletenessResponse of(Integer score, String level, 
                                                 List<String> missingFields, 
                                                 List<String> completedFields) {
        return new ProfileCompletenessResponse(score, level, missingFields, completedFields);
    }
}

