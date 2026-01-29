package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AstrologerApprovalRequest {

    @NotNull(message = "Approval status is required")
    private Boolean isApproved;

    @Size(max = 1000, message = "Approval notes must not exceed 1000 characters")
    private String approvalNotes;
}
