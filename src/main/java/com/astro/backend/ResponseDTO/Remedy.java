package com.astro.backend.ResponseDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Remedy {

    private String remedyType;  // "Gemstone", "Rudraksha", "Mantra", "Color", "Day", "Direction"

    private String remedyFor;  // What problem it solves

    private String recommendation;  // Specific recommendation

    private String details;  // How to use/wear

    private String benefits;  // What it will help with

    private Double estimatedPrice;  // INR

    private String whereToSource;  // Where to buy

    private String precautions;  // Any precautions to take
}
