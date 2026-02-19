package com.astro.backend.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompatibilityMatchingService {

    private static final List<String> NAKSHATRA_LIST = List.of(
        "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashirsha", "Ardra", "Punarvasu",
        "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta",
        "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha",
        "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada",
        "Uttara Bhadrapada", "Revati"
    );

    private static final Map<String, Integer> RASHI_INDEX = Map.ofEntries(
        Map.entry("Aries", 1), Map.entry("Taurus", 2), Map.entry("Gemini", 3),
        Map.entry("Cancer", 4), Map.entry("Leo", 5), Map.entry("Virgo", 6),
        Map.entry("Libra", 7), Map.entry("Scorpio", 8), Map.entry("Sagittarius", 9),
        Map.entry("Capricorn", 10), Map.entry("Aquarius", 11), Map.entry("Pisces", 12)
    );

    private static final Map<String, Integer> RASHI_LORD = Map.ofEntries(
        Map.entry("Aries", 3), Map.entry("Taurus", 6), Map.entry("Gemini", 4),
        Map.entry("Cancer", 2), Map.entry("Leo", 1), Map.entry("Virgo", 4),
        Map.entry("Libra", 6), Map.entry("Scorpio", 3), Map.entry("Sagittarius", 5),
        Map.entry("Capricorn", 7), Map.entry("Aquarius", 7), Map.entry("Pisces", 5)
    );

    private static final int[] NAKSHATRA_TO_GANA = {
        1, 2, 3, 2, 1, 2, 1, 1, 3,
        3, 2, 2, 1, 3, 1, 3, 1, 3,
        3, 2, 2, 1, 3, 3, 2, 2, 1
    };

    private static final int[] NAKSHATRA_TO_NADI = {
        1, 1, 1, 1, 1, 1, 1, 1, 1,
        2, 2, 2, 2, 2, 2, 2, 2, 2,
        3, 3, 3, 3, 3, 3, 3, 3, 3
    };

    private static final int[] NAKSHATRA_TO_YONI = {
        1, 2, 3, 4, 4, 5, 6, 3, 6,
        7, 7, 8, 9, 10, 9, 10, 11, 11,
        5, 12, 13, 12, 13, 1, 14, 8, 2
    };

    public Map<String, Integer> buildGunasFromChart(String moonSign, String nakshatra, boolean mangalDosha) {
    Map<String, Integer> gunas = new LinkedHashMap<>();
    int rashiIndex = RASHI_INDEX.getOrDefault(moonSign, 0);
    int nakshatraIndex = getNakshatraIndex(nakshatra);

    gunas.put("Varna", getVarnaFromRashi(moonSign));
    gunas.put("Vasya", getVasyaFromRashi(moonSign));
    gunas.put("Tara", nakshatraIndex);
    gunas.put("Yoni", getYoniFromNakshatra(nakshatraIndex));
    gunas.put("Graha Maitri", RASHI_LORD.getOrDefault(moonSign, 0));
    gunas.put("Gana", getGanaFromNakshatra(nakshatraIndex));
    gunas.put("Bhakoot", rashiIndex);
    gunas.put("Nadi", getNadiFromNakshatra(nakshatraIndex));
    gunas.put("Mangal Dosha", mangalDosha ? 1 : 0);

    return gunas;
    }

    /**
     * Calculate 36 Gun Milan (compatibility matching)
     * @param groom36Guns Map of 12 guna with their points
     * @param bride36Guns Map of 12 guna with their points
     * @return Compatibility score and breakdown
     */
    public CompatibilityResult calculateGunMilan(Map<String, Integer> groom36Guns, Map<String, Integer> bride36Guns) {
        try {
            int totalMatch = 0;
            Map<String, Integer> categoryMatches = new HashMap<>();

            // 1. Varna Guna (1 point)
            int varnaMatch = calculateVarnaMatch(
                    groom36Guns.getOrDefault("Varna", 0),
                    bride36Guns.getOrDefault("Varna", 0)
            );
            totalMatch += varnaMatch;
            categoryMatches.put("Varna (Caste Harmony)", varnaMatch);

            // 2. Vasya Guna (2 points)
            int vasyaMatch = calculateVasyaMatch(
                    groom36Guns.getOrDefault("Vasya", 0),
                    bride36Guns.getOrDefault("Vasya", 0)
            );
            totalMatch += vasyaMatch;
            categoryMatches.put("Vasya (Attraction)", vasyaMatch);

            // 3. Tara Guna (3 points)
            int taraMatch = calculateTaraMatch(
                    groom36Guns.getOrDefault("Tara", 0),
                    bride36Guns.getOrDefault("Tara", 0)
            );
            totalMatch += taraMatch;
            categoryMatches.put("Tara (Longevity)", taraMatch);

            // 4. Yoni Guna (4 points)
            int yoniMatch = calculateYoniMatch(
                    groom36Guns.getOrDefault("Yoni", 0),
                    bride36Guns.getOrDefault("Yoni", 0)
            );
            totalMatch += yoniMatch;
            categoryMatches.put("Yoni (Sexual Compatibility)", yoniMatch);

            // 5. Graha Maitri (5 points)
            int grahaMaitriMatch = calculateGrahaMaitri(
                    groom36Guns.getOrDefault("Graha Maitri", 0),
                    bride36Guns.getOrDefault("Graha Maitri", 0)
            );
            totalMatch += grahaMaitriMatch;
            categoryMatches.put("Graha Maitri (Planetary Friendship)", grahaMaitriMatch);

            // 6. Gana Guna (6 points)
            int ganaMatch = calculateGanaMatch(
                    groom36Guns.getOrDefault("Gana", 0),
                    bride36Guns.getOrDefault("Gana", 0)
            );
            totalMatch += ganaMatch;
            categoryMatches.put("Gana (Temperament)", ganaMatch);

            // 7. Bhakoot Guna (7 points)
            int bhakootMatch = calculateBhakootMatch(
                    groom36Guns.getOrDefault("Bhakoot", 0),
                    bride36Guns.getOrDefault("Bhakoot", 0)
            );
            totalMatch += bhakootMatch;
            categoryMatches.put("Bhakoot (Health/Wealth)", bhakootMatch);

            // 8. Nadi Guna (8 points - most important)
            int nadiMatch = calculateNadiMatch(
                    groom36Guns.getOrDefault("Nadi", 0),
                    bride36Guns.getOrDefault("Nadi", 0)
            );
            totalMatch += nadiMatch;
            categoryMatches.put("Nadi (Health Compatibility)", nadiMatch);

            // Detect doshas
            boolean nadiDosha = detectNadiDosha(groom36Guns, bride36Guns);
            boolean bhakootDosha = detectBhakootDosha(groom36Guns, bride36Guns);
            boolean mangalDosha = detectMangalDoshaInMatch(groom36Guns, bride36Guns);

            String prediction = getPrediction(totalMatch, nadiDosha, bhakootDosha, mangalDosha);

            return CompatibilityResult.builder()
                    .totalPoints(totalMatch)
                    .outOf(36)
                    .percentage((totalMatch / 36.0) * 100)
                    .categoryMatches(categoryMatches)
                    .nadiDosha(nadiDosha)
                    .bhakootDosha(bhakootDosha)
                    .mangalDosha(mangalDosha)
                    .prediction(prediction)
                    .build();

        } catch (Exception e) {
            log.error("Error calculating gun milan", e);
            throw new RuntimeException("Failed to calculate compatibility: " + e.getMessage());
        }
    }

    private int calculateVarnaMatch(int groomVarna, int brideVarna) {
        return groomVarna == brideVarna ? 1 : 0;
    }

    private int calculateVasyaMatch(int groomVasya, int brideVasya) {
        // Vasya rules: Quadruped, Bipedal, Insect, Aquatic, Reptile
        return groomVasya == brideVasya ? 2 : 0;
    }

    private int calculateTaraMatch(int groomTara, int brideTara) {
        int diff = Math.abs(groomTara - brideTara);
        return switch (diff % 9) {
            case 2, 4, 6, 8 -> 3;
            default -> 0;
        };
    }

    private int calculateYoniMatch(int groomYoni, int brideYoni) {
        return groomYoni == brideYoni ? 4 : 0;
    }

    private int calculateGrahaMaitri(int groomMaitri, int brideMaitri) {
        return groomMaitri == brideMaitri ? 5 : (Math.abs(groomMaitri - brideMaitri) == 1 ? 2 : 0);
    }

    private int calculateGanaMatch(int groomGana, int brideGana) {
        return groomGana == brideGana ? 6 : 0;
    }

    private int calculateBhakootMatch(int groomBhakoot, int brideBhakoot) {
        int diff = Math.abs(groomBhakoot - brideBhakoot);
        return switch (diff) {
            case 0, 1, 2, 5 -> 7;
            case 3, 4 -> 4;
            default -> 0;
        };
    }

    private int calculateNadiMatch(int groomNadi, int brideNadi) {
        // Same nadi is considered Nadi Dosha, so points should be zero.
        return groomNadi == brideNadi ? 0 : 8;
    }

    private boolean detectNadiDosha(Map<String, Integer> groomData, Map<String, Integer> brideData) {
        // Nadi Dosha: Same nadi in groom and bride
        int groomNadi = groomData.getOrDefault("Nadi", 0);
        int brideNadi = brideData.getOrDefault("Nadi", 0);
        return groomNadi == brideNadi && groomNadi != 0;
    }

    private boolean detectBhakootDosha(Map<String, Integer> groomData, Map<String, Integer> brideData) {
        int diff = Math.abs(groomData.getOrDefault("Bhakoot", 0) - brideData.getOrDefault("Bhakoot", 0));
        return diff == 3 || diff == 4;
    }

    private boolean detectMangalDoshaInMatch(Map<String, Integer> groomData, Map<String, Integer> brideData) {
        boolean groomMangal = groomData.getOrDefault("Mangal Dosha", 0) == 1;
        boolean brideMangal = brideData.getOrDefault("Mangal Dosha", 0) == 1;
        return groomMangal || brideMangal;
    }

    private String getPrediction(int totalMatch, boolean nadiDosha, boolean bhakootDosha, boolean mangalDosha) {
        String base = "";

        if (totalMatch >= 32) {
            base = "Excellent Match - Marriage is highly recommended";
        } else if (totalMatch >= 26) {
            base = "Good Match - Marriage can be beneficial";
        } else if (totalMatch >= 20) {
            base = "Average Match - Marriage is possible with adjustments";
        } else if (totalMatch >= 14) {
            base = "Below Average - Caution needed before marriage";
        } else {
            base = "Poor Match - Marriage is not recommended";
        }

        StringBuilder doshaInfo = new StringBuilder();
        if (nadiDosha) doshaInfo.append(", But Nadi Dosha present");
        if (bhakootDosha) doshaInfo.append(", Bhakoot Dosha detected");
        if (mangalDosha) doshaInfo.append(", Mangal Dosha present");

        return base + doshaInfo;
    }

    /**
     * Basic sign compatibility for quick matching view.
     */
    public Map<String, Object> calculateSignCompatibility(String yourSign, String partnerSign) {
        String normalizedYourSign = normalizeSign(yourSign);
        String normalizedPartnerSign = normalizeSign(partnerSign);

        String yourElement = getElement(normalizedYourSign);
        String partnerElement = getElement(normalizedPartnerSign);

        int baseScore = 55;
        if (yourElement.equals(partnerElement)) {
            baseScore += 25;
        } else if (isComplementaryElement(yourElement, partnerElement)) {
            baseScore += 15;
        } else {
            baseScore -= 10;
        }

        int love = clamp(baseScore + 4);
        int trust = clamp(baseScore + (yourElement.equals(partnerElement) ? 8 : 2));
        int communication = clamp(baseScore + (isComplementaryElement(yourElement, partnerElement) ? 7 : 1));
        int overall = clamp(Math.round((love + trust + communication) / 3.0f));

        List<String> strengths = new ArrayList<>();
        strengths.add("Natural attraction between " + normalizedYourSign + " and " + normalizedPartnerSign);
        strengths.add("Shared energy in key life decisions");
        strengths.add("Potential for long-term emotional growth");

        List<String> challenges = new ArrayList<>();
        if (!yourElement.equals(partnerElement)) {
            challenges.add("Different emotional styles may cause misunderstandings");
        }
        challenges.add("Consistency and communication need conscious effort");
        challenges.add("Mutual patience is required during disagreements");

        String description = buildSignDescription(normalizedYourSign, normalizedPartnerSign, overall);
        String advice = overall >= 80
            ? "Strong match. Keep communication open and maintain balance."
            : overall >= 65
                ? "Good potential. Focus on trust-building and emotional clarity."
                : "Needs effort. Respect differences and communicate with patience.";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("signs", normalizedYourSign + " & " + normalizedPartnerSign);
        result.put("overall", overall);
        result.put("love", love);
        result.put("trust", trust);
        result.put("communication", communication);
        result.put("compatibility", overall);
        result.put("description", description);
        result.put("strengths", strengths);
        result.put("challenges", challenges);
        result.put("advice", advice);
        return result;
    }

    private String normalizeSign(String sign) {
        if (sign == null || sign.isBlank()) {
            return "Aries";
        }
        String trimmed = sign.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String getElement(String sign) {
        return switch (sign) {
            case "Aries", "Leo", "Sagittarius" -> "Fire";
            case "Taurus", "Virgo", "Capricorn" -> "Earth";
            case "Gemini", "Libra", "Aquarius" -> "Air";
            case "Cancer", "Scorpio", "Pisces" -> "Water";
            default -> "Neutral";
        };
    }

    private boolean isComplementaryElement(String first, String second) {
        return (first.equals("Fire") && second.equals("Air"))
            || (first.equals("Air") && second.equals("Fire"))
            || (first.equals("Earth") && second.equals("Water"))
            || (first.equals("Water") && second.equals("Earth"));
    }

    private String buildSignDescription(String yourSign, String partnerSign, int score) {
        if (score >= 80) {
            return yourSign + " and " + partnerSign + " show high astrological harmony with strong emotional and practical balance.";
        }
        if (score >= 65) {
            return yourSign + " and " + partnerSign + " are a good match with growth potential through better communication.";
        }
        return yourSign + " and " + partnerSign + " may face compatibility challenges that require conscious effort and patience.";
    }

    private int getNakshatraIndex(String nakshatra) {
        if (nakshatra == null) {
            return 0;
        }
        int index = NAKSHATRA_LIST.indexOf(nakshatra.trim());
        return index >= 0 ? index + 1 : 0;
    }

    private int getVarnaFromRashi(String rashi) {
        if (rashi == null) return 0;
        return switch (rashi) {
            case "Aries", "Leo", "Sagittarius" -> 2; // Kshatriya
            case "Taurus", "Virgo", "Capricorn" -> 3; // Vaishya
            case "Gemini", "Libra", "Aquarius" -> 4; // Shudra
            case "Cancer", "Scorpio", "Pisces" -> 1; // Brahmin
            default -> 0;
        };
    }

    private int getVasyaFromRashi(String rashi) {
        if (rashi == null) return 0;
        return switch (rashi) {
            case "Aries", "Cancer", "Libra", "Capricorn" -> 1; // Movable
            case "Taurus", "Leo", "Scorpio", "Aquarius" -> 2; // Fixed
            case "Gemini", "Virgo", "Sagittarius", "Pisces" -> 3; // Dual
            default -> 0;
        };
    }

    private int getGanaFromNakshatra(int nakshatraIndex) {
        if (nakshatraIndex < 1 || nakshatraIndex > 27) return 0;
        return NAKSHATRA_TO_GANA[nakshatraIndex - 1];
    }

    private int getNadiFromNakshatra(int nakshatraIndex) {
        if (nakshatraIndex < 1 || nakshatraIndex > 27) return 0;
        return NAKSHATRA_TO_NADI[nakshatraIndex - 1];
    }

    private int getYoniFromNakshatra(int nakshatraIndex) {
        if (nakshatraIndex < 1 || nakshatraIndex > 27) return 0;
        return NAKSHATRA_TO_YONI[nakshatraIndex - 1];
    }

    public static class CompatibilityResult {
        public int totalPoints;
        public int outOf;
        public double percentage;
        public Map<String, Integer> categoryMatches;
        public boolean nadiDosha;
        public boolean bhakootDosha;
        public boolean mangalDosha;
        public String prediction;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private int totalPoints;
            private int outOf;
            private double percentage;
            private Map<String, Integer> categoryMatches;
            private boolean nadiDosha;
            private boolean bhakootDosha;
            private boolean mangalDosha;
            private String prediction;

            public Builder totalPoints(int totalPoints) {
                this.totalPoints = totalPoints;
                return this;
            }

            public Builder outOf(int outOf) {
                this.outOf = outOf;
                return this;
            }

            public Builder percentage(double percentage) {
                this.percentage = percentage;
                return this;
            }

            public Builder categoryMatches(Map<String, Integer> categoryMatches) {
                this.categoryMatches = categoryMatches;
                return this;
            }

            public Builder nadiDosha(boolean nadiDosha) {
                this.nadiDosha = nadiDosha;
                return this;
            }

            public Builder bhakootDosha(boolean bhakootDosha) {
                this.bhakootDosha = bhakootDosha;
                return this;
            }

            public Builder mangalDosha(boolean mangalDosha) {
                this.mangalDosha = mangalDosha;
                return this;
            }

            public Builder prediction(String prediction) {
                this.prediction = prediction;
                return this;
            }

            public CompatibilityResult build() {
                CompatibilityResult result = new CompatibilityResult();
                result.totalPoints = this.totalPoints;
                result.outOf = this.outOf;
                result.percentage = this.percentage;
                result.categoryMatches = this.categoryMatches;
                result.nadiDosha = this.nadiDosha;
                result.bhakootDosha = this.bhakootDosha;
                result.mangalDosha = this.mangalDosha;
                result.prediction = this.prediction;
                return result;
            }
        }
    }
}
