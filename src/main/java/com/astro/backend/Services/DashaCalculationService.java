package com.astro.backend.Services;

import com.astro.backend.ResponseDTO.DashaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashaCalculationService {

        private static final String[] VIMSHOTTARI_SEQUENCE = {
            "Ketu", "Venus", "Sun", "Moon", "Mars", "Rahu", "Jupiter", "Saturn", "Mercury"
        };

    // Vimshottari Dasha years for each planet
    private static final Map<String, Integer> VIMSHOTTARI_YEARS = Map.ofEntries(
            Map.entry("Sun", 6),
            Map.entry("Moon", 10),
            Map.entry("Mars", 7),
            Map.entry("Mercury", 17),
            Map.entry("Jupiter", 16),
            Map.entry("Venus", 20),
            Map.entry("Saturn", 19),
            Map.entry("Rahu", 18),
            Map.entry("Ketu", 7)
    );

    /**
     * Calculate Vimshottari Dasha
     * @param dateOfBirth User's date of birth
     * @param nakshatra User's birth nakshatra
     * @return Current and upcoming dasha periods
     */
    public DashaResponse calculateVimshottariDasha(LocalDate dateOfBirth, String nakshatra) {
        try {
            // Calculate Nakshatra number (0-26)
            int nakshatraNum = getNakshatraNumber(nakshatra);

            // Without Moon longitude we cannot compute precise balance; fall back to simplified
            return buildSimplifiedDasha(dateOfBirth, nakshatraNum);

        } catch (Exception e) {
            log.error("Error calculating Vimshottari Dasha", e);
            throw new RuntimeException("Failed to calculate dasha: " + e.getMessage());
        }
    }

    /**
     * Preferred: Calculate Vimshottari Dasha using Moon longitude at birth for exact balance
     */
    public DashaResponse calculateVimshottariDasha(LocalDate dateOfBirth, String nakshatra, double moonLongitudeAtBirth) {
        try {
            final double NAK_DEG = 360.0 / 27.0; // 13°20'

            // Derive nakshatra index from Moon longitude for consistency
            int nakshatraIndex = (int) Math.floor(moonLongitudeAtBirth / NAK_DEG);
            String mahadashaLord = getDashaLordFromNakshatra(nakshatraIndex);

            // Fraction within current nakshatra at birth
            double offsetInNak = moonLongitudeAtBirth - (nakshatraIndex * NAK_DEG);
            double elapsedFrac = offsetInNak / NAK_DEG; // 0..1 progressed within nakshatra
            double balanceFrac = 1.0 - elapsedFrac;     // remaining fraction in start nakshatra

            // Balance years of start Mahadasha
            double startMahaYears = VIMSHOTTARI_YEARS.get(mahadashaLord);
            double startMahaBalanceYears = startMahaYears * balanceFrac;
            long startMahaBalanceDays = Math.round(startMahaBalanceYears * 365.2422);

            LocalDate mahaStartDate = dateOfBirth; // Mahadasha starts at birth
            LocalDate mahaEndDate = mahaStartDate.plusDays(startMahaBalanceDays);

            // Determine current antardasha at birth by consuming progressed units across antardashas
            // Progressed units within Mahadasha measured on 120-base
            double progressedUnits = elapsedFrac * 120.0;
            int startMahadashaIndex = getPlanetIndex(mahadashaLord);

            // Build upcoming 15 pratyantar entries from today's date
            List<Map<String, String>> dashaTable = generateDetailedDashaTableFromToday(
                    dateOfBirth,
                    startMahadashaIndex,
                    startMahaYears,
                    progressedUnits,
                    15
            );

            LocalDate today = LocalDate.now();
            double progressionPercentage = calculateProgression(mahaStartDate, mahaEndDate);
            String currentAntardasha = calculateAntardasha(mahadashaLord);

            return DashaResponse.builder()
                    .dashaType("Vimshottari")
                    .currentMahadasha(mahadashaLord)
                    .mahadashaLord(mahadashaLord)
                    .mahadashaStartDate(mahaStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .mahadashaEndDate(mahaEndDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .mahadashaRemainingYears((int) calculateYearsDifference(today, mahaEndDate))
                    .currentAntardasha(currentAntardasha)
                    .antardashaLord(currentAntardasha)
                    .mahadashaSignification(getSignification(mahadashaLord))
                    .upcomingChanges(getUpcomingChanges(mahadashaLord))
                    .remedyAdvice(getRemedy(mahadashaLord))
                    .progressionPercentage((int) Math.round(progressionPercentage))
                    .dashaTable(dashaTable)
                    .build();
        } catch (Exception e) {
            log.error("Error calculating Vimshottari Dasha (exact)", e);
            throw new RuntimeException("Failed to calculate dasha: " + e.getMessage());
        }
    }

    private DashaResponse buildSimplifiedDasha(LocalDate dateOfBirth, int nakshatraNum) {
        // Determine starting dasha lord
        String mahadashaLord = getDashaLordFromNakshatra(nakshatraNum);

        // Calculate dasha periods (simplified fall-back)
        LocalDate[] dashaPeriod = calculateDashaPeriod(dateOfBirth, nakshatraNum);
        LocalDate today = LocalDate.now();
        String antardashaLord = calculateAntardasha(mahadashaLord);
        double progressionPercentage = calculateProgression(dashaPeriod[0], dashaPeriod[1]);

        // Generate detailed dasha table (simplified)
        List<Map<String, String>> dashaTable = generateDetailedDashaTable(dateOfBirth, nakshatraNum);

        return DashaResponse.builder()
                .dashaType("Vimshottari")
                .currentMahadasha(mahadashaLord)
                .mahadashaLord(mahadashaLord)
                .mahadashaStartDate(dashaPeriod[0].format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .mahadashaEndDate(dashaPeriod[1].format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .mahadashaRemainingYears((int) calculateYearsDifference(today, dashaPeriod[1]))
                .currentAntardasha(antardashaLord)
                .antardashaLord(antardashaLord)
                .mahadashaSignification(getSignification(mahadashaLord))
                .upcomingChanges(getUpcomingChanges(mahadashaLord))
                .remedyAdvice(getRemedy(mahadashaLord))
                .progressionPercentage((int) Math.round(progressionPercentage))
                .dashaTable(dashaTable)
                .build();
    }

    /**
     * Calculate Yogini Dasha
     */
    public DashaResponse calculateYoginiDasha(LocalDate dateOfBirth, String nakshatra) {
        try {
            String[] yoginis = {
                    "Mangala", "Pingala", "Dhanya", "Bhramari",
                    "Bhadrika", "Ulka", "Siddha", "Sankata"
            };
            int[] years = {1, 2, 3, 4, 5, 6, 7, 8};

            int nakshatraNum = getNakshatraNumber(nakshatra);
            int yoginiIndex = nakshatraNum % yoginis.length;

            String current = yoginis[yoginiIndex];
            int durationYears = years[yoginiIndex];

            LocalDate start = dateOfBirth.plusYears(1);
            LocalDate end = start.plusYears(durationYears);

            return DashaResponse.builder()
                    .dashaType("Yogini")
                    .currentMahadasha(current)
                    .mahadashaLord(current)
                    .mahadashaStartDate(start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .mahadashaEndDate(end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .mahadashaRemainingYears((int) calculateYearsDifference(LocalDate.now(), end))
                    .remedyAdvice("Yogini dasha insights depend on birth nakshatra")
                    .build();
        } catch (Exception e) {
            log.error("Error calculating Yogini Dasha", e);
            throw new RuntimeException("Failed to calculate Yogini dasha: " + e.getMessage());
        }
    }

    /**
     * Get Nakshatra number (0-26)
     */
    private int getNakshatraNumber(String nakshatra) {
        String[] nakshatras = {
                "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashirsha", "Ardra", "Punarvasu",
                "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta",
                "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha",
                "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada",
                "Uttara Bhadrapada", "Revati"
        };

        for (int i = 0; i < nakshatras.length; i++) {
            if (nakshatras[i].equalsIgnoreCase(nakshatra)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Determine dasha lord based on birth nakshatra
     */
    private String getDashaLordFromNakshatra(int nakshatraNum) {
        // Dasha sequence follows 9-planet Vimshottari order
        int planetIndex = (nakshatraNum / 3) % VIMSHOTTARI_SEQUENCE.length;
        return VIMSHOTTARI_SEQUENCE[planetIndex];
    }

    /**
     * Calculate dasha start and end dates
     */
    private LocalDate[] calculateDashaPeriod(LocalDate dateOfBirth, int nakshatraNum) {
        // Simplified calculation
        String lord = getDashaLordFromNakshatra(nakshatraNum);
        int years = VIMSHOTTARI_YEARS.getOrDefault(lord, 10);

        LocalDate start = dateOfBirth.plusYears(2);  // Simplified
        LocalDate end = start.plusYears(years);

        return new LocalDate[]{start, end};
    }

    /**
     * Calculate antardasha lord
     */
    private String calculateAntardasha(String mahadashaLord) {
        for (int i = 0; i < VIMSHOTTARI_SEQUENCE.length; i++) {
            if (VIMSHOTTARI_SEQUENCE[i].equalsIgnoreCase(mahadashaLord)) {
                int next = (i + 1) % VIMSHOTTARI_SEQUENCE.length;
                return VIMSHOTTARI_SEQUENCE[next];
            }
        }
        return "Ketu";
    }

    /**
     * Calculate percentage progression of dasha
     */
    private double calculateProgression(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end);
        long passedDays = java.time.temporal.ChronoUnit.DAYS.between(start, today);

        if (totalDays == 0) return 0;
        return (double) passedDays / totalDays * 100;
    }

    /**
     * Calculate years difference
     */
    private long calculateYearsDifference(LocalDate from, LocalDate to) {
        return java.time.temporal.ChronoUnit.YEARS.between(from, to);
    }

    /**
     * Get dasha signification
     */
    private String getSignification(String lord) {
        return switch (lord) {
            case "Sun" -> "Authority, Leadership, Confidence";
            case "Moon" -> "Emotions, Home, Family";
            case "Mars" -> "Energy, Aggression, Ambition";
            case "Mercury" -> "Communication, Commerce, Intellect";
            case "Jupiter" -> "Expansion, Wisdom, Fortune";
            case "Venus" -> "Love, Luxury, Creativity";
            case "Saturn" -> "Hard work, Delays, Discipline";
            default -> "Mixed results";
        };
    }

    /**
     * Get upcoming changes
     */
    private String getUpcomingChanges(String nextLord) {
        return "Dasha of " + nextLord + " will bring its specific effects";
    }

    /**
     * Get remedy advice for dasha
     */
    private String getRemedy(String lord) {
        return switch (lord) {
            case "Sun" -> "Wear Ruby, Chant Aditya Hridayam";
            case "Moon" -> "Wear Pearl, Perform Moon worship";
            case "Mars" -> "Wear Red Coral, Recite Hanuman Chalisa";
            case "Mercury" -> "Wear Emerald, Chant Mercury mantra";
            case "Jupiter" -> "Wear Yellow Sapphire, Fast on Thursdays";
            case "Venus" -> "Wear Diamond, Fast on Fridays";
            case "Saturn" -> "Wear Blue Sapphire, Serve poor people";
            default -> "Consult an astrologer";
        };
    }

    /**
     * Generate detailed Vimshottari dasha table using exact formula
     * Generates 15+ days of dasha periods with proper calculations
     */
    private List<Map<String, String>> generateDetailedDashaTable(LocalDate dateOfBirth, int nakshatraNum) {
        List<Map<String, String>> dashaTable = new ArrayList<>();
        
        try {
            // Step 1: Get starting Mahadasha lord from Nakshatra
            String startMahadashaLord = getDashaLordFromNakshatra(nakshatraNum);
            int startMahadashaIndex = getPlanetIndex(startMahadashaLord);
            
            LocalDate currentDate = LocalDate.now();
            int entriesGenerated = 0;
            final int TARGET_ENTRIES = 15; // Generate 15 dasha entries
            
            // Iterate through Mahadashas
            for (int mahaIndex = 0; mahaIndex < VIMSHOTTARI_SEQUENCE.length && entriesGenerated < TARGET_ENTRIES; mahaIndex++) {
                String mahadashaLord = VIMSHOTTARI_SEQUENCE[(startMahadashaIndex + mahaIndex) % VIMSHOTTARI_SEQUENCE.length];
                double mahadashaYears = VIMSHOTTARI_YEARS.get(mahadashaLord);
                
                // Iterate through Antardashas
                for (int antarIndex = 0; antarIndex < VIMSHOTTARI_SEQUENCE.length && entriesGenerated < TARGET_ENTRIES; antarIndex++) {
                    String antardashaLord = VIMSHOTTARI_SEQUENCE[(startMahadashaIndex + antarIndex) % VIMSHOTTARI_SEQUENCE.length];
                    
                    // Step 5: Calculate Antardasha duration using exact formula
                    // Antar Duration = (MahadashaYears × AntarPlanetYears) / 120
                    double antardashaYears = (mahadashaYears * VIMSHOTTARI_YEARS.get(antardashaLord)) / 120.0;
                    
                    // Iterate through Pratyantar dashas
                    for (int pratyIndex = 0; pratyIndex < VIMSHOTTARI_SEQUENCE.length && entriesGenerated < TARGET_ENTRIES; pratyIndex++) {
                        String pratyantarLord = VIMSHOTTARI_SEQUENCE[(startMahadashaIndex + pratyIndex) % VIMSHOTTARI_SEQUENCE.length];
                        
                        // Step 6: Calculate Pratyantar duration using exact formula
                        // Pratyantar Duration = (AntarYears × PratyantarPlanetYears) / 120
                        double pratyantarYears = (antardashaYears * VIMSHOTTARI_YEARS.get(pratyantarLord)) / 120.0;
                        double pratyantarDays = pratyantarYears * 365.2422; // Convert to days
                        
                        // Add days to current date
                        LocalDate pratyantarEndDate = currentDate.plusDays((long) pratyantarDays);
                        
                        // Create dasha entry
                        String periodName = abbreviatePlanet(mahadashaLord) + "-" + 
                                           abbreviatePlanet(antardashaLord) + "-" + 
                                           abbreviatePlanet(pratyantarLord);
                        String dayName = pratyantarEndDate.getDayOfWeek().toString().substring(0, 3); // Mon, Tue, etc
                        String formattedDate = pratyantarEndDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                        
                        Map<String, String> dashaEntry = new HashMap<>();
                        dashaEntry.put("period", periodName);
                        dashaEntry.put("day", dayName);
                        dashaEntry.put("endDate", formattedDate);
                        
                        dashaTable.add(dashaEntry);
                        entriesGenerated++;
                        
                        currentDate = pratyantarEndDate;
                        
                        // Stop if we have enough entries
                        if (entriesGenerated >= TARGET_ENTRIES) {
                            break;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error generating detailed dasha table", e);
        }
        
        return dashaTable;
    }

    /**
     * Generate 15 pratyantar entries starting from today's date by traversing
     * the birth timeline using exact Vimshottari formulas.
     */
    private List<Map<String, String>> generateDetailedDashaTableFromToday(
            LocalDate dateOfBirth,
            int startMahadashaIndex,
            double startMahadashaYears,
            double progressedUnitsInStartMahadasha,
            int targetEntries
    ) {
        List<Map<String, String>> table = new ArrayList<>();
        try {
            // Build full timeline from birth until we reach today, then emit next entries
            LocalDate pointer = dateOfBirth;
            LocalDate today = LocalDate.now();
            long daysToToday = java.time.temporal.ChronoUnit.DAYS.between(dateOfBirth, today);
            long consumedDays = 0;

            // Consume starting Mahadasha first
            // Antardasha distribution measured on 120 base; determine starting antar/praty by progressed units
            double remainingUnits = 120.0 - progressedUnitsInStartMahadasha; // units remaining in start Mahadasha

            int mahaIdx = startMahadashaIndex;
            int antarStartIdx = startMahadashaIndex; // Antardasha cycle starts with Mahadasha lord
            double mahaYears = startMahadashaYears;

            boolean locatedCurrent = false;

            // Iterate over Mahadashas cyclically until we pass today
            for (int mahaCount = 0; mahaCount < VIMSHOTTARI_SEQUENCE.length * 3 && !locatedCurrent; mahaCount++) {
                String mahaLord = VIMSHOTTARI_SEQUENCE[mahaIdx % VIMSHOTTARI_SEQUENCE.length];
                mahaYears = (mahaCount == 0) ? startMahadashaYears : VIMSHOTTARI_YEARS.get(mahaLord);

                // Iterate Antardashas within this Mahadasha
                for (int a = 0; a < VIMSHOTTARI_SEQUENCE.length && !locatedCurrent; a++) {
                    int antarIdx = (antarStartIdx + a) % VIMSHOTTARI_SEQUENCE.length;
                    String antarLord = VIMSHOTTARI_SEQUENCE[antarIdx];
                    double antarYears = (mahaYears * VIMSHOTTARI_YEARS.get(antarLord)) / 120.0;
                    double antarUnits = VIMSHOTTARI_YEARS.get(antarLord); // units on 120-base

                    // Determine pratyantar start index
                    int pratyStartIdx = antarIdx;

                    // If in first Mahadasha and we still have remainingUnits, fast-forward into the correct antar/praty position
                    if (mahaCount == 0 && remainingUnits > 0) {
                        if (remainingUnits > antarUnits) {
                            remainingUnits -= antarUnits; // skip this antardasha entirely
                            continue;
                        }
                        // We're inside this antardasha; iterate pratyantars to locate today's position
                        for (int p = 0; p < VIMSHOTTARI_SEQUENCE.length; p++) {
                            int pratyIdx = (pratyStartIdx + p) % VIMSHOTTARI_SEQUENCE.length;
                            String pratyLord = VIMSHOTTARI_SEQUENCE[pratyIdx];
                            double pratyYears = (antarYears * VIMSHOTTARI_YEARS.get(pratyLord)) / 120.0;
                            long pratyDays = Math.round(pratyYears * 365.2422);
                            if (consumedDays + pratyDays >= daysToToday) {
                                // Found current pratyantar; now emit targetEntries consecutively
                                // Emit current and next (targetEntries) entries
                                int emitted = 0;
                                int emitMahaIdx = mahaIdx;
                                int emitAntarIdx = antarIdx;
                                int emitPratyIdx = pratyIdx;
                                LocalDate emitPointer = today; // start emitting from today forward
                                while (emitted < targetEntries) {
                                    String eMaha = VIMSHOTTARI_SEQUENCE[emitMahaIdx % VIMSHOTTARI_SEQUENCE.length];
                                    String eAntar = VIMSHOTTARI_SEQUENCE[emitAntarIdx % VIMSHOTTARI_SEQUENCE.length];
                                    String ePraty = VIMSHOTTARI_SEQUENCE[emitPratyIdx % VIMSHOTTARI_SEQUENCE.length];
                                    double eMahaYears = (emitMahaIdx == startMahadashaIndex) ? startMahadashaYears : VIMSHOTTARI_YEARS.get(eMaha);
                                    double eAntarYears = (eMahaYears * VIMSHOTTARI_YEARS.get(eAntar)) / 120.0;
                                    double ePratyYears = (eAntarYears * VIMSHOTTARI_YEARS.get(ePraty)) / 120.0;
                                    long ePratyDays = Math.round(ePratyYears * 365.2422);

                                    LocalDate eEnd = emitPointer.plusDays(ePratyDays);
                                    String periodName = abbreviatePlanet(eMaha) + "-" + abbreviatePlanet(eAntar) + "-" + abbreviatePlanet(ePraty);
                                    String dayName = eEnd.getDayOfWeek().toString().substring(0, 3);
                                    String formattedDate = eEnd.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                                    Map<String, String> entry = new HashMap<>();
                                    entry.put("period", periodName);
                                    entry.put("day", dayName);
                                    entry.put("endDate", formattedDate);
                                    table.add(entry);

                                    emitted++;
                                    emitPointer = eEnd; // move forward

                                    // advance praty/antar/maha indices
                                    emitPratyIdx = (emitPratyIdx + 1) % VIMSHOTTARI_SEQUENCE.length;
                                    if (emitPratyIdx == pratyStartIdx) {
                                        emitAntarIdx = (emitAntarIdx + 1) % VIMSHOTTARI_SEQUENCE.length;
                                        if (emitAntarIdx == antarStartIdx) {
                                            emitMahaIdx = (emitMahaIdx + 1) % VIMSHOTTARI_SEQUENCE.length;
                                        }
                                    }
                                }

                                locatedCurrent = true;
                                break;
                            } else {
                                consumedDays += pratyDays;
                                pointer = pointer.plusDays(pratyDays);
                            }
                        }
                        // After locating current, break out
                        if (locatedCurrent) break;
                        remainingUnits = 0; // if not located, remaining units exhausted
                    }

                    // If not in first Mahadasha or remainingUnits already handled, walk pratyantars normally
                    for (int p = 0; p < VIMSHOTTARI_SEQUENCE.length && !locatedCurrent; p++) {
                        int pratyIdx = (pratyStartIdx + p) % VIMSHOTTARI_SEQUENCE.length;
                        String pratyLord = VIMSHOTTARI_SEQUENCE[pratyIdx];
                        double pratyYears = (antarYears * VIMSHOTTARI_YEARS.get(pratyLord)) / 120.0;
                        long pratyDays = Math.round(pratyYears * 365.2422);
                        if (consumedDays + pratyDays >= daysToToday) {
                            // Found current pratyantar; emit next entries
                            LocalDate emitPointer = today;
                            int emitted = 0;
                            int emitMahaIdx = mahaIdx;
                            int emitAntarIdx = antarIdx;
                            int emitPratyIdx = pratyIdx;
                            while (emitted < targetEntries) {
                                String eMaha = VIMSHOTTARI_SEQUENCE[emitMahaIdx % VIMSHOTTARI_SEQUENCE.length];
                                String eAntar = VIMSHOTTARI_SEQUENCE[emitAntarIdx % VIMSHOTTARI_SEQUENCE.length];
                                String ePraty = VIMSHOTTARI_SEQUENCE[emitPratyIdx % VIMSHOTTARI_SEQUENCE.length];
                                double eMahaYears = (emitMahaIdx == startMahadashaIndex) ? startMahadashaYears : VIMSHOTTARI_YEARS.get(eMaha);
                                double eAntarYears = (eMahaYears * VIMSHOTTARI_YEARS.get(eAntar)) / 120.0;
                                double ePratyYears = (eAntarYears * VIMSHOTTARI_YEARS.get(ePraty)) / 120.0;
                                long ePratyDays = Math.round(ePratyYears * 365.2422);

                                LocalDate eEnd = emitPointer.plusDays(ePratyDays);
                                String periodName = abbreviatePlanet(eMaha) + "-" + abbreviatePlanet(eAntar) + "-" + abbreviatePlanet(ePraty);
                                String dayName = eEnd.getDayOfWeek().toString().substring(0, 3);
                                String formattedDate = eEnd.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                                Map<String, String> entry = new HashMap<>();
                                entry.put("period", periodName);
                                entry.put("day", dayName);
                                entry.put("endDate", formattedDate);
                                table.add(entry);

                                emitted++;
                                emitPointer = eEnd;

                                // advance praty/antar/maha indices
                                emitPratyIdx = (emitPratyIdx + 1) % VIMSHOTTARI_SEQUENCE.length;
                                if (emitPratyIdx == pratyStartIdx) {
                                    emitAntarIdx = (emitAntarIdx + 1) % VIMSHOTTARI_SEQUENCE.length;
                                    if (emitAntarIdx == antarStartIdx) {
                                        emitMahaIdx = (emitMahaIdx + 1) % VIMSHOTTARI_SEQUENCE.length;
                                    }
                                }
                            }
                            locatedCurrent = true;
                            break;
                        } else {
                            consumedDays += pratyDays;
                            pointer = pointer.plusDays(pratyDays);
                        }
                    }
                }
                mahaIdx = (mahaIdx + 1) % VIMSHOTTARI_SEQUENCE.length;
                antarStartIdx = mahaIdx; // next mahadasha's antar cycle starts with itself
            }
        } catch (Exception e) {
            log.error("Error generating dasha table from today", e);
        }
        return table;
    }
    
    /**
     * Get index of planet in Vimshottari sequence
     */
    private int getPlanetIndex(String planet) {
        for (int i = 0; i < VIMSHOTTARI_SEQUENCE.length; i++) {
            if (VIMSHOTTARI_SEQUENCE[i].equalsIgnoreCase(planet)) {
                return i;
            }
        }
        return 0;
    }
    
    /**
     * Abbreviate planet name for dasha display
     */
    private String abbreviatePlanet(String planet) {
        return switch (planet) {
            case "Sun" -> "Su";
            case "Moon" -> "Mo";
            case "Mars" -> "Ma";
            case "Mercury" -> "Me";
            case "Jupiter" -> "Ju";
            case "Venus" -> "Ve";
            case "Saturn" -> "Sa";
            case "Rahu" -> "Ra";
            case "Ketu" -> "Ke";
            default -> planet.substring(0, 2);
        };
    }
}
