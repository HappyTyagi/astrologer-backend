package com.astro.backend.RequestDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AstrologerSearchRequest {

    private String specialization;
    private String language;
    private Double minRating;
    private Double maxRate;
    private Boolean availableOnly;
    private String sortBy; // rating, rate, experience
}
