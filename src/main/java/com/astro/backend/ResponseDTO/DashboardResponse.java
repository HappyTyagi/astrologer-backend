package com.astro.backend.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private Long totalRegisteredUsers;
    private Double totalAmount;
    private Integer upcomingBirths;
    private Integer totalUpcomingPuja;
    private Boolean status;
    private String message;
}
