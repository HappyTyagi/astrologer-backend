package com.astro.backend.ResponseDTO;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AstrologerProfileResponse {

    private Long id;
    private Long userId;
    private String name;  // From User entity
    private String email; // From User entity
    private String mobileNumber; // From User entity
    private String profileImageUrl; // From User entity
    
    private Double consultationRate;
    private String bio;
    private String certifications;
    private Double rating;
    private Integer totalSessions;
    private Integer totalClients;
    
    private Boolean isAvailable;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
    
    private String languages;
    private Integer maxConcurrentChats;
    private String specializations;
    
    private Boolean isVerified;
    private Boolean isApproved;
    private LocalDateTime approvedAt;
    private String approvalNotes;
    
    private Integer experienceYears;
    private Boolean isActive;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Response metadata
    private Boolean status;
    private String message;
}
