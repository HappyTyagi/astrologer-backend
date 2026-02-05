package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.PlanetaryPositionRequest;
import com.astro.backend.ResponseDTO.FullKundliResponse;
import com.astro.backend.ResponseDTO.PlanetaryPositionResponse;
import com.astro.backend.Services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/astro-services")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AstroServicesController {

    private final AdvancedKundliService advancedKundliService;
    private final DashaCalculationService dashaCalculationService;
    private final CompatibilityMatchingService compatibilityService;
    private final RemedyRecommendationService remedyService;
    private final MuhuratService muhuratService;
    private final PredictionService predictionService;
    private final PlanetaryCalculationService planetaryCalculationService;

    /**
     * Generate complete birth chart (Kundli) with Planetary Positions and HTML rendering
     */
    @PostMapping("/kundli/calculate")
    public ResponseEntity<?> generateFullKundli(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = payload.get("userId") != null ? Long.valueOf(payload.get("userId").toString()) : null;
            String dateOfBirth = (String) payload.get("dateOfBirth");
            String timeOfBirth = (String) payload.get("timeOfBirth");
            Double latitude = payload.get("latitude") != null ? Double.valueOf(payload.get("latitude").toString()) : 0.0;
            Double longitude = payload.get("longitude") != null ? Double.valueOf(payload.get("longitude").toString()) : 0.0;
            String timezone = payload.get("timezone") != null ? payload.get("timezone").toString() : null;

            log.info("Generating full kundli for user: {}", userId);

            int[] dob = parseDate(dateOfBirth);
            double birthTime = parseTimeToHours(timeOfBirth);
            double lat = latitude != null ? latitude : 0.0;
            double lon = longitude != null ? longitude : 0.0;
            double tz = timezone != null ? parseTimezone(timezone) : 5.5;

            FullKundliResponse kundliData = advancedKundliService.generateFullKundli(
                    lat,
                    lon,
                    dob[0],
                    dob[1],
                    dob[2],
                    birthTime,
                    "User",
                    timeOfBirth
            );

            // Add Planetary Positions with HTML rendering
            try {
                PlanetaryPositionRequest planetaryRequest = PlanetaryPositionRequest.builder()
                        .year(dob[2])
                        .month(dob[1])
                        .date(dob[0])
                        .hours((int) birthTime)
                        .minutes((int) ((birthTime - (int) birthTime) * 60))
                        .seconds(0)
                        .latitude(lat)
                        .longitude(lon)
                        .timezone(tz)
                        .config(PlanetaryPositionRequest.Config.builder()
                                .observationPoint("topocentric")
                                .ayanamsha("lahiri")
                                .build())
                        .build();

                PlanetaryPositionResponse planetaryData = planetaryCalculationService
                        .calculatePlanetaryPositions(planetaryRequest, userId);

                String svgUrl = planetaryCalculationService.saveSvgToFile(planetaryData);
                planetaryData.setSvgUrl(svgUrl);

                kundliData.setPlanetaryPositions(planetaryData);
                log.info("✅ Planetary positions added to kundli response");
            } catch (Exception e) {
                log.warn("⚠️ Failed to add planetary positions: {}", e.getMessage());
                // Continue without planetary positions if calculation fails
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Full Kundli generated successfully with planetary positions and HTML",
                    "data", kundliData
            ));
        } catch (Exception e) {
            log.error("Error generating kundli", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private int[] parseDate(String dateOfBirth) {
        if (dateOfBirth == null || dateOfBirth.isBlank()) {
            throw new IllegalArgumentException("dateOfBirth is required");
        }

        // Only accept YYYY-MM-DD format for consistency
        String[] parts;
        if (dateOfBirth.contains("-")) {
            parts = dateOfBirth.split("-");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid dateOfBirth format. Expected YYYY-MM-DD");
            }
            int yyyy = Integer.parseInt(parts[0]);
            int mm = Integer.parseInt(parts[1]);
            int dd = Integer.parseInt(parts[2]);
            return new int[]{dd, mm, yyyy};
        }

        throw new IllegalArgumentException("Invalid dateOfBirth format. Use YYYY-MM-DD only");
    }

    private LocalDate parseDateToLocalDate(String dateOfBirth) {
        int[] dob = parseDate(dateOfBirth);
        return LocalDate.of(dob[2], dob[1], dob[0]);
    }

    private double parseTimeToHours(String timeOfBirth) {
        if (timeOfBirth == null || timeOfBirth.isBlank()) {
            throw new IllegalArgumentException("timeOfBirth is required");
        }

        String timeValue = timeOfBirth.trim().toUpperCase();
        boolean isPm = timeValue.endsWith("PM");
        boolean isAm = timeValue.endsWith("AM");

        if (isPm || isAm) {
            timeValue = timeValue.replace("AM", "").replace("PM", "").trim();
        }

        String[] parts = timeValue.split(":");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid timeOfBirth format. Expected HH:MM");
        }

        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);

        if (isPm && hours < 12) {
            hours += 12;
        } else if (isAm && hours == 12) {
            hours = 0;
        }

        return hours + ((double) minutes / 60.0);
    }

    private double parseTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return 5.5; // Default to IST
        }
        
        // Handle timezone names like "Asia/Kolkata", "Asia/Kolkata", etc.
        if (timezone.contains("/")) {
            // For now, extract the offset based on common patterns
            if (timezone.contains("Kolkata") || timezone.contains("India")) {
                return 5.5;
            } else if (timezone.contains("Delhi")) {
                return 5.5;
            } else if (timezone.contains("Mumbai")) {
                return 5.5;
            }
            // Default to 5.5 for any Asia/Indian timezone
            return 5.5;
        }
        
        // Try to parse as a number (e.g., "5.5", "+05:30")
        try {
            return Double.parseDouble(timezone);
        } catch (NumberFormatException e) {
            log.warn("Could not parse timezone: {}, using default 5.5", timezone);
            return 5.5;
        }
    }

    private boolean isPresent(FullKundliResponse.Dosha dosha) {
        return dosha != null && Boolean.TRUE.equals(dosha.getPresent());
    }

    /**
     * Get today's Lagna (Ascendant) chart
     */
    @GetMapping("/lagna/today")
    public ResponseEntity<?> getTodayLagnaChart(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        try {
            log.info("Calculating today's Lagna chart for lat: {}, lon: {}", latitude, longitude);
            
            var lagnaData = advancedKundliService.calculateTodayLagna(latitude, longitude);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Today's Lagna calculated successfully",
                    "data", lagnaData
            ));
        } catch (Exception e) {
            log.error("Error calculating today's lagna", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get today's Panchang
     */
    @GetMapping("/panchang/today")
    public ResponseEntity<?> getTodayPanchang(
            @RequestParam(required = false, defaultValue = "0.0") Double latitude,
            @RequestParam(required = false, defaultValue = "0.0") Double longitude) {
        try {
            log.info("Calculating today's Panchang for lat: {}, lon: {}", latitude, longitude);
            
            var panchangData = advancedKundliService.calculateTodayPanchang(latitude, longitude);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Today's Panchang calculated successfully",
                    "data", panchangData
            ));
        } catch (Exception e) {
            log.error("Error calculating today's panchang", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get Dasha calculations
     */
    @GetMapping("/dasha/{chartId}")
    public ResponseEntity<?> getDashaCalculations(
            @PathVariable Long chartId,
            @RequestParam String dateOfBirth,
            @RequestParam String nakshatra,
            @RequestParam(defaultValue = "Vimshottari") String dashaType) {
        try {
            log.info("Getting {} dasha for chart: {}", dashaType, chartId);

            LocalDate dob = parseDateToLocalDate(dateOfBirth);
            var dasha = "Yogini".equalsIgnoreCase(dashaType)
                ? dashaCalculationService.calculateYoginiDasha(dob, nakshatra)
                : dashaCalculationService.calculateVimshottariDasha(dob, nakshatra);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", dasha
            ));
        } catch (Exception e) {
            log.error("Error getting dasha", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get Yogini Dasha
     */
    @GetMapping("/dasha/yogini/{chartId}")
    public ResponseEntity<?> getYoginiDasha(
            @PathVariable Long chartId,
            @RequestParam String dateOfBirth,
            @RequestParam String nakshatra) {
        try {
            LocalDate dob = parseDateToLocalDate(dateOfBirth);
            var dasha = dashaCalculationService.calculateYoginiDasha(dob, nakshatra);
            return ResponseEntity.ok(Map.of("status", "success", "data", dasha));
        } catch (Exception e) {
            log.error("Error getting yogini dasha", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Calculate compatibility between two charts
     */
    @PostMapping("/compatibility/match")
    public ResponseEntity<?> checkCompatibility(
            @RequestParam(required = false) Long groomChartId,
            @RequestParam(required = false) Long brideChartId,
            @RequestBody CompatibilityRequest request) {
        try {
            log.info("Checking compatibility between {} and {}", groomChartId, brideChartId);

            if (request == null || request.groomGunas == null || request.brideGunas == null) {
            throw new IllegalArgumentException("groomGunas and brideGunas are required in request body");
            }

            var compatibility = compatibilityService.calculateGunMilan(
                request.groomGunas,
                request.brideGunas
            );

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "groomChart", groomChartId,
                    "brideChart", brideChartId,
                    "compatibility", compatibility
            ));
        } catch (Exception e) {
            log.error("Error checking compatibility", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

            /**
             * Calculate compatibility using bride/groom birth details
             */
            @PostMapping("/compatibility/match-details")
            public ResponseEntity<?> checkCompatibilityByDetails(
                @RequestBody CompatibilityDetailsRequest request) {
            try {
                if (request == null || request.groom == null || request.bride == null) {
                throw new IllegalArgumentException("groom and bride are required in request body");
                }

                var groom = request.groom;
                var bride = request.bride;

int[] groomDob = parseDate(groom.dateOfBirth());
            int[] brideDob = parseDate(bride.dateOfBirth());

            double groomTime = parseTimeToHours(groom.timeOfBirth());
            double brideTime = parseTimeToHours(bride.timeOfBirth());

            double groomLat = groom.latitude() != null ? groom.latitude() : 0.0;
            double groomLon = groom.longitude() != null ? groom.longitude() : 0.0;
            double brideLat = bride.latitude() != null ? bride.latitude() : 0.0;
            double brideLon = bride.longitude() != null ? bride.longitude() : 0.0;

            FullKundliResponse groomChart = advancedKundliService.generateFullKundli(
                    groomLat,
                    groomLon,
                    groomDob[0],
                    groomDob[1],
                    groomDob[2],
                    groomTime,
                    groom.name() != null && !groom.name().isBlank() ? groom.name() : "Groom",
                    groom.timeOfBirth()  // Pass original time string
            );

            FullKundliResponse brideChart = advancedKundliService.generateFullKundli(
                    brideLat,
                    brideLon,
                    brideDob[0],
                    brideDob[1],
                    brideDob[2],
                    brideTime,
                    bride.name() != null && !bride.name().isBlank() ? bride.name() : "Bride",
                    bride.timeOfBirth()  // Pass original time string
                );

                Map<String, Integer> groomGunas = compatibilityService.buildGunasFromChart(
                    groomChart.getMoonSign(),
                    groomChart.getNakshatra(),
                    isPresent(groomChart.getMangalDosha())
                );

                Map<String, Integer> brideGunas = compatibilityService.buildGunasFromChart(
                    brideChart.getMoonSign(),
                    brideChart.getNakshatra(),
                    isPresent(brideChart.getMangalDosha())
                );

                var compatibility = compatibilityService.calculateGunMilan(groomGunas, brideGunas);

                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "groom", Map.of(
                        "name", groomChart.getName(),
                        "moonSign", groomChart.getMoonSign(),
                        "nakshatra", groomChart.getNakshatra(),
                        "pada", groomChart.getPada(),
                        "mangalDosha", isPresent(groomChart.getMangalDosha())
                    ),
                    "bride", Map.of(
                        "name", brideChart.getName(),
                        "moonSign", brideChart.getMoonSign(),
                        "nakshatra", brideChart.getNakshatra(),
                        "pada", brideChart.getPada(),
                        "mangalDosha", isPresent(brideChart.getMangalDosha())
                    ),
                    "compatibility", compatibility
                ));
            } catch (Exception e) {
                log.error("Error checking compatibility by details", e);
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
            }

    /**
     * Get remedy suggestions for a chart
     */
    @PostMapping("/remedy")
    public ResponseEntity<?> getRemedySuggestions(@RequestBody RemedyRequest request) {
        try {
            log.info("Getting remedy suggestions");

            if (request == null || request.doshas == null || request.planets == null) {
                throw new IllegalArgumentException("doshas and planets are required in request body");
            }

            var remedies = remedyService.generateRemedies(request.doshas, request.planets);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "remedies", remedies
            ));
        } catch (Exception e) {
            log.error("Error getting remedies", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public record CompatibilityRequest(
            Map<String, Integer> groomGunas,
            Map<String, Integer> brideGunas
    ) {
    }

        public record CompatibilityDetailsRequest(
            PersonDetails groom,
            PersonDetails bride
        ) {
        }

        public record PersonDetails(
            String name,
            String dateOfBirth,
            String timeOfBirth,
            Double latitude,
            Double longitude,
            String timezone
        ) {
        }

    public record RemedyRequest(
            Map<String, Boolean> doshas,
            Map<String, String> planets
    ) {
    }

    /**
     * Find auspicious muhurat (timing)
     */
    @GetMapping("/muhurat/find")
    public ResponseEntity<?> findMuhurat(
            @RequestParam String eventType,
            @RequestParam(defaultValue = "30") int durationDays) {
        try {
            log.info("Finding muhurat for event: {}", eventType);
            
            LocalDate startDate = LocalDate.now().plusDays(1);
            var muhurats = muhuratService.findAuspiciousMuhurat(eventType, startDate, durationDays);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "eventType", eventType,
                    "muhurats", muhurats
            ));
        } catch (Exception e) {
            log.error("Error finding muhurat", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get monthly auspicious dates
     */
    @GetMapping("/muhurat/monthly")
    public ResponseEntity<?> getMonthlyMuhurat(
            @RequestParam String eventType,
            @RequestParam(defaultValue = "1") int month,
            @RequestParam(defaultValue = "2024") int year) {
        try {
            LocalDate monthStart = LocalDate.of(year, month, 1);
            var monthlyData = muhuratService.getMonthlyAuspiciousDates(monthStart, eventType);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", monthlyData
            ));
        } catch (Exception e) {
            log.error("Error getting monthly muhurat", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get daily horoscope
     */
    @GetMapping("/prediction/daily-horoscope")
    public ResponseEntity<?> getDailyHoroscope(
            @RequestParam String sunSign,
            @RequestParam(required = false) String date) {
        try {
            log.info("Getting daily horoscope for sign: {}", sunSign);
            
            LocalDate checkDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            var horoscope = predictionService.generateDailyHoroscope(sunSign, checkDate);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "horoscope", horoscope
            ));
        } catch (Exception e) {
            log.error("Error getting daily horoscope", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get weekly horoscope
     */
    @GetMapping("/prediction/weekly-horoscope")
    public ResponseEntity<?> getWeeklyHoroscope(
            @RequestParam String sunSign) {
        try {
            log.info("Getting weekly horoscope for sign: {}", sunSign);
            
            LocalDate weekStart = LocalDate.now();
            var horoscope = predictionService.generateWeeklyHoroscope(sunSign, weekStart);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "horoscope", horoscope
            ));
        } catch (Exception e) {
            log.error("Error getting weekly horoscope", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get monthly horoscope
     */
    @GetMapping("/prediction/monthly-horoscope")
    public ResponseEntity<?> getMonthlyHoroscope(
            @RequestParam String sunSign,
            @RequestParam(defaultValue = "1") int month,
            @RequestParam(defaultValue = "2024") int year) {
        try {
            log.info("Getting monthly horoscope for sign: {}", sunSign);
            
            var horoscope = predictionService.generateMonthlyHoroscope(sunSign, month, year);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "horoscope", horoscope
            ));
        } catch (Exception e) {
            log.error("Error getting monthly horoscope", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get transit analysis
     */
    @GetMapping("/prediction/transit-analysis")
    public ResponseEntity<?> getTransitAnalysis(
            @RequestParam String birthChart) {
        try {
            log.info("Getting transit analysis for chart: {}", birthChart);
            
            var transit = predictionService.getTransitAnalysis(birthChart, LocalDate.now());
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "transit", transit
            ));
        } catch (Exception e) {
            log.error("Error getting transit analysis", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get Sade Sati analysis
     */
    @GetMapping("/prediction/sade-sati")
    public ResponseEntity<?> getSadeSatiAnalysis(
            @RequestParam String moonSign) {
        try {
            log.info("Getting Sade Sati analysis for moon sign: {}", moonSign);
            
            var sadeSati = predictionService.getSadeSatiAnalysis(moonSign, LocalDate.now());
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "sadeSati", sadeSati
            ));
        } catch (Exception e) {
            log.error("Error getting Sade Sati analysis", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get Dhaiya analysis
     */
    @GetMapping("/prediction/dhaiya")
    public ResponseEntity<?> getDhaiyaAnalysis(
            @RequestParam String moonSign) {
        try {
            log.info("Getting Dhaiya analysis for moon sign: {}", moonSign);
            
            var dhaiya = predictionService.getDhaiyaAnalysis(moonSign, LocalDate.now());
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "dhaiya", dhaiya
            ));
        } catch (Exception e) {
            log.error("Error getting Dhaiya analysis", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check for astrology module
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Astrology module is running",
                "services", new String[]{
                        "Kundli Calculator",
                        "Dasha Calculator",
                        "Compatibility Matcher",
                        "Remedy Recommender",
                        "Muhurat Finder",
                        "Prediction Engine"
                }
        ));
    }
}
