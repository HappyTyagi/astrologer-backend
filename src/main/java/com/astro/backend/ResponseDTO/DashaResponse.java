package com.astro.backend.ResponseDTO;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashaResponse {

    private String dashaType;  // "Vimshottari", "Yogini", "Char"

    private String currentMahadasha;
    private String mahadashaLord;
    private String mahadashaStartDate;
    private String mahadashaEndDate;
    private Integer mahadashaRemainingYears;

    private String currentAntardasha;
    private String antardashaLord;
    private String antardashaStartDate;
    private String antardashaEndDate;

    private String currentPratyantar;
    private String pratyantarStartDate;
    private String pratyantarEndDate;

    private String mahadashaSignification;  // What this dasha period represents
    private String upcomingChanges;  // What's coming next
    private String remedyAdvice;

    private Integer progressionPercentage;  // How much of current dasha has passed (%)
    
    // Detailed dasha table - List of upcoming periods
    private List<Map<String, String>> dashaTable;  // [{period: "Ra-Ke-Ke", day: "Wed", endDate: "17-09-2025"}, ...]
}
