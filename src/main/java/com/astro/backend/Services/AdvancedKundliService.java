package com.astro.backend.Services;

import com.astro.backend.ResponseDTO.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import swisseph.SweConst;
import swisseph.SweDate;
import swisseph.SwissEph;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedKundliService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final SwissEph swissEph = new SwissEph("libs/ephe");

    private static final Map<Integer, String> RASHI_MAP = Map.ofEntries(
            Map.entry(0, "Aries"), Map.entry(1, "Taurus"), Map.entry(2, "Gemini"),
            Map.entry(3, "Cancer"), Map.entry(4, "Leo"), Map.entry(5, "Virgo"),
            Map.entry(6, "Libra"), Map.entry(7, "Scorpio"), Map.entry(8, "Sagittarius"),
            Map.entry(9, "Capricorn"), Map.entry(10, "Aquarius"), Map.entry(11, "Pisces")
    );

    private static final Map<String, Integer> RASHI_INDEX_MAP = Map.ofEntries(
            Map.entry("Aries", 0), Map.entry("Taurus", 1), Map.entry("Gemini", 2),
            Map.entry("Cancer", 3), Map.entry("Leo", 4), Map.entry("Virgo", 5),
            Map.entry("Libra", 6), Map.entry("Scorpio", 7), Map.entry("Sagittarius", 8),
            Map.entry("Capricorn", 9), Map.entry("Aquarius", 10), Map.entry("Pisces", 11)
        );

    private final RemedyRecommendationService remedyRecommendationService;
    private final DashaCalculationService dashaCalculationService;

    private static final String[] NAKSHATRAS = {
            "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashirsha", "Ardra", "Punarvasu",
            "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta",
            "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha",
            "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada",
            "Uttara Bhadrapada", "Revati"
    };

        private static final String[] TITHI_NAMES = {
            "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami",
            "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
            "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Purnima",
            "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami",
            "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
            "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Amavasya"
        };

        private static final String[] TITHI_MEANINGS = {
            "New beginnings and fresh starts",
            "Growth, balance, and steady progress",
            "Creativity, courage, and initiative",
            "Overcoming obstacles and building focus",
            "Learning, purification, and harmony",
            "Discipline, service, and resilience",
            "Vitality, movement, and success",
            "Intensity, transformation, and inner strength",
            "Completion, endurance, and determination",
            "Accomplishment, leadership, and stability",
            "Purification, devotion, and clarity",
            "Gratitude, service, and refinement",
            "Strength, confidence, and achievement",
            "Protection, power, and deep insight",
            "Wholeness, fulfillment, and abundance",
            "Reset, renewal, and new direction",
            "Expansion, support, and grounding",
            "Creativity, communication, and action",
            "Removing blocks and preparing change",
            "Nurturing, learning, and consistency",
            "Duty, discipline, and perseverance",
            "Momentum, courage, and progress",
            "Introspection, transformation, and release",
            "Reflection, completion, and closure",
            "Achievement, structure, and resolve",
            "Spiritual focus, fasting, and alignment",
            "Giving, gratitude, and balance",
            "Ambition, strength, and completion",
            "Letting go, protection, and inner power",
            "Rest, introspection, and renewal"
        };

        private static final Map<Integer, String> PADA_MEANINGS = Map.of(
            1, "Initiation and direction",
            2, "Growth and expansion",
            3, "Refinement and effort",
            4, "Stability and culmination"
        );

    /**
     * Calculate today's Lagna (Ascendant) chart
     */
    public Map<String, Object> calculateTodayLagna(double lat, double lon) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int yyyy = now.getYear();
            int mm = now.getMonthValue();
            int dd = now.getDayOfMonth();
            double time = now.getHour() + now.getMinute() / 60.0 + now.getSecond() / 3600.0;
            
            // Convert IST to UT
            double[] dateTimeUT = convertToUT(yyyy, mm, dd, time, 5.5);
            int yyyyUT = (int) dateTimeUT[0];
            int mmUT = (int) dateTimeUT[1];
            int ddUT = (int) dateTimeUT[2];
            double timeUT = dateTimeUT[3];
            
            SweDate sd = new SweDate(yyyyUT, mmUT, ddUT, timeUT);
            double julDay = sd.getJulDay();
            
            // Set sidereal mode with Lahiri Ayanamsa
            swissEph.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0, 0);
            double ayanamsaValue = swissEph.swe_get_ayanamsa(julDay);
            
            // Calculate ascendant
            double ascendantLong = getAscendantLongitude(julDay, lat, lon);
            int ascRashi = (int) (ascendantLong / 30);
            double ascDegree = ascendantLong % 30;
            String lagna = RASHI_MAP.get(ascRashi);
            
            // Calculate houses
            Map<Integer, String> houses = calculateHouses(julDay, lat, lon);
            
            // Calculate planetary positions
            List<PlanetPosition> planets = calculatePlanetaryPositions(julDay);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("dateTime", now.toString());
            result.put("lagna", lagna);
            result.put("ascendantDegree", String.format("%.2f", ascDegree));
            result.put("ascendantLongitude", ascendantLong);
            result.put("houses", houses);
            result.put("planets", planets);
            result.put("latitude", lat);
            result.put("longitude", lon);
            result.put("ayanamsa", String.format("Lahiri %.6f", ayanamsaValue));
            result.put("julianDay", julDay);
            
            return result;
        } catch (Exception e) {
            log.error("Error calculating today's lagna", e);
            throw new RuntimeException("Failed to calculate today's Lagna: " + e.getMessage());
        }
    }

    /**
     * Calculate today's Panchang
     */
    public Map<String, Object> calculateTodayPanchang(double lat, double lon) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int yyyy = now.getYear();
            int mm = now.getMonthValue();
            int dd = now.getDayOfMonth();
            double time = now.getHour() + now.getMinute() / 60.0 + now.getSecond() / 3600.0;
            
            // Convert IST to UT
            double[] dateTimeUT = convertToUT(yyyy, mm, dd, time, 5.5);
            int yyyyUT = (int) dateTimeUT[0];
            int mmUT = (int) dateTimeUT[1];
            int ddUT = (int) dateTimeUT[2];
            double timeUT = dateTimeUT[3];
            
            SweDate sd = new SweDate(yyyyUT, mmUT, ddUT, timeUT);
            double julDay = sd.getJulDay();
            
            // Set sidereal mode with Lahiri Ayanamsa
            swissEph.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0, 0);
            double ayanamsaValue = swissEph.swe_get_ayanamsa(julDay);
            
            // Calculate planetary positions
            List<PlanetPosition> planets = calculatePlanetaryPositions(julDay);
            
            double sunLong = getPlanetLongitude(planets, "Sun");
            double moonLong = getPlanetLongitude(planets, "Moon");
            
            // Calculate Tithi
            double diff = (moonLong - sunLong + 360.0) % 360.0;
            int tithiNumber = (int) Math.floor(diff / 12.0) + 1;
            // Tithi details are built from master mappings below
            
            // Calculate Nakshatra
            String nakshatra = getNakshatra(moonLong);
            int padaNumber = getPadaNumber(moonLong);
            int nakshatraNumber = ((int) Math.floor(moonLong / (360.0 / 27.0))) + 1;
            
            // Calculate Yoga
            int yogaNumber = (int) Math.floor(((sunLong + moonLong) % 360.0) / (360.0 / 27.0)) + 1;
            String[] yogaNames = {
                "Vishkambha", "Priti", "Ayushman", "Saubhagya", "Shobhana",
                "Atiganda", "Sukarman", "Dhriti", "Shula", "Ganda",
                "Vriddhi", "Dhruva", "Vyaghata", "Harshana", "Vajra",
                "Siddhi", "Vyatipata", "Variyan", "Parigha", "Shiva",
                "Siddha", "Sadhya", "Shubha", "Shukla", "Brahma",
                "Indra", "Vaidhriti"
            };
            String yogaName = yogaNames[(yogaNumber - 1) % 27];
            
            // Calculate Karana
            int karanaNumber = (int) Math.floor(diff / 6.0) + 1;
            String[] karanaNames = {
                "Bava", "Balava", "Kaulava", "Taitila", "Gara",
                "Vanija", "Vishti", "Shakuni", "Chatushpada", "Naga", "Kimstughna"
            };
            String karanaName = karanaNumber <= 7 ? karanaNames[(karanaNumber - 1) % 7] : karanaNames[7 + (karanaNumber - 8) % 4];
            
            // Calculate Rashi (Sun sign)
            int sunRashi = (int) (sunLong / 30);
            String currentRashi = RASHI_MAP.get(sunRashi);
            int moonRashi = (int) (moonLong / 30);
            String moonRashiName = RASHI_MAP.get(moonRashi);
            
            // Day of week
            String dayOfWeek = now.getDayOfWeek().toString();
            String weekdayLord = getWeekdayLord(now.getDayOfWeek());

            // Derived phase/progress values
            double tithiProgressPercent = ((diff % 12.0) / 12.0) * 100.0;
            double nakshatraSegment = 360.0 / 27.0;
            double nakshatraProgressPercent = ((moonLong % nakshatraSegment) / nakshatraSegment) * 100.0;
            double moonPhaseAngle = diff;
            double illuminationPercent = 0.5 * (1 - Math.cos(Math.toRadians(moonPhaseAngle))) * 100.0;
            String moonPhaseName = tithiNumber <= 15 ? "Shukla Paksha (Waxing)" : "Krishna Paksha (Waning)";
            
            Map<String, Object> panchang = new LinkedHashMap<>();
            panchang.put("date", String.format("%02d-%02d-%04d", dd, mm, yyyy));
            panchang.put("dateTime", now.toString());
            panchang.put("dayOfWeek", dayOfWeek);
            panchang.put("weekdayLord", weekdayLord);
            panchang.put("tithi", buildTithiDetails(tithiNumber));
            panchang.put("tithiNumber", tithiNumber);
            panchang.put("nakshatra", buildNakshatraDetails(nakshatra, padaNumber));
            panchang.put("nakshatraNumber", nakshatraNumber);
            panchang.put("padaNumber", padaNumber);
            panchang.put("yoga", Map.of(
                "number", yogaNumber,
                "name", yogaName
            ));
            panchang.put("yogaNumber", yogaNumber);
            panchang.put("karana", Map.of(
                "number", karanaNumber,
                "name", karanaName
            ));
            panchang.put("karanaNumber", karanaNumber);
            panchang.put("rashi", currentRashi);
            panchang.put("sunLongitude", sunLong);
            panchang.put("moonLongitude", moonLong);
            panchang.put("julianDay", julDay);
            panchang.put("latitude", lat);
            panchang.put("longitude", lon);
            panchang.put("phase", Map.of(
                    "name", moonPhaseName,
                    "angle", roundValue(moonPhaseAngle, 4),
                    "illuminationPercent", roundValue(illuminationPercent, 2)
            ));
            panchang.put("progress", Map.of(
                    "tithiPercent", roundValue(tithiProgressPercent, 2),
                    "nakshatraPercent", roundValue(nakshatraProgressPercent, 2)
            ));
            panchang.put("sunPosition", Map.of(
                    "rashi", currentRashi,
                    "rashiNumber", sunRashi + 1,
                    "degreeInRashi", roundValue(sunLong % 30.0, 4),
                    "absoluteLongitude", roundValue(sunLong, 6)
            ));
            panchang.put("moonPosition", Map.of(
                    "rashi", moonRashiName,
                    "rashiNumber", moonRashi + 1,
                    "degreeInRashi", roundValue(moonLong % 30.0, 4),
                    "absoluteLongitude", roundValue(moonLong, 6)
            ));
            panchang.put("meta", Map.of(
                    "timezone", "Asia/Kolkata",
                    "utcOffsetHours", 5.5,
                    "ayanamsa", roundValue(ayanamsaValue, 6),
                    "calculationEngine", "Swiss Ephemeris (Lahiri)"
            ));
            
            return panchang;
        } catch (Exception e) {
            log.error("Error calculating today's panchang", e);
            throw new RuntimeException("Failed to calculate today's Panchang: " + e.getMessage());
        }
    }

    private String getWeekdayLord(java.time.DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case SUNDAY -> "Sun";
            case MONDAY -> "Moon";
            case TUESDAY -> "Mars";
            case WEDNESDAY -> "Mercury";
            case THURSDAY -> "Jupiter";
            case FRIDAY -> "Venus";
            case SATURDAY -> "Saturn";
        };
    }

    private double roundValue(double value, int scale) {
        double base = Math.pow(10, scale);
        return Math.round(value * base) / base;
    }

    /**
     * Generate full Kundli with all divisional charts and doshas
     */
    public FullKundliResponse generateFullKundli(double lat, double lon, int dd, int mm, int yyyy, double time, String name, String originalTimeString, double timezoneOffset) {
        try {
            // Convert local time to UT (Universal Time)
            double[] dateTimeUT = convertToUT(yyyy, mm, dd, time, timezoneOffset);
            int yyyyUT = (int) dateTimeUT[0];
            int mmUT = (int) dateTimeUT[1];
            int ddUT = (int) dateTimeUT[2];
            double timeUT = dateTimeUT[3];
            
            SweDate sd = new SweDate(yyyyUT, mmUT, ddUT, timeUT);
            double julDay = sd.getJulDay();

            // Set sidereal mode with Lahiri Ayanamsa at the beginning
            swissEph.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0, 0);
            double ayanamsaValue = swissEph.swe_get_ayanamsa(julDay);

            List<PlanetPosition> planets = calculatePlanetaryPositions(julDay);
            Map<Integer, String> houses = calculateHouses(julDay, lat, lon);

            double ascendantLong = getAscendantLongitude(julDay, lat, lon);
            String lagna = RASHI_MAP.get((int) (ascendantLong / 30));

            String sunSign = getPlanetRashi(planets, "Sun");
            String moonSign = getPlanetRashi(planets, "Moon");
            String nakshatra = getNakshatra(getPlanetLongitude(planets, "Moon"));
            String pada = getPada(getPlanetLongitude(planets, "Moon"));

            // Calculate element distribution
            Map<String, Double> elements = calculateElements(planets, lagna);

            // Detect doshas
            DoshaDetection doshaDetection = detectAllDoshas(julDay, planets, lagna);

                // Calculate yogas
                List<String> yogas = detectYogas(planets, lagna);

                List<String> inauspiciousYogas = detectInauspiciousYogas(doshaDetection);
                String navamsaChart = buildDivisionalChartJson(planets, 9);
                String dashamsaChart = buildDivisionalChartJson(planets, 10);
                Map<String, Object> panchang = buildPanchang(julDay, planets, nakshatra, pada);
                DashaResponse vimshottariDasha = dashaCalculationService.calculateVimshottariDasha(
                    LocalDate.of(yyyy, mm, dd),
                    nakshatra,
                    getPlanetLongitude(planets, "Moon")
                );
                Map<String, Object> remedies = buildRemedies(doshaDetection, planets);
                String place = buildPlace(lat, lon);
                String timezone = TimeZone.getDefault().getID();
                double julianDay = julDay;

        return FullKundliResponse.builder()
        .chartId(null)
        .name(name)
        .dateOfBirth(String.format("%02d-%02d-%04d", dd, mm, yyyy))
        .timeOfBirth(originalTimeString)  // Use original time string instead of decimal
        .latitude(lat)
        .longitude(lon)

        .lagna(lagna)
        .sunSign(sunSign)
        .moonSign(moonSign)
        .nakshatra(nakshatra)
        .pada(pada)

        .planets(planets)
        .houses(houses)
        .navamsaChart(navamsaChart)
        .dashamsa(dashamsaChart)

        .panchang(panchang)

        .mangalDosha(doshaDetection.mangalDosha)
        .kaalSarpDosha(doshaDetection.kaalSarpDosha)
        .pitruDosha(doshaDetection.pitruDosha)
        .grahanDosha(doshaDetection.grahanDosha)

        .auspiciousYogas(yogas)
        .inauspiciousYogas(inauspiciousYogas)

        .elements(elements)

        .vimshottariDasha(vimshottariDasha)

        .remedies(remedies)

        .overallMessage(generateOverallMessage(doshaDetection))
        .healthScore(calculateHealthScore(doshaDetection))

        .locationMeta(FullKundliResponse.LocationMeta.builder()
                .place(place)
                .timezone(timezone)
            .ayanamsa(String.format("Lahiri %.6f", ayanamsaValue))
                .julianDay(julianDay)
                .build())

        .build();

        } catch (Exception e) {
            log.error("Error generating full kundli", e);
            throw new RuntimeException("Failed to generate Kundli: " + e.getMessage());
        }
    }

    private Map<String, Object> buildPanchang(
            double julDay,
            List<PlanetPosition> planets,
            String nakshatra,
            String pada
    ) {
        double sunLong = getPlanetLongitude(planets, "Sun");
        double moonLong = getPlanetLongitude(planets, "Moon");

        double diff = (moonLong - sunLong + 360.0) % 360.0;
        int tithiNumber = (int) Math.floor(diff / 12.0) + 1;
        int yogaNumber = (int) Math.floor(((sunLong + moonLong) % 360.0) / (360.0 / 27.0)) + 1;
        int karanaNumber = (int) Math.floor(diff / 6.0) + 1;
        int padaNumber = getPadaNumber(moonLong);

        Map<String, Object> panchang = new LinkedHashMap<>();
        panchang.put("tithiNumber", tithiNumber);
        panchang.put("nakshatra", nakshatra);
        panchang.put("pada", pada);
        panchang.put("tithi", buildTithiDetails(tithiNumber));
        panchang.put("nakshatraDetails", buildNakshatraDetails(nakshatra, padaNumber));
        panchang.put("yogaNumber", yogaNumber);
        panchang.put("karanaNumber", karanaNumber);
        panchang.put("julianDay", julDay);

        return panchang;
    }

    private String buildDivisionalChartJson(List<PlanetPosition> planets, int division) {
        Map<String, String> divisional = new LinkedHashMap<>();
        for (PlanetPosition planet : planets) {
            double longitude = planet.getLongitude();
            int signIndex = (int) Math.floor((longitude * division) / 30.0) % 12;
            divisional.put(planet.getPlanet(), RASHI_MAP.get(signIndex));
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(divisional);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build divisional chart", e);
        }
    }

    private List<String> detectInauspiciousYogas(DoshaDetection doshaDetection) {
        List<String> inauspicious = new ArrayList<>();
        if (isPresent(doshaDetection.mangalDosha)) {
            inauspicious.add("Mangal Dosha");
        }
        if (isPresent(doshaDetection.kaalSarpDosha)) {
            inauspicious.add("Kaal Sarp Dosha");
        }
        if (isPresent(doshaDetection.pitruDosha)) {
            inauspicious.add("Pitru Dosha");
        }
        if (isPresent(doshaDetection.grahanDosha)) {
            inauspicious.add("Grahan Dosha");
        }
        return inauspicious;
    }

    private Map<String, Object> buildRemedies(DoshaDetection doshaDetection, List<PlanetPosition> planets) {
        Map<String, Boolean> doshas = new LinkedHashMap<>();
        doshas.put("Mangal Dosha", isPresent(doshaDetection.mangalDosha));
        doshas.put("Kaal Sarp Dosha", isPresent(doshaDetection.kaalSarpDosha));
        doshas.put("Pitru Dosha", isPresent(doshaDetection.pitruDosha));
        doshas.put("Grahan Dosha", isPresent(doshaDetection.grahanDosha));

        Map<String, String> planetRashis = new LinkedHashMap<>();
        for (PlanetPosition planet : planets) {
            planetRashis.put(planet.getPlanet(), planet.getRashi());
        }

        return remedyRecommendationService.generateRemedies(doshas, planetRashis);
    }

    private String buildPlace(double lat, double lon) {
        return String.format("Lat %.4f, Lon %.4f", lat, lon);
    }

    /**
     * Calculate planetary positions for all planets
     */
    private List<PlanetPosition> calculatePlanetaryPositions(double julDay) {
        int[] planets = {
                SweConst.SE_SUN, SweConst.SE_MOON, SweConst.SE_MARS,
                SweConst.SE_MERCURY, SweConst.SE_JUPITER, SweConst.SE_VENUS,
                SweConst.SE_SATURN, SweConst.SE_TRUE_NODE, SweConst.SE_CHIRON
        };

        List<PlanetPosition> planetPositions = new ArrayList<>();

        for (int p : planets) {
            double[] xx = new double[6];
            StringBuffer serr = new StringBuffer();

            // Use SEFLG_SIDEREAL for Vedic astrology (sidereal calculations)
            swissEph.swe_calc(julDay, p, SweConst.SEFLG_SIDEREAL, xx, serr);
            double lonDeg = xx[0];
            int rashi = (int) (lonDeg / 30);
            double degree = lonDeg % 30;

            planetPositions.add(PlanetPosition.builder()
                    .planet(getPlanetName(p))
                    .longitude(lonDeg)
                    .degree(degree)
                    .rashi(RASHI_MAP.get(rashi))
                    .speed(xx[3])
                    .retrograde(xx[3] < 0)
                    .build());
        }

        return planetPositions;
    }

    /**
     * Calculate 12 houses
     */
    private Map<Integer, String> calculateHouses(double julDay, double lat, double lon) {
        double[] cusps = new double[13];
        double[] ascmc = new double[10];

        // Use SEFLG_SIDEREAL for Vedic astrology
        swissEph.swe_houses(julDay, SweConst.SEFLG_SIDEREAL, lat, lon, 'P', cusps, ascmc);

        Map<Integer, String> houses = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            int rashi = (int) (cusps[i] / 30);
            houses.put(i, RASHI_MAP.get(rashi));
        }

        return houses;
    }

    /**
     * Detect all doshas
     */
    private DoshaDetection detectAllDoshas(double julDay, List<PlanetPosition> planets, String lagna) {
        DoshaDetection detection = new DoshaDetection();

        // Mangal Dosha
        detection.mangalDosha = detectMangalDosha(planets, lagna);

        // Kaal Sarp Dosha
        detection.kaalSarpDosha = detectKaalSarpDosha(planets);

        // Pitru Dosha
        detection.pitruDosha = detectPitruDosha(planets);

        // Grahan Dosha
        detection.grahanDosha = detectGrahanDosha(planets);

        return detection;
    }

    /**
     * Mangal Dosha - Mars in 1, 4, 7, 8, 12 houses or 2nd from Moon
     */
    private FullKundliResponse.Dosha detectMangalDosha(List<PlanetPosition> planets, String lagna) {
        PlanetPosition mars = planets.stream()
                .filter(p -> "Mars".equals(p.getPlanet()))
                .findFirst()
                .orElse(null);

        if (mars == null) {
            return FullKundliResponse.Dosha.builder()
                    .present(false)
                    .description("No Mangal Dosha")
                    .build();
        }

        // Simplified: Check if Mars is in certain houses
        // In real implementation, calculate exact house position
        boolean hasMangal = false;  // Simplified logic

        return FullKundliResponse.Dosha.builder()
                .present(hasMangal)
                .description(hasMangal ? "Mangal Dosha Detected" : "No Mangal Dosha")
                .remedyAdvice(hasMangal ? "Wear Red Coral, perform Hanuman Puja" : "")
                .build();
    }

    /**
     * Kaal Sarp Dosha - All planets between Rahu and Ketu
     */
    private FullKundliResponse.Dosha detectKaalSarpDosha(List<PlanetPosition> planets) {
        // Simplified detection
        return FullKundliResponse.Dosha.builder()
                .present(false)
                .description("No Kaal Sarp Dosha detected")
                .build();
    }

    /**
     * Pitru Dosha - Various conditions based on Sun and Rahu
     */
    private FullKundliResponse.Dosha detectPitruDosha(List<PlanetPosition> planets) {
        return FullKundliResponse.Dosha.builder()
                .present(false)
                .description("No Pitru Dosha detected")
                .build();
    }

    /**
     * Grahan Dosha - Sun or Moon with Rahu/Ketu
     */
    private FullKundliResponse.Dosha detectGrahanDosha(List<PlanetPosition> planets) {
        return FullKundliResponse.Dosha.builder()
                .present(false)
                .description("No Grahan Dosha detected")
                .build();
    }

    /**
     * Detect auspicious yogas
     */
    private List<String> detectYogas(List<PlanetPosition> planets, String lagna) {
        List<String> yogas = new ArrayList<>();

        String moonRashi = getPlanetRashi(planets, "Moon");
        String jupiterRashi = getPlanetRashi(planets, "Jupiter");
        String sunRashi = getPlanetRashi(planets, "Sun");
        String mercuryRashi = getPlanetRashi(planets, "Mercury");

        if (moonRashi != null && jupiterRashi != null && isKendraRelation(moonRashi, jupiterRashi)) {
            yogas.add("Gaj Kesari Yoga");
        }

        if (sunRashi != null && sunRashi.equals(mercuryRashi)) {
            yogas.add("Budhaditya Yoga");
        }

        return yogas;
    }

    private boolean isKendraRelation(String fromRashi, String toRashi) {
        Integer from = RASHI_INDEX_MAP.get(fromRashi);
        Integer to = RASHI_INDEX_MAP.get(toRashi);

        if (from == null || to == null) {
            return false;
        }

        int diff = (to - from + 12) % 12;
        return diff == 0 || diff == 3 || diff == 6 || diff == 9;
    }

    private String getPlanetRashi(List<PlanetPosition> planets, String planetName) {
        return planets.stream()
                .filter(p -> planetName.equalsIgnoreCase(p.getPlanet()))
                .map(PlanetPosition::getRashi)
                .findFirst()
                .orElse(null);
    }

    private Double getPlanetLongitude(List<PlanetPosition> planets, String planetName) {
        return planets.stream()
                .filter(p -> planetName.equalsIgnoreCase(p.getPlanet()))
                .map(PlanetPosition::getLongitude)
                .findFirst()
                .orElse(0.0);
    }

    private double getAscendantLongitude(double julDay, double lat, double lon) {
        double[] cusps = new double[13];
        double[] ascmc = new double[10];
        // Use SEFLG_SIDEREAL for Vedic astrology
        swissEph.swe_houses(julDay, SweConst.SEFLG_SIDEREAL, lat, lon, 'P', cusps, ascmc);
        return ascmc[0];
    }

    private String getNakshatra(double moonLong) {
        int nak = (int) (moonLong / (360.0 / 27.0));
        return NAKSHATRAS[nak % 27];
    }

    private String getPada(double moonLong) {
        return "Pada " + getPadaNumber(moonLong);
    }

    private int getPadaNumber(double moonLong) {
        // Pada is based on nakshatra position (1-4)
        return ((int) ((moonLong % (360.0 / 27.0)) / (360.0 / 108.0)) + 1);
    }

    private Map<String, Object> buildTithiDetails(int tithiNumber) {
        int index = Math.max(1, Math.min(30, tithiNumber)) - 1;
        String name = TITHI_NAMES[index];
        String meaning = TITHI_MEANINGS[index];
        String paksha = tithiNumber <= 15 ? "Shukla" : "Krishna";

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("number", tithiNumber);
        details.put("name", name);
        details.put("meaning", meaning);
        details.put("paksha", paksha);
        return details;
    }

    private Map<String, Object> buildNakshatraDetails(String nakshatra, int padaNumber) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("name", nakshatra);
        details.put("pada", padaNumber);
        details.put("padaName", "Pada " + padaNumber);
        details.put("padaMeaning", PADA_MEANINGS.getOrDefault(padaNumber, ""));
        return details;
    }

    private String getPlanetName(int id) {
        return switch (id) {
            case SweConst.SE_SUN -> "Sun";
            case SweConst.SE_MOON -> "Moon";
            case SweConst.SE_MARS -> "Mars";
            case SweConst.SE_MERCURY -> "Mercury";
            case SweConst.SE_JUPITER -> "Jupiter";
            case SweConst.SE_VENUS -> "Venus";
            case SweConst.SE_SATURN -> "Saturn";
            case SweConst.SE_TRUE_NODE -> "Rahu";
            case SweConst.SE_CHIRON -> "Chiron";
            default -> "Unknown";
        };
    }

    private String generateOverallMessage(DoshaDetection doshaDetection) {
        StringBuilder msg = new StringBuilder();
        int doshaCount = 0;

        if (isPresent(doshaDetection.mangalDosha)) doshaCount++;
        if (isPresent(doshaDetection.kaalSarpDosha)) doshaCount++;
        if (isPresent(doshaDetection.pitruDosha)) doshaCount++;
        if (isPresent(doshaDetection.grahanDosha)) doshaCount++;

        if (doshaCount == 0) {
            msg.append("Chart is free from major doshas. ");
        } else {
            msg.append(doshaCount).append(" doshas detected. ");
        }

        return msg.toString();
    }

    private Integer calculateHealthScore(DoshaDetection doshaDetection) {
        int score = 100;
        if (isPresent(doshaDetection.mangalDosha)) score -= 20;
        if (isPresent(doshaDetection.kaalSarpDosha)) score -= 15;
        if (isPresent(doshaDetection.pitruDosha)) score -= 10;
        if (isPresent(doshaDetection.grahanDosha)) score -= 10;
        return Math.max(score, 0);
    }

    private boolean isPresent(FullKundliResponse.Dosha dosha) {
        return dosha != null && Boolean.TRUE.equals(dosha.getPresent());
    }

    /**
     * Convert local time to Universal Time (UT)
     * @param yyyy Year
     * @param mm Month
     * @param dd Day
     * @param localTime Local time in decimal hours
     * @param timezoneOffset Timezone offset in hours (e.g., 5.5 for IST)
     * @return Array of [year, month, day, time in UT]
     */
    private double[] convertToUT(int yyyy, int mm, int dd, double localTime, double timezoneOffset) {
        double timeUT = localTime - timezoneOffset;
        int yyyyUT = yyyy;
        int mmUT = mm;
        int ddUT = dd;
        
        if (timeUT < 0) {
            timeUT += 24;
            ddUT--;
            
            if (ddUT < 1) {
                mmUT--;
                if (mmUT < 1) {
                    mmUT = 12;
                    yyyyUT--;
                }
                
                // Get number of days in previous month
                int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
                if (mmUT == 2 && isLeapYear(yyyyUT)) {
                    ddUT = 29;
                } else {
                    ddUT = daysInMonth[mmUT - 1];
                }
            }
        } else if (timeUT >= 24) {
            timeUT -= 24;
            ddUT++;
            
            int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
            int maxDays = daysInMonth[mmUT - 1];
            if (mmUT == 2 && isLeapYear(yyyyUT)) {
                maxDays = 29;
            }
            
            if (ddUT > maxDays) {
                ddUT = 1;
                mmUT++;
                if (mmUT > 12) {
                    mmUT = 1;
                    yyyyUT++;
                }
            }
        }
        
        return new double[]{yyyyUT, mmUT, ddUT, timeUT};
    }
    
    private boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * Calculate element distribution (Fire, Earth, Air, Water)
     * Fire: Aries, Leo, Sagittarius
     * Earth: Taurus, Virgo, Capricorn
     * Air: Gemini, Libra, Aquarius
     * Water: Cancer, Scorpio, Pisces
     */
    private Map<String, Double> calculateElements(List<PlanetPosition> planets, String lagna) {
        Map<String, Double> elements = new LinkedHashMap<>();
        elements.put("Fire", 0.0);
        elements.put("Earth", 0.0);
        elements.put("Air", 0.0);
        elements.put("Water", 0.0);

        // Element mapping
        Map<String, String> rashiToElement = Map.ofEntries(
            Map.entry("Aries", "Fire"),
            Map.entry("Taurus", "Earth"),
            Map.entry("Gemini", "Air"),
            Map.entry("Cancer", "Water"),
            Map.entry("Leo", "Fire"),
            Map.entry("Virgo", "Earth"),
            Map.entry("Libra", "Air"),
            Map.entry("Scorpio", "Water"),
            Map.entry("Sagittarius", "Fire"),
            Map.entry("Capricorn", "Earth"),
            Map.entry("Aquarius", "Air"),
            Map.entry("Pisces", "Water")
        );

        // Count Lagna (Ascendant) - weighted double (2x weight)
        String lagnaElement = rashiToElement.get(lagna);
        if (lagnaElement != null) {
            elements.put(lagnaElement, elements.get(lagnaElement) + 2.0);
        }

        // Count each planet (1x weight)
        for (PlanetPosition planet : planets) {
            String rashi = planet.getRashi();
            String element = rashiToElement.get(rashi);
            if (element != null) {
                elements.put(element, elements.get(element) + 1.0);
            }
        }

        return elements;
    }

    private static class DoshaDetection {
        FullKundliResponse.Dosha mangalDosha;
        FullKundliResponse.Dosha kaalSarpDosha;
        FullKundliResponse.Dosha pitruDosha;
        FullKundliResponse.Dosha grahanDosha;
    }
}
