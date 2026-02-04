package com.astro.backend.Services;

import com.astro.backend.Entity.BirthChart;
import com.astro.backend.Repositry.BirthChartRepository;
import com.astro.backend.RequestDTO.PlanetaryPositionRequest;
import com.astro.backend.ResponseDTO.PlanetaryPositionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import swisseph.SweConst;
import swisseph.SweDate;
import swisseph.SwissEph;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanetaryCalculationService {

    private static final SwissEph swissEph = new SwissEph("libs/ephe");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final BirthChartRepository birthChartRepository;

    // Planet constants
    private static final int[] PLANET_CONSTANTS = {
            SweConst.SE_SUN, SweConst.SE_MOON, SweConst.SE_MARS,
            SweConst.SE_MERCURY, SweConst.SE_JUPITER, SweConst.SE_VENUS,
            SweConst.SE_SATURN, SweConst.SE_TRUE_NODE, SweConst.SE_URANUS,
            SweConst.SE_NEPTUNE, SweConst.SE_PLUTO
    };

    private static final String[] PLANET_NAMES = {
            "Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus",
            "Saturn", "Rahu", "Uranus", "Neptune", "Pluto"
    };

    private static final Map<Integer, String> SIGN_MAP = Map.ofEntries(
            Map.entry(1, "Aries"), Map.entry(2, "Taurus"), Map.entry(3, "Gemini"),
            Map.entry(4, "Cancer"), Map.entry(5, "Leo"), Map.entry(6, "Virgo"),
            Map.entry(7, "Libra"), Map.entry(8, "Scorpio"), Map.entry(9, "Sagittarius"),
            Map.entry(10, "Capricorn"), Map.entry(11, "Aquarius"), Map.entry(12, "Pisces")
    );

    /**
     * Calculate planetary positions with house numbers and save to database
     */
    public PlanetaryPositionResponse calculatePlanetaryPositions(PlanetaryPositionRequest request, Long userId) {
        try {
            // Set ayanamsha FIRST before any calculations (default to Lahiri)
            String ayanamsha = request.getConfig() != null && request.getConfig().getAyanamsha() != null
                    ? request.getConfig().getAyanamsha()
                    : "lahiri";
            
            if ("lahiri".equalsIgnoreCase(ayanamsha)) {
                swissEph.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0, 0);
            }
            
            // Convert local time to UT (Universal Time) by subtracting timezone offset
            double localTime = request.getHours() + (request.getMinutes() / 60.0) + (request.getSeconds() / 3600.0);
            double utTime = localTime - request.getTimezone();
            
            // Handle day overflow/underflow
            int utYear = request.getYear();
            int utMonth = request.getMonth();
            int utDate = request.getDate();
            
            if (utTime < 0) {
                // Previous day
                utTime += 24;
                utDate -= 1;
                if (utDate < 1) {
                    utMonth -= 1;
                    if (utMonth < 1) {
                        utMonth = 12;
                        utYear -= 1;
                    }
                    // Get last day of previous month
                    utDate = java.time.YearMonth.of(utYear, utMonth).lengthOfMonth();
                }
            } else if (utTime >= 24) {
                // Next day
                utTime -= 24;
                utDate += 1;
                int daysInMonth = java.time.YearMonth.of(utYear, utMonth).lengthOfMonth();
                if (utDate > daysInMonth) {
                    utDate = 1;
                    utMonth += 1;
                    if (utMonth > 12) {
                        utMonth = 1;
                        utYear += 1;
                    }
                }
            }
            
            // Convert to Julian Day using UT
            SweDate sd = new SweDate(utYear, utMonth, utDate, utTime);
            double julDay = sd.getJulDay();

            double ayanamsaValue = swissEph.swe_get_ayanamsa(julDay);

            // Calculate ascendant with sidereal mode
            double[] cusps = new double[13];
            double[] ascmc = new double[10];
            swissEph.swe_houses(julDay, SweConst.SEFLG_SIDEREAL, 
                    request.getLatitude(), request.getLongitude(), 'P', cusps, ascmc);

            double ascendantDegree = ascmc[0];
            int ascendantSign = ((int) (ascendantDegree / 30)) + 1;
            double ascendantNormDegree = ascendantDegree % 30;

            // Prepare output data structures
            List<Map<String, Object>> outputList = new ArrayList<>();
            Map<String, Object> indexedPlanets = new LinkedHashMap<>();
            Map<String, Object> namedPlanets = new LinkedHashMap<>();

            // Add Ascendant
            Map<String, Object> ascendantData = new LinkedHashMap<>();
            ascendantData.put("name", "Ascendant");
            ascendantData.put("fullDegree", ascendantDegree);
            ascendantData.put("normDegree", ascendantNormDegree);
            ascendantData.put("isRetro", "false");
            ascendantData.put("current_sign", ascendantSign);
            indexedPlanets.put("0", ascendantData);

            Map<String, Object> ascendantNamed = new LinkedHashMap<>();
            ascendantNamed.put("current_sign", ascendantSign);
            ascendantNamed.put("fullDegree", ascendantDegree);
            ascendantNamed.put("normDegree", ascendantNormDegree);
            ascendantNamed.put("isRetro", "false");
            namedPlanets.put("Ascendant", ascendantNamed);

            // Calculate each planet
            for (int i = 0; i < PLANET_CONSTANTS.length; i++) {
                double[] xx = new double[6];
                StringBuffer serr = new StringBuffer();

                int flags = SweConst.SEFLG_SIDEREAL;
                if (PLANET_CONSTANTS[i] == SweConst.SE_TRUE_NODE) {
                    // Rahu (North Node)
                    swissEph.swe_calc(julDay, SweConst.SE_MEAN_NODE, flags, xx, serr);
                } else {
                    swissEph.swe_calc(julDay, PLANET_CONSTANTS[i], flags, xx, serr);
                }

                double longitude = xx[0];
                double speed = xx[3];

                // Handle Ketu separately (opposite of Rahu)
                if (PLANET_NAMES[i].equals("Rahu")) {
                    // Add Rahu
                    addPlanetData(indexedPlanets, namedPlanets, 
                            PLANET_NAMES[i], longitude, speed, ascendantDegree, i + 1);

                    // Calculate and add Ketu (180 degrees opposite)
                    double ketuLongitude = (longitude + 180) % 360;
                    addPlanetData(indexedPlanets, namedPlanets,
                            "Ketu", ketuLongitude, speed, ascendantDegree, i + 2);
                } else if (!PLANET_NAMES[i].equals("Rahu")) {
                    addPlanetData(indexedPlanets, namedPlanets,
                            PLANET_NAMES[i], longitude, speed, ascendantDegree, i + 1);
                }
            }

            // Add ayanamsa info
            Map<String, Object> ayanamsaData = new LinkedHashMap<>();
            ayanamsaData.put("name", "ayanamsa");
            ayanamsaData.put("value", ayanamsaValue);
            indexedPlanets.put("13", ayanamsaData);

            // Add debug info
            Map<String, Object> debugInfo = new LinkedHashMap<>();
            debugInfo.put("observation_point", 
                    request.getConfig() != null && request.getConfig().getObservationPoint() != null
                            ? request.getConfig().getObservationPoint()
                            : "topocentric");
            debugInfo.put("ayanamsa", ayanamsha);
            indexedPlanets.put("debug", debugInfo);

            // Create separate Navamsha data array
            Map<String, Object> navamshaIndexed = new LinkedHashMap<>();
            Map<String, Object> navamshaNames = new LinkedHashMap<>();
            
            // Calculate Navamsha Ascendant
            int[] navamshaAscendantData = calculateNavamshaSign(ascendantDegree);
            int navamshaAscendantSign = navamshaAscendantData[0];
            
            // Add Navamsha Ascendant
            Map<String, Object> navamshaAscData = new LinkedHashMap<>();
            navamshaAscData.put("name", "Ascendant");
            navamshaAscData.put("navamsha_sign", navamshaAscendantSign);
            navamshaAscData.put("navamsha_degree", navamshaAscendantData[1]);
            navamshaIndexed.put("0", navamshaAscData);
            
            Map<String, Object> navamshaAscNamed = new LinkedHashMap<>();
            navamshaAscNamed.put("navamsha_sign", navamshaAscendantSign);
            navamshaAscNamed.put("navamsha_degree", navamshaAscendantData[1]);
            navamshaNames.put("Ascendant", navamshaAscNamed);
            
            // Add Navamsha planets
            for (Map.Entry<String, Object> entry : namedPlanets.entrySet()) {
                String planetName = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> planetData = (Map<String, Object>) entry.getValue();
                
                if (!planetName.equals("Ascendant")) {
                    // Create Navamsha entry with house calculation
                    Integer navamshaSign = (Integer) planetData.get("navamsha_sign");
                    Integer navamshaDegree = (Integer) planetData.get("navamsha_degree");
                    int navamshaHouse = calculateNavamshaHouseNumber(navamshaSign, navamshaAscendantSign);
                    
                    Map<String, Object> navamshaPlanetData = new LinkedHashMap<>();
                    navamshaPlanetData.put("name", planetName);
                    navamshaPlanetData.put("navamsha_sign", navamshaSign);
                    navamshaPlanetData.put("navamsha_degree", navamshaDegree);
                    navamshaPlanetData.put("navamsha_house", navamshaHouse);
                    navamshaPlanetData.put("isRetro", planetData.get("isRetro"));
                    
                    navamshaIndexed.put(planetName, navamshaPlanetData);
                    
                    // Named map entry
                    Map<String, Object> navamshaPlanetNamed = new LinkedHashMap<>();
                    navamshaPlanetNamed.put("navamsha_sign", navamshaSign);
                    navamshaPlanetNamed.put("navamsha_degree", navamshaDegree);
                    navamshaPlanetNamed.put("navamsha_house", navamshaHouse);
                    navamshaPlanetNamed.put("isRetro", planetData.get("isRetro"));
                    navamshaNames.put(planetName, navamshaPlanetNamed);
                }
            }
            
            // Add ayanamsa to Navamsha output
            navamshaIndexed.put("13", ayanamsaData);
            navamshaIndexed.put("debug", debugInfo);

            // Calculate Transit chart (Today from Lagna)
            Map<String, Object> transitIndexed = new LinkedHashMap<>();
            Map<String, Object> transitNames = new LinkedHashMap<>();
            
            // Get current date/time for transit
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
            double currentUtTime = now.getHour() + (now.getMinute() / 60.0) + (now.getSecond() / 3600.0);
            SweDate currentDate = new SweDate(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), currentUtTime);
            double currentJulDay = currentDate.getJulDay();
            
            // Calculate current planets using birth ascendant as reference
            for (int i = 0; i < PLANET_CONSTANTS.length; i++) {
                double[] xx = new double[6];
                StringBuffer serr = new StringBuffer();
                
                int flags = SweConst.SEFLG_SIDEREAL;
                if (PLANET_CONSTANTS[i] == SweConst.SE_TRUE_NODE) {
                    swissEph.swe_calc(currentJulDay, SweConst.SE_MEAN_NODE, flags, xx, serr);
                } else {
                    swissEph.swe_calc(currentJulDay, PLANET_CONSTANTS[i], flags, xx, serr);
                }
                
                double transitLongitude = xx[0];
                double transitSpeed = xx[3];
                int transitSign = ((int) (transitLongitude / 30)) + 1;
                double transitNormDegree = transitLongitude % 30;
                boolean isRetro = transitSpeed < 0;
                int transitHouse = calculateHouseNumber(transitLongitude, ascendantDegree);
                
                // Handle Ketu
                if (PLANET_NAMES[i].equals("Rahu")) {
                    // Add Rahu
                    Map<String, Object> rahuData = new LinkedHashMap<>();
                    rahuData.put("name", "Rahu");
                    rahuData.put("current_sign", transitSign);
                    rahuData.put("fullDegree", transitLongitude);
                    rahuData.put("normDegree", transitNormDegree);
                    rahuData.put("transit_house", transitHouse);
                    rahuData.put("isRetro", String.valueOf(isRetro));
                    transitIndexed.put(PLANET_NAMES[i], rahuData);
                    transitNames.put("Rahu", rahuData);
                    
                    // Calculate and add Ketu
                    double ketuLongitude = (transitLongitude + 180) % 360;
                    int ketuSign = ((int) (ketuLongitude / 30)) + 1;
                    double ketuNormDegree = ketuLongitude % 30;
                    int ketuHouse = calculateHouseNumber(ketuLongitude, ascendantDegree);
                    
                    Map<String, Object> ketuData = new LinkedHashMap<>();
                    ketuData.put("name", "Ketu");
                    ketuData.put("current_sign", ketuSign);
                    ketuData.put("fullDegree", ketuLongitude);
                    ketuData.put("normDegree", ketuNormDegree);
                    ketuData.put("transit_house", ketuHouse);
                    ketuData.put("isRetro", String.valueOf(isRetro));
                    transitIndexed.put("Ketu", ketuData);
                    transitNames.put("Ketu", ketuData);
                } else if (!PLANET_NAMES[i].equals("Rahu")) {
                    Map<String, Object> planetTransitData = new LinkedHashMap<>();
                    planetTransitData.put("name", PLANET_NAMES[i]);
                    planetTransitData.put("current_sign", transitSign);
                    planetTransitData.put("fullDegree", transitLongitude);
                    planetTransitData.put("normDegree", transitNormDegree);
                    planetTransitData.put("transit_house", transitHouse);
                    planetTransitData.put("isRetro", String.valueOf(isRetro));
                    transitIndexed.put(PLANET_NAMES[i], planetTransitData);
                    transitNames.put(PLANET_NAMES[i], planetTransitData);
                }
            }
            
            // Add birth Ascendant reference to transit
            Map<String, Object> transitAscData = new LinkedHashMap<>();
            transitAscData.put("name", "Ascendant");
            transitAscData.put("current_sign", ascendantSign);
            transitAscData.put("fullDegree", ascendantDegree);
            transitAscData.put("transit_house", 1);
            transitIndexed.put("Ascendant", transitAscData);
            transitNames.put("Ascendant", transitAscData);
            
            transitIndexed.put("13", ayanamsaData);
            transitIndexed.put("debug", debugInfo);

            // Create output structure with separate Kundli, Navamsha, and Transit arrays
            Map<String, Object> outputMap = new LinkedHashMap<>();
            outputMap.put("kundli", Arrays.asList(indexedPlanets, namedPlanets));
            outputMap.put("navamsha", Arrays.asList(navamshaIndexed, navamshaNames));
            outputMap.put("transit", Arrays.asList(transitIndexed, transitNames));
            
            outputList.add(outputMap);

            // Save to database
            Long chartId = saveBirthChart(request, outputList, userId);

            // Build response
            return PlanetaryPositionResponse.builder()
                    .statusCode(200)
                    .input(buildInputData(request))
                    .output(outputList)
                    .chartId(chartId)
                    .build();

        } catch (Exception e) {
            log.error("Error calculating planetary positions", e);
            throw new RuntimeException("Failed to calculate planetary positions: " + e.getMessage());
        }
    }

    /**
     * Add planet data to both indexed and named maps (Kundli/Rasi chart)
     */
    private void addPlanetData(Map<String, Object> indexedMap, Map<String, Object> namedMap,
                                String planetName, double longitude, double speed,
                                double ascendantDegree, int index) {
        
        int sign = ((int) (longitude / 30)) + 1;
        double normDegree = longitude % 30;
        boolean isRetro = speed < 0;

        // Calculate house number
        int houseNumber = calculateHouseNumber(longitude, ascendantDegree);
        
        // Calculate Navamsha data
        int[] navamshaData = calculateNavamshaSign(longitude);
        int navamshaSign = navamshaData[0];
        int navamshaDegree = navamshaData[1];

        // For indexed map
        Map<String, Object> indexedData = new LinkedHashMap<>();
        indexedData.put("name", planetName);
        indexedData.put("fullDegree", longitude);
        indexedData.put("normDegree", normDegree);
        indexedData.put("isRetro", String.valueOf(isRetro));
        indexedData.put("current_sign", sign);
        if (!planetName.equals("Ascendant")) {
            indexedData.put("house_number", houseNumber);
        }
        // Add Navamsha info
        indexedData.put("navamsha_sign", navamshaSign);
        indexedData.put("navamsha_degree", navamshaDegree);
        indexedMap.put(String.valueOf(index), indexedData);

        // For named map
        Map<String, Object> namedData = new LinkedHashMap<>();
        namedData.put("current_sign", sign);
        if (!planetName.equals("Ascendant")) {
            namedData.put("house_number", houseNumber);
        }
        namedData.put("fullDegree", longitude);
        namedData.put("normDegree", normDegree);
        namedData.put("isRetro", String.valueOf(isRetro));
        // Add Navamsha info
        namedData.put("navamsha_sign", navamshaSign);
        namedData.put("navamsha_degree", navamshaDegree);
        namedMap.put(planetName, namedData);
    }

    /**
     * Calculate house number based on planet longitude and ascendant
     * House starts from Ascendant (Lagna) and moves forward
     */
    private int calculateHouseNumber(double planetLongitude, double ascendantDegree) {
        double difference = (planetLongitude - ascendantDegree + 360) % 360;
        int house = ((int) (difference / 30)) + 1;
        if (house > 12) house = house - 12;
        return house;
    }

    /**
     * Calculate Navamsha position (D9 - 9th divisional chart)
     * Navamsha divides 360 degrees into 9 equal parts of 40 degrees each
     * Returns [navamshaSign, degree_in_navamsha]
     */
    private int[] calculateNavamshaSign(double planetLongitude) {
        // Each Navamsha division is 360/9 = 40 degrees
        double navamshaDivision = planetLongitude / 40.0;
        int navamshaIndex = (int) navamshaDivision;
        if (navamshaIndex >= 9) navamshaIndex = 8;
        
        // Navamsha signs start from Aries (1) and repeat 9 times
        // Aries: 0-40°, Taurus: 40-80°, Gemini: 80-120°, etc.
        int navamshaSign = (navamshaIndex % 12) + 1;
        
        // Degree within the Navamsha (0-40)
        double degreeInNavamsha = planetLongitude % 40.0;
        
        return new int[]{navamshaSign, (int) degreeInNavamsha};
    }

    /**
     * Calculate Navamsha house number based on Navamsha ascendant
     */
    private int calculateNavamshaHouseNumber(int navamshaPlanetSign, int navamshaAscendantSign) {
        int difference = navamshaPlanetSign - navamshaAscendantSign;
        if (difference < 0) {
            difference += 12;
        }
        return (difference % 12) + 1;
    }

    /**
     * Save birth chart data to database
     */
    private Long saveBirthChart(PlanetaryPositionRequest request, 
                                 List<Map<String, Object>> outputData,
                                 Long userId) {
        try {
            LocalDate dob = LocalDate.of(request.getYear(), request.getMonth(), request.getDate());
            double birthTime = request.getHours() + (request.getMinutes() / 60.0);

            // Extract key data from output - new structure with "kundli" and "navamsha" keys
            @SuppressWarnings("unchecked")
            Map<String, Object> outputMap = (Map<String, Object>) outputData.get(0);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> kundliData = (List<Map<String, Object>>) outputMap.get("kundli");
            Map<String, Object> namedPlanets = kundliData.get(1);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> ascendantData = (Map<String, Object>) namedPlanets.get("Ascendant");
            Integer ascendantSign = (Integer) ascendantData.get("current_sign");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> sunData = (Map<String, Object>) namedPlanets.get("Sun");
            Integer sunSign = (Integer) sunData.get("current_sign");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> moonData = (Map<String, Object>) namedPlanets.get("Moon");
            Integer moonSign = (Integer) moonData.get("current_sign");

            BirthChart birthChart = BirthChart.builder()
                    .userId(userId != null ? userId : 0L)
                    .dateOfBirth(dob)
                    .birthTime(birthTime)
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .timezone("UTC+" + request.getTimezone())
                    .lagna(SIGN_MAP.get(ascendantSign))
                    .sunSign(SIGN_MAP.get(sunSign))
                    .moonSign(SIGN_MAP.get(moonSign))
                    .planetaryPositions(objectMapper.writeValueAsString(namedPlanets))
                    .build();

            @SuppressWarnings("null")
            BirthChart saved = birthChartRepository.save(birthChart);
            return saved.getId();

        } catch (Exception e) {
            log.error("Error saving birth chart", e);
            return null;
        }
    }

    /**
     * Build input data for response
     */
    private PlanetaryPositionResponse.InputData buildInputData(PlanetaryPositionRequest request) {
        return PlanetaryPositionResponse.InputData.builder()
                .year(request.getYear())
                .month(request.getMonth())
                .date(request.getDate())
                .hours(request.getHours())
                .minutes(request.getMinutes())
                .seconds(request.getSeconds())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .timezone(request.getTimezone())
                .config(PlanetaryPositionResponse.ConfigData.builder()
                        .observationPoint(request.getConfig() != null 
                                && request.getConfig().getObservationPoint() != null
                                ? request.getConfig().getObservationPoint()
                                : "topocentric")
                        .ayanamsha(request.getConfig() != null 
                                && request.getConfig().getAyanamsha() != null
                                ? request.getConfig().getAyanamsha()
                                : "lahiri")
                        .build())
                .build();
    }

    /**
     * Save HTML Kundli chart to file and return the URL
     */
    @org.springframework.beans.factory.annotation.Value("${spring.web.resources.static-locations:classpath:/static/}")
    private String staticResourceLocation;
    
    public String saveSvgToFile(PlanetaryPositionResponse response) {
        try {
            // Create directory if it doesn't exist using proper resource path
            String baseDir = "target/classes/static/kundli-charts";
            java.nio.file.Path dirPath = java.nio.file.Paths.get(baseDir);
            
            // If target doesn't work (dev mode), try src/main/resources
            if (!baseDir.contains("target")) {
                baseDir = "src/main/resources/static/kundli-charts";
                dirPath = java.nio.file.Paths.get(baseDir);
            }
            
            if (!java.nio.file.Files.exists(dirPath)) {
                java.nio.file.Files.createDirectories(dirPath);
            }

            // Extract data from response
            Long chartId = response.getChartId();
            int year = response.getInput().getYear();
            int month = response.getInput().getMonth();
            int date = response.getInput().getDate();
            
            // Generate unique filename
            String timestamp = String.valueOf(System.currentTimeMillis());
            String userPrefix = chartId != null ? "chart" + chartId + "_" : "";
            String filename = String.format("%skundli_%d%02d%02d_%s.html", 
                    userPrefix, year, month, date, timestamp);
            
            // Generate HTML content from planetary positions
            String htmlData = generateKundliHtml(response);
            
            // Save file
            java.nio.file.Path filePath = dirPath.resolve(filename);
            java.nio.file.Files.write(filePath, htmlData.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            log.info("Chart saved to: " + filePath.toAbsolutePath());
            
            // Return URL path
            return "/kundli-charts/" + filename;
            
        } catch (Exception e) {
            log.error("Error saving Kundli HTML file", e);
            return null;
        }
    }

    /**
     * Generate Kundli HTML report with dynamic data
     */
    private String generateKundliHtml(PlanetaryPositionResponse response) {
        StringBuilder html = new StringBuilder();

        // Extract data from new output structure
        @SuppressWarnings("unchecked")
        Map<String, Object> outputMap = (Map<String, Object>) response.getOutput().get(0);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> kundliData = (List<Map<String, Object>>) outputMap.get("kundli");
        Map<String, Object> indexedPlanets = kundliData.get(0);
        Map<String, Object> namedPlanets = kundliData.get(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> ascendantData = (Map<String, Object>) namedPlanets.get("Ascendant");
        @SuppressWarnings("unchecked")
        Map<String, Object> sunData = (Map<String, Object>) namedPlanets.get("Sun");
        @SuppressWarnings("unchecked")
        Map<String, Object> moonData = (Map<String, Object>) namedPlanets.get("Moon");

        Integer ascendantSign = ascendantData != null ? (Integer) ascendantData.get("current_sign") : null;
        Double ascendantDegree = ascendantData != null ? (Double) ascendantData.get("fullDegree") : 0.0;
        Integer sunSign = sunData != null ? (Integer) sunData.get("current_sign") : null;
        Integer moonSign = moonData != null ? (Integer) moonData.get("current_sign") : null;

        @SuppressWarnings("unchecked")
        Map<String, Object> ayanamsaData = indexedPlanets != null ? (Map<String, Object>) indexedPlanets.get("13") : null;
        Double ayanamsaValue = ayanamsaData != null ? (Double) ayanamsaData.get("value") : null;

        Map<String, String> planetAbbr = Map.ofEntries(
                Map.entry("Sun", "Sun"),
                Map.entry("Moon", "Mo"),
                Map.entry("Mars", "Mar"),
                Map.entry("Mercury", "Mer"),
                Map.entry("Jupiter", "Jup"),
                Map.entry("Venus", "Ve"),
                Map.entry("Saturn", "Sat"),
                Map.entry("Rahu", "Ra"),
                Map.entry("Ketu", "Ke"),
                Map.entry("Ascendant", "As"),
                Map.entry("Uranus", "Ur"),
                Map.entry("Neptune", "Ne"),
                Map.entry("Pluto", "Pl")
        );

        Map<Integer, List<String>> planetsBySign = new HashMap<>();
        for (Map.Entry<String, Object> entry : namedPlanets.entrySet()) {
            String planetName = entry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> planetData = (Map<String, Object>) entry.getValue();
            Integer sign = (Integer) planetData.get("current_sign");
            if (sign != null) {
                planetsBySign.computeIfAbsent(sign, k -> new ArrayList<>()).add(planetName);
            }
        }
        if (ascendantSign != null && !planetsBySign.getOrDefault(ascendantSign, new ArrayList<>()).contains("Ascendant")) {
            planetsBySign.computeIfAbsent(ascendantSign, k -> new ArrayList<>()).add(0, "Ascendant");
        }

        // Extract Navamsha data
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> navamshaData = (List<Map<String, Object>>) outputMap.get("navamsha");
        Map<String, Object> navamshaNamedPlanets = navamshaData.get(1);
        
        // Calculate Navamsha Ascendant
        int[] navamshaAscendantData = calculateNavamshaSign(ascendantDegree);
        int navamshaAscendantSign = navamshaAscendantData[0];
        
        // Build planetsByNavamshaHouse from Navamsha data
        Map<Integer, List<String>> planetsByNavamshaHouse = new HashMap<>();
        for (Map.Entry<String, Object> entry : navamshaNamedPlanets.entrySet()) {
            String planetName = entry.getKey();
            if ("Ascendant".equals(planetName)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> planetData = (Map<String, Object>) entry.getValue();
            Integer navamshaHouseNum = (Integer) planetData.get("navamsha_house");
            if (navamshaHouseNum != null) {
                planetsByNavamshaHouse.computeIfAbsent(navamshaHouseNum, k -> new ArrayList<>()).add(planetName);
            }
        }
        
        // Add Navamsha Ascendant to house 1
        int navamshaAscendantHouse = 1;
        if (!planetsByNavamshaHouse.getOrDefault(navamshaAscendantHouse, new ArrayList<>()).contains("Ascendant")) {
            planetsByNavamshaHouse.computeIfAbsent(navamshaAscendantHouse, k -> new ArrayList<>()).add(0, "Ascendant");
        }

        String dateText = String.format("%04d-%02d-%02d", response.getInput().getYear(), response.getInput().getMonth(), response.getInput().getDate());
        String timeText = String.format("%02d:%02d:%02d", response.getInput().getHours(), response.getInput().getMinutes(), response.getInput().getSeconds());
        String timezoneText = String.format("UTC%+.1f", response.getInput().getTimezone());

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"hi\">\n");
        html.append("<head>\n");
        html.append("<meta charset=\"UTF-8\" />\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>Kundli Report</title>\n");
        html.append("<style>\n");
        html.append("*{box-sizing:border-box}\n");
        html.append("html,body{width:100%;height:100%;margin:0;padding:0;font-size:16px;}\n");
        html.append("body{margin:0;background:#cdb68a;font-family: Calibre, Georgia, \"Times New Roman\", serif;}\n");
        html.append(".page{max-width:1200px;margin:20px auto;background:radial-gradient(circle,#f6e6c6,#e1c18a);border:10px solid #8a5a2b;box-shadow:0 0 30px rgba(0,0,0,.45);padding:25px;width:100%;}\n");
        html.append(".header{display:grid;grid-template-columns:80px 1fr 80px;align-items:center;border-bottom:2px solid #8a5a2b;padding-bottom:15px;}\n");
        html.append(".header .om{font-size:48px;color:#8a5a2b;}\n");
        html.append(".header .title{text-align:center;}\n");
        html.append(".header .title h1{margin:0;font-size:32px;}\n");
        html.append(".header .title h2{margin:6px 0 0;font-weight:normal;font-size:18px;}\n");
        html.append(".header .sun{font-size:48px;text-align:right;}\n");
        html.append(".info{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-top:20px;font-size:16px;line-height:1.7;}\n");
        html.append(".main{display:grid;grid-template-columns:1fr 1fr;gap:25px;margin-top:25px;width:100%;}\n");
        html.append(".charts{display:grid;grid-template-columns:1fr 1fr;gap:25px;margin-top:25px;width:100%;}\n");
        html.append(".data-section{display:grid;grid-template-columns:1fr 1fr;gap:25px;margin-top:25px;width:100%;}\n");
        html.append(".box{border:3px solid #8a5a2b;padding:15px;background:rgba(255,255,255,.18);width:100%;}\n");
        html.append(".box h3{text-align:center;margin:0 0 10px;font-size:20px;}\n");
        html.append("svg{width:100%;height:auto;max-width:100%;}\n");
        html.append("table{width:100%;border-collapse:collapse;font-size:16px;}\n");
        html.append("th,td{border:1px solid #8a5a2b;padding:8px;font-size:16px;}\n");
        html.append(".footer{margin-top:20px;text-align:center;font-size:16px;}\n");
        html.append("@media screen and (max-width:1200px){.page{margin:10px;padding:20px;}}\n");
        html.append("@media screen and (max-width:900px){.page{margin:5px;padding:15px;border-width:8px;}.charts{grid-template-columns:1fr !important;}.data-section{grid-template-columns:1fr !important;}.main{grid-template-columns:1fr !important;}}\n");
        html.append("@media screen and (max-width:768px){.page{padding:12px;margin:0;border-width:6px;}.header{grid-template-columns:60px 1fr 60px;gap:10px;}.header .om{font-size:40px;}.header .sun{font-size:40px;}.header .title h1{font-size:26px;margin:0;}.header .title h2{font-size:16px;margin:3px 0 0;}.info{grid-template-columns:1fr;gap:12px;font-size:16px;}.charts{grid-template-columns:1fr !important;gap:15px;}.data-section{grid-template-columns:1fr !important;gap:15px;}.box{padding:10px;}.box h3{font-size:18px;margin:0 0 8px;}.box svg{max-height:300px;}.main{grid-template-columns:1fr !important;}table{font-size:16px;}th,td{padding:6px;font-size:16px;}}\n");
        html.append("@media screen and (max-width:480px){.page{padding:10px;margin:0;border-width:4px;}.header{grid-template-columns:50px 1fr 50px;gap:8px;}.header .om{font-size:36px;}.header .sun{font-size:36px;}.header .title h1{font-size:22px;margin:0;}.header .title h2{font-size:14px;margin:2px 0 0;}.info{grid-template-columns:1fr;gap:8px;font-size:16px;}.charts{grid-template-columns:1fr !important;gap:12px;}.data-section{grid-template-columns:1fr !important;gap:12px;}.box{padding:8px;}.box h3{font-size:16px;margin:0 0 6px;}.box svg{max-height:250px;}.main{grid-template-columns:1fr !important;}table{font-size:16px;}th,td{padding:4px;font-size:16px;}}\n");
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class=\"page\">\n");
        html.append("<div class=\"header\">\n");
        html.append("  <div class=\"om\">ॐ</div>\n");
        html.append("  <div class=\"title\"><h1>कुंडली / Kundli</h1><h2>Kundli Report</h2></div>\n");
        html.append("  <div class=\"sun\">☀️</div>\n");
        html.append("</div>\n");

        html.append("<div class=\"info\">\n");
        html.append("  <div>");
        html.append("<b>Date:</b> ").append(dateText).append("<br>");
        html.append("<b>Time:</b> ").append(timeText).append("<br>");
        html.append("<b>Time Zone:</b> ").append(timezoneText).append("<br>");
        html.append("<b>Latitude:</b> ").append(String.format("%.6f", response.getInput().getLatitude())).append("<br>");
        html.append("<b>Longitude:</b> ").append(String.format("%.6f", response.getInput().getLongitude()));
        html.append("</div>\n");
        html.append("  <div>");
        html.append("<b>Rasi (Moon Sign):</b> ").append(moonSign != null ? SIGN_MAP.get(moonSign) : "-").append("<br>");
        html.append("<b>Ascendant (Lagna):</b> ").append(ascendantSign != null ? SIGN_MAP.get(ascendantSign) : "-").append("<br>");
        html.append("<b>Sun Sign:</b> ").append(sunSign != null ? SIGN_MAP.get(sunSign) : "-").append("<br>");
        html.append("<b>Ayanamsa:</b> ").append(ayanamsaValue != null ? String.format("%.4f", ayanamsaValue) : "-");
        html.append("</div>\n");
        html.append("</div>\n");

        // Charts section (Kundli, Navamsha, Transit)
        html.append("<div class=\"charts\">\n");
        html.append("<div>\n");
        html.append("<div class=\"box\">\n");
        html.append("<h3>Kundli Birth Chart</h3>\n");

        html.append("<svg width=\"100%\" viewBox=\"0 0 400 400\" xmlns=\"http://www.w3.org/2000/svg\">\n");
        html.append("  <rect x=\"0\" y=\"0\" width=\"400\" height=\"400\" fill=\"none\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"0\" y1=\"0\" x2=\"400\" y2=\"400\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"400\" y1=\"0\" x2=\"0\" y2=\"400\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"200\" y1=\"0\" x2=\"400\" y2=\"200\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"400\" y1=\"200\" x2=\"200\" y2=\"400\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"200\" y1=\"400\" x2=\"0\" y2=\"200\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"0\" y1=\"200\" x2=\"200\" y2=\"0\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <text x=\"200\" y=\"200\" font-size=\"180\" fill=\"#8a5a2b\" opacity=\"0.06\" text-anchor=\"middle\" dominant-baseline=\"middle\">ॐ</text>\n");
        html.append("  <g fill=\"#3b2414\" font-size=\"12\" font-weight=\"bold\" text-anchor=\"middle\" dominant-baseline=\"middle\">\n");
        html.append("    <text x=\"325\" y=\"100\">1</text>\n");
        html.append("    <text x=\"300\" y=\"75\">2</text>\n");
        html.append("    <text x=\"200\" y=\"175\">3</text>\n");
        html.append("    <text x=\"100\" y=\"75\">4</text>\n");
        html.append("    <text x=\"75\" y=\"100\">5</text>\n");
        html.append("    <text x=\"175\" y=\"200\">6</text>\n");
        html.append("    <text x=\"200\" y=\"225\">9</text>\n");
        html.append("    <text x=\"75\" y=\"300\">7</text>\n");
        html.append("    <text x=\"100\" y=\"325\">8</text>\n");
        html.append("    <text x=\"300\" y=\"260\">9</text>\n");
        html.append("    <text x=\"300\" y=\"320\">10</text>\n");
        html.append("    <text x=\"325\" y=\"300\">11</text>\n");
        html.append("    <text x=\"225\" y=\"200\">12</text>\n");
        html.append("  </g>\n");

        // Planet labels per house (alignment coordinates)
        Map<Integer, int[]> houseCoords = new HashMap<>();
        houseCoords.put(1, new int[]{360, 85});
        houseCoords.put(2, new int[]{285, 20});
        houseCoords.put(3, new int[]{180, 76});
        houseCoords.put(4, new int[]{70, 25});
        houseCoords.put(5, new int[]{10, 90});
        houseCoords.put(6, new int[]{70, 180});
        houseCoords.put(7, new int[]{10, 255});
        houseCoords.put(8, new int[]{70, 365});
        houseCoords.put(9, new int[]{180, 300});
        houseCoords.put(10, new int[]{265, 365});
        houseCoords.put(11, new int[]{360, 300});
        houseCoords.put(12, new int[]{265, 180});

        html.append("  <g fill=\"#000\" font-size=\"12\" font-weight=\"bold\">\n");
        for (int sign = 1; sign <= 12; sign++) {
            int[] coord = houseCoords.get(sign);
            if (coord == null) {
                continue;
            }
            List<String> planets = planetsBySign.getOrDefault(sign, new ArrayList<>());
            if (planets.size() > 1) {
                // Multiple planets: use tspan for vertical stacking
                html.append("    <text x=\"").append(coord[0]).append("\" y=\"").append(coord[1])
                        .append("\" font-size=\"12\" font-weight=\"bold\" text-anchor=\"start\" dominant-baseline=\"middle\">\n");
                for (int i = 0; i < planets.size(); i++) {
                    String planetName = planets.get(i);
                    String abbr = planetAbbr.getOrDefault(planetName, planetName.substring(0, Math.min(3, planetName.length())));
                    html.append("      <tspan x=\"").append(coord[0]).append("\" dy=\"").append(i == 0 ? "0" : "8").append("\">").append(abbr).append("</tspan>\n");
                }
                html.append("    </text>\n");
            } else if (planets.size() == 1) {
                // Single planet: simple text
                String planetName = planets.get(0);
                String abbr = planetAbbr.getOrDefault(planetName, planetName.substring(0, Math.min(3, planetName.length())));
                html.append("    <text x=\"").append(coord[0]).append("\" y=\"").append(coord[1]).append("\">").append(abbr).append("</text>\n");
            }
        }
        html.append("  </g>\n");
        html.append("</svg>\n");
        html.append("</div>\n");

        html.append("<div class=\"box\">\n");
        html.append("<h3>Navamsha  Chart</h3>\n");
        html.append("<svg width=\"100%\" viewBox=\"0 0 400 400\" xmlns=\"http://www.w3.org/2000/svg\">\n");
        html.append("  <rect x=\"0\" y=\"0\" width=\"400\" height=\"400\" fill=\"none\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"0\" y1=\"0\" x2=\"400\" y2=\"400\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"400\" y1=\"0\" x2=\"0\" y2=\"400\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"200\" y1=\"0\" x2=\"400\" y2=\"200\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"400\" y1=\"200\" x2=\"200\" y2=\"400\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"200\" y1=\"400\" x2=\"0\" y2=\"200\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"0\" y1=\"200\" x2=\"200\" y2=\"0\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <text x=\"200\" y=\"200\" font-size=\"180\" fill=\"#8a5a2b\" opacity=\"0.06\" text-anchor=\"middle\" dominant-baseline=\"middle\">ॐ</text>\n");
        html.append("  <g fill=\"#3b2414\" font-size=\"12\" font-weight=\"bold\" text-anchor=\"middle\" dominant-baseline=\"middle\">\n");
        html.append("    <text x=\"325\" y=\"100\">1</text>\n");
        html.append("    <text x=\"300\" y=\"75\">2</text>\n");
        html.append("    <text x=\"200\" y=\"175\">3</text>\n");
        html.append("    <text x=\"100\" y=\"75\">4</text>\n");
        html.append("    <text x=\"75\" y=\"100\">5</text>\n");
        html.append("    <text x=\"175\" y=\"200\">6</text>\n");
        html.append("    <text x=\"200\" y=\"225\">9</text>\n");
        html.append("    <text x=\"75\" y=\"300\">7</text>\n");
        html.append("    <text x=\"100\" y=\"325\">8</text>\n");
        html.append("    <text x=\"300\" y=\"260\">9</text>\n");
        html.append("    <text x=\"300\" y=\"320\">10</text>\n");
        html.append("    <text x=\"325\" y=\"300\">11</text>\n");
        html.append("    <text x=\"225\" y=\"200\">12</text>\n");
        html.append("  </g>\n");
        html.append("  <g fill=\"#000\" font-size=\"12\" font-weight=\"bold\">\n");
        for (int house = 1; house <= 12; house++) {
            int[] coord = houseCoords.get(house);
            if (coord == null) {
                continue;
            }
            List<String> planets = planetsByNavamshaHouse.getOrDefault(house, new ArrayList<>());
            if (planets.size() > 1) {
                // Multiple planets: use tspan for vertical stacking
                html.append("    <text x=\"").append(coord[0]).append("\" y=\"").append(coord[1])
                        .append("\" font-size=\"12\" font-weight=\"bold\" text-anchor=\"start\" dominant-baseline=\"middle\">\n");
                for (int i = 0; i < planets.size(); i++) {
                    String planetName = planets.get(i);
                    String abbr = planetAbbr.getOrDefault(planetName, planetName.substring(0, Math.min(3, planetName.length())));
                    html.append("      <tspan x=\"").append(coord[0]).append("\" dy=\"").append(i == 0 ? "0" : "8").append("\">").append(abbr).append("</tspan>\n");
                }
                html.append("    </text>\n");
            } else if (planets.size() == 1) {
                // Single planet: simple text
                String planetName = planets.get(0);
                String abbr = planetAbbr.getOrDefault(planetName, planetName.substring(0, Math.min(3, planetName.length())));
                html.append("    <text x=\"").append(coord[0]).append("\" y=\"").append(coord[1]).append("\">").append(abbr).append("</text>\n");
            }
        }
        html.append("  </g>\n");
        html.append("</svg>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        html.append("</div>\n");  // Close charts section

        // Second row of charts (Transit chart)
        html.append("<div class=\"charts\">\n");
        html.append("<div>\n");
        
        // Transit Chart (Today from Lagna)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> transitData = (List<Map<String, Object>>) outputMap.get("transit");
        Map<String, Object> transitNamedPlanets = transitData.get(1);
        
        // Build planetsByTransitHouse
        Map<Integer, List<String>> planetsByTransitHouse = new HashMap<>();
        for (Map.Entry<String, Object> entry : transitNamedPlanets.entrySet()) {
            String planetName = entry.getKey();
            if ("Ascendant".equals(planetName)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> planetData = (Map<String, Object>) entry.getValue();
            Integer transitHouse = (Integer) planetData.get("transit_house");
            if (transitHouse != null) {
                planetsByTransitHouse.computeIfAbsent(transitHouse, k -> new ArrayList<>()).add(planetName);
            }
        }
        
        // Add Ascendant to house 1
        if (!planetsByTransitHouse.getOrDefault(1, new ArrayList<>()).contains("Ascendant")) {
            planetsByTransitHouse.computeIfAbsent(1, k -> new ArrayList<>()).add(0, "Ascendant");
        }
        
        html.append("<div class=\"box\">\n");
        html.append("<h3>Transit Chart (Today from Lagna)</h3>\n");
        html.append("<svg width=\"100%\" viewBox=\"0 0 400 400\" xmlns=\"http://www.w3.org/2000/svg\">\n");
        html.append("  <rect x=\"0\" y=\"0\" width=\"400\" height=\"400\" fill=\"none\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"0\" y1=\"0\" x2=\"400\" y2=\"400\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"400\" y1=\"0\" x2=\"0\" y2=\"400\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"200\" y1=\"0\" x2=\"400\" y2=\"200\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"400\" y1=\"200\" x2=\"200\" y2=\"400\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"200\" y1=\"400\" x2=\"0\" y2=\"200\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <line x1=\"0\" y1=\"200\" x2=\"200\" y2=\"0\" stroke=\"#5b3a1c\" stroke-width=\"3\"/>\n");
        html.append("  <text x=\"200\" y=\"200\" font-size=\"180\" fill=\"#8a5a2b\" opacity=\"0.06\" text-anchor=\"middle\" dominant-baseline=\"middle\">ॐ</text>\n");
        html.append("  <g fill=\"#3b2414\" font-size=\"12\" font-weight=\"bold\" text-anchor=\"middle\" dominant-baseline=\"middle\">\n");
        html.append("    <text x=\"325\" y=\"100\">1</text>\n");
        html.append("    <text x=\"300\" y=\"75\">2</text>\n");
        html.append("    <text x=\"200\" y=\"175\">3</text>\n");
        html.append("    <text x=\"100\" y=\"75\">4</text>\n");
        html.append("    <text x=\"75\" y=\"100\">5</text>\n");
        html.append("    <text x=\"175\" y=\"200\">6</text>\n");
        html.append("    <text x=\"200\" y=\"225\">9</text>\n");
        html.append("    <text x=\"75\" y=\"300\">7</text>\n");
        html.append("    <text x=\"100\" y=\"325\">8</text>\n");
        html.append("    <text x=\"300\" y=\"260\">9</text>\n");
        html.append("    <text x=\"300\" y=\"320\">10</text>\n");
        html.append("    <text x=\"325\" y=\"300\">11</text>\n");
        html.append("    <text x=\"225\" y=\"200\">12</text>\n");
        html.append("  </g>\n");
        html.append("  <g fill=\"#000\" font-size=\"12\" font-weight=\"bold\">\n");
        for (int house = 1; house <= 12; house++) {
            int[] coord = houseCoords.get(house);
            if (coord == null) {
                continue;
            }
            List<String> planets = planetsByTransitHouse.getOrDefault(house, new ArrayList<>());
            if (planets.size() > 1) {
                // Multiple planets: use tspan for vertical stacking
                html.append("    <text x=\"").append(coord[0]).append("\" y=\"").append(coord[1])
                        .append("\" font-size=\"12\" font-weight=\"bold\" text-anchor=\"start\" dominant-baseline=\"middle\">\n");
                for (int i = 0; i < planets.size(); i++) {
                    String planetName = planets.get(i);
                    String abbr = planetAbbr.getOrDefault(planetName, planetName.substring(0, Math.min(3, planetName.length())));
                    html.append("      <tspan x=\"").append(coord[0]).append("\" dy=\"").append(i == 0 ? "0" : "8").append("\">").append(abbr).append("</tspan>\n");
                }
                html.append("    </text>\n");
            } else if (planets.size() == 1) {
                // Single planet: simple text
                String planetName = planets.get(0);
                String abbr = planetAbbr.getOrDefault(planetName, planetName.substring(0, Math.min(3, planetName.length())));
                html.append("    <text x=\"").append(coord[0]).append("\" y=\"").append(coord[1]).append("\">").append(abbr).append("</text>\n");
            }
        }
        html.append("  </g>\n");
        html.append("</svg>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        html.append("</div>\n");  // Close charts section

        // Data section (Planetary Positions and Astrological Data)
        html.append("<div class=\"data-section\">\n");
        html.append("<div>\n");
        html.append("  <div class=\"box\">\n");
        html.append("    <h3>Planetary Positions</h3>\n");
        html.append("    <table>\n");
        html.append("      <tr><th>Planet</th><th>Sign</th><th>Degree</th><th>House</th></tr>\n");
        for (Map.Entry<String, Object> entry : namedPlanets.entrySet()) {
            String planetName = entry.getKey();
            if ("Ascendant".equals(planetName)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> planetData = (Map<String, Object>) entry.getValue();
            Integer sign = (Integer) planetData.get("current_sign");
            Double normDeg = (Double) planetData.get("normDegree");
            Integer houseNum = (Integer) planetData.get("house_number");
            html.append("      <tr><td>").append(planetName).append("</td><td>")
                    .append(sign != null ? SIGN_MAP.get(sign) : "-")
                    .append("</td><td>")
                    .append(normDeg != null ? String.format("%.2f°", normDeg) : "-")
                    .append("</td><td>")
                    .append(houseNum != null ? houseNum : "-")
                    .append("</td></tr>\n");
        }
        html.append("    </table>\n");
        html.append("  </div>\n");

        html.append("  <div class=\"box\" style=\"margin-top:20px;\">\n");
        html.append("    <h3>Astrological Data</h3>\n");
        html.append("    Ascendant: ").append(ascendantSign != null ? SIGN_MAP.get(ascendantSign) : "-")
                .append(ascendantData != null ? String.format(" %.2f°", (Double) ascendantData.get("normDegree")) : "").append("<br>\n");
        html.append("    Sun Sign: ").append(sunSign != null ? SIGN_MAP.get(sunSign) : "-").append("<br>\n");
        html.append("    Moon Sign: ").append(moonSign != null ? SIGN_MAP.get(moonSign) : "-").append("<br>\n");
        html.append("    Ayanamsa: ").append(ayanamsaValue != null ? String.format("%.4f", ayanamsaValue) : "-").append("<br>\n");
        html.append("  </div>\n");
        html.append("</div>\n");

        html.append("</div>\n");
        html.append("<div class=\"footer\">© Kundli Report • Traditional Theme</div>\n");
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }
}
