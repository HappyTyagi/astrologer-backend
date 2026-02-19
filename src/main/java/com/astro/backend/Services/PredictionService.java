package com.astro.backend.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {
    private static final List<String> ZODIAC_SIGNS = List.of(
            "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
            "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    );


    /**
     * Generate daily horoscope based on user's birth chart
     */
    public Map<String, Object> generateDailyHoroscope(String sunSign, LocalDate date) {
        try {
            Map<String, Object> horoscope = new LinkedHashMap<>();

            horoscope.put("date", date.toString());
            horoscope.put("sign", sunSign);
            horoscope.put("overall", getOverallDayPrediction(sunSign, date));
            horoscope.put("love", getLovePrediction(sunSign, date));
            horoscope.put("career", getCareerPrediction(sunSign, date));
            horoscope.put("health", getHealthPrediction(sunSign, date));
            horoscope.put("finance", getFinancePrediction(sunSign, date));
            horoscope.put("lucky", getLuckyDetails(sunSign, date));
            horoscope.put("advice", getDailyAdvice(sunSign));

            return horoscope;

        } catch (Exception e) {
            log.error("Error generating daily horoscope", e);
            throw new RuntimeException("Failed to generate horoscope: " + e.getMessage());
        }
    }

    /**
     * Generate weekly horoscope
     */
    public Map<String, Object> generateWeeklyHoroscope(String sunSign, LocalDate weekStart) {
        Map<String, Object> weeklyHoroscope = new LinkedHashMap<>();

        weeklyHoroscope.put("week", weekStart + " to " + weekStart.plusDays(6));
        weeklyHoroscope.put("sign", sunSign);
        weeklyHoroscope.put("overview", getWeeklyOverview(sunSign));
        weeklyHoroscope.put("dailyBreakdown", getDailyBreakdown(sunSign, weekStart));
        weeklyHoroscope.put("weeklyLucky", getLuckyDetailsWeekly(sunSign));
        weeklyHoroscope.put("weeklyAdvice", getWeeklyAdvice(sunSign));

        return weeklyHoroscope;
    }

    /**
     * Generate monthly horoscope
     */
    public Map<String, Object> generateMonthlyHoroscope(String sunSign, int month, int year) {
        Map<String, Object> monthlyHoroscope = new LinkedHashMap<>();

        monthlyHoroscope.put("month", month + "/" + year);
        monthlyHoroscope.put("sign", sunSign);
        monthlyHoroscope.put("overview", getMonthlyOverview(sunSign, month));
        monthlyHoroscope.put("love", getMonthlyLove(sunSign));
        monthlyHoroscope.put("career", getMonthlyCareer(sunSign));
        monthlyHoroscope.put("finance", getMonthlyFinance(sunSign));
        monthlyHoroscope.put("health", getMonthlyHealth(sunSign));
        monthlyHoroscope.put("important_dates", getImportantDates(month, sunSign));
        monthlyHoroscope.put("monthly_advice", getMonthlyAdvice(sunSign));

        return monthlyHoroscope;
    }

    /**
     * Transit analysis based on current planetary positions
     */
    public Map<String, Object> getTransitAnalysis(String birthChart, LocalDate date) {
        Map<String, Object> transitAnalysis = new LinkedHashMap<>();

        String saturnSign = getApproxSaturnTransitSign(date);
        String jupiterSign = getApproxJupiterTransitSign(date);
        String rahuSign = getApproxRahuTransitSign(date);

        List<Map<String, String>> currentTransits = new ArrayList<>();
        currentTransits.add(buildTransit("Saturn", saturnSign, "High",
                "Discipline, responsibility and karmic results are highlighted.",
                "Current ~ next 2.5 years"));
        currentTransits.add(buildTransit("Jupiter", jupiterSign, "Moderate",
                "Growth in wisdom, guidance and opportunities.",
                "Current ~ next 12 months"));
        currentTransits.add(buildTransit("Rahu", rahuSign, "Moderate",
                "Unconventional shifts and strong material focus.",
                "Current ~ next 18 months"));

        List<Map<String, String>> upcomingEvents = List.of(
                buildUpcomingEvent("Lunar Cycle Shift", date.plusDays(7).toString(),
                        "Review emotional priorities and avoid impulsive reactions."),
                buildUpcomingEvent("Mercury Transit Window", date.plusDays(15).toString(),
                        "Double-check communication and travel plans."),
                buildUpcomingEvent("New Moon Intention Day", date.plusDays(28).toString(),
                        "Good period to start focused personal goals.")
        );

        transitAnalysis.put("date", date.toString());
        transitAnalysis.put("birthChart", birthChart);
        transitAnalysis.put("currentTransits", currentTransits);
        transitAnalysis.put("upcoming", upcomingEvents);

        // Keep legacy keys for backward compatibility.
        transitAnalysis.put("majorTransits", currentTransits.stream()
                .map(t -> t.get("planet") + " in " + t.get("sign") + " - " + t.get("description"))
                .toList());
        transitAnalysis.put("affectedSigns", getAffectedSigns(date));
        transitAnalysis.put("durationAndImpact", getTransitDurationAndImpact(date));
        transitAnalysis.put("recommendations", getTransitRecommendations(date));

        return transitAnalysis;
    }

    /**
     * Sade Sati (7.5 year Saturn transit) analysis
     */
    public Map<String, Object> getSadeSatiAnalysis(String moonSign, LocalDate currentDate) {
        Map<String, Object> sadeSati = new LinkedHashMap<>();
        String normalizedMoonSign = normalizeSign(moonSign);
        String saturnSign = getApproxSaturnTransitSign(currentDate);

        int moonIndex = getSignIndex(normalizedMoonSign);
        int saturnIndex = getSignIndex(saturnSign);

        int firstPhaseSign = Math.floorMod(moonIndex - 1, 12);
        int secondPhaseSign = moonIndex;
        int thirdPhaseSign = Math.floorMod(moonIndex + 1, 12);

        boolean isSadeSati = saturnIndex == firstPhaseSign
                || saturnIndex == secondPhaseSign
                || saturnIndex == thirdPhaseSign;

        String phase = "Not Active";
        if (saturnIndex == firstPhaseSign) {
            phase = "1st Phase (Rising)";
        } else if (saturnIndex == secondPhaseSign) {
            phase = "2nd Phase (Peak)";
        } else if (saturnIndex == thirdPhaseSign) {
            phase = "3rd Phase (Setting)";
        }

        List<Map<String, Object>> phases = List.of(
                buildPhase("1st Phase (Rising)", "Saturn in 12th from Moon", "Increased expenses, relocations, inner unease",
                        List.of("Practice discipline in spending", "Saturday mantra and charity")),
                buildPhase("2nd Phase (Peak)", "Saturn over Moon sign", "Emotional pressure, responsibility and karmic tests",
                        List.of("Meditation and routine stability", "Serve elders and needy")),
                buildPhase("3rd Phase (Setting)", "Saturn in 2nd from Moon", "Family/finance restructuring and maturity",
                        List.of("Patient communication", "Long-term planning with consistency"))
        );

        String nextSadeSatiDate = getNextSadeSatiDate(normalizedMoonSign, currentDate);
        String generalAdvice = "Stay patient, maintain routines, and focus on dharma-based actions for best results.";

        sadeSati.put("moonSign", normalizedMoonSign);
        sadeSati.put("saturnTransitSign", saturnSign);
        sadeSati.put("inSadeSati", isSadeSati);
        sadeSati.put("phase", phase);
        sadeSati.put("expectedDuration", "7.5 years");
        sadeSati.put("characteristics", getSadeSatiCharacteristics(normalizedMoonSign));
        sadeSati.put("remedies", getSadeSatiRemedies());
        sadeSati.put("advice", generalAdvice);
        sadeSati.put("nextSadeSatiDate", nextSadeSatiDate);

        // Keys aligned for mobile screens.
        sadeSati.put("status", isSadeSati ? "Active - " + phase : "Not In Sade Sati");
        sadeSati.put("description", isSadeSati
                ? "Saturn is currently influencing your Moon cycle. Follow remedies and remain disciplined."
                : "You are currently outside Sade Sati influence.");
        sadeSati.put("nextSadeSati", nextSadeSatiDate);
        sadeSati.put("duration", "7.5 years");
        sadeSati.put("phases", phases);
        sadeSati.put("generalAdvice", generalAdvice);
        sadeSati.put("message", isSadeSati
                ? "Sade Sati is active. Follow structured routines."
                : "You are not in Sade Sati period. Enjoy this peaceful phase.");
        return sadeSati;
    }

    /**
     * Dhaiya (2.5 year Saturn transit) analysis
     */
    public Map<String, Object> getDhaiyaAnalysis(String moonSign, LocalDate currentDate) {
        Map<String, Object> dhaiya = new LinkedHashMap<>();
        String normalizedMoonSign = normalizeSign(moonSign);
        String saturnSign = getApproxSaturnTransitSign(currentDate);

        int moonIndex = getSignIndex(normalizedMoonSign);
        int saturnIndex = getSignIndex(saturnSign);

        int fourthFromMoon = Math.floorMod(moonIndex + 3, 12);
        int eighthFromMoon = Math.floorMod(moonIndex + 7, 12);

        boolean isDhaiya = saturnIndex == fourthFromMoon || saturnIndex == eighthFromMoon;
        String phase = saturnIndex == fourthFromMoon ? "Kantaka Shani (4th from Moon)"
                : (saturnIndex == eighthFromMoon ? "Ashtama Shani (8th from Moon)" : "Not Active");

        String nextDhaiyaDate = getNextDhaiyaDate(normalizedMoonSign, currentDate);
        List<Map<String, String>> characteristics = List.of(
                Map.of("title", "Mental Pressure", "description",
                        "Temporary stress may rise; emotional discipline is important."),
                Map.of("title", "Workload Shift", "description",
                        "Responsibilities can increase and require structured planning."),
                Map.of("title", "Financial Caution", "description",
                        "Avoid impulsive financial decisions during sensitive periods."),
                Map.of("title", "Health Discipline", "description",
                        "Maintain sleep, routine and steady physical activity.")
        );
        List<String> remedies = getDhaiyaRemedies();
        String advice = "Stay consistent with routines and avoid reactive decisions.";

        dhaiya.put("moonSign", normalizedMoonSign);
        dhaiya.put("saturnTransitSign", saturnSign);
        dhaiya.put("inDhaiya", isDhaiya);
        dhaiya.put("phase", phase);
        dhaiya.put("duration", "2.5 years");
        dhaiya.put("characteristics", characteristics);
        dhaiya.put("affectedLife", new String[]{"Career", "Relationships", "Finance", "Health"});
        dhaiya.put("remedies", remedies);
        dhaiya.put("advice", advice);
        dhaiya.put("nextDhaiyaDate", nextDhaiyaDate);

        // Keys aligned for mobile screens.
        dhaiya.put("status", isDhaiya ? "Active - " + phase : "Not In Dhaiya");
        dhaiya.put("description", isDhaiya
                ? "Saturn is in a Dhaiya-sensitive position from your Moon sign."
                : "You are currently not under Dhaiya influence.");
        dhaiya.put("nextDhaiya", nextDhaiyaDate);
        dhaiya.put("message", isDhaiya ? "Dhaiya is active." : "You are not in Dhaiya period.");

        return dhaiya;
    }

    // Helper methods

    private String getOverallDayPrediction(String sunSign, LocalDate date) {
        return switch (sunSign.toLowerCase()) {
            case "aries" -> "A dynamic day ahead. Take action on pending projects.";
            case "taurus" -> "A stable day. Focus on financial matters.";
            case "gemini" -> "Communication plays a key role. Express yourself clearly.";
            case "cancer" -> "Emotional day. Trust your intuition.";
            case "leo" -> "Shiny day ahead. Your charm works wonders.";
            case "virgo" -> "Analytical energy. Perfect for planning.";
            case "libra" -> "Balance is key. Weigh your decisions carefully.";
            case "scorpio" -> "Transformation possible. Embrace changes.";
            case "sagittarius" -> "Adventure awaits. Explore new possibilities.";
            case "capricorn" -> "Hard work pays off. Stay disciplined.";
            case "aquarius" -> "Innovation time. Think outside the box.";
            case "pisces" -> "Dreamy energy. Follow your intuition.";
            default -> "A day full of possibilities awaits you.";
        };
    }

    private String getLovePrediction(String sunSign, LocalDate date) {
        return "Romantic energy surrounds you. Single natives may meet someone interesting.";
    }

    private String getCareerPrediction(String sunSign, LocalDate date) {
        return "Professional growth is indicated. Good day for important meetings.";
    }

    private String getHealthPrediction(String sunSign, LocalDate date) {
        return "Health remains stable. Continue with your wellness routines.";
    }

    private String getFinancePrediction(String sunSign, LocalDate date) {
        return "Financial gains are possible. However, avoid unnecessary expenses.";
    }

    private Map<String, String> getLuckyDetails(String sunSign, LocalDate date) {
        Map<String, String> lucky = new LinkedHashMap<>();
        lucky.put("number", String.valueOf((date.getDayOfMonth() * 7) % 9 + 1));
        lucky.put("color", getSignColor(sunSign));
        lucky.put("time", "06:00 AM - 08:00 AM");
        lucky.put("direction", getSignDirection(sunSign));
        return lucky;
    }

    private String getDailyAdvice(String sunSign) {
        return "Take time for self-reflection. Remember that challenges are opportunities for growth.";
    }

    private String getWeeklyOverview(String sunSign) {
        return "An eventful week lies ahead. Balance work and personal life.";
    }

    private List<Map<String, String>> getDailyBreakdown(String sunSign, LocalDate weekStart) {
        List<Map<String, String>> breakdown = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Map<String, String> day = new LinkedHashMap<>();
            day.put("date", weekStart.plusDays(i).toString());
            day.put("prediction", "Day " + (i + 1) + " brings " + getRandomPrediction());
            breakdown.add(day);
        }
        return breakdown;
    }

    private Map<String, String> getLuckyDetailsWeekly(String sunSign) {
        Map<String, String> lucky = new LinkedHashMap<>();
        lucky.put("bestDay", "Thursday");
        lucky.put("luckyNumber", "7");
        lucky.put("luckyColor", getSignColor(sunSign));
        return lucky;
    }

    private String getWeeklyAdvice(String sunSign) {
        return "Focus on your goals. Do not let obstacles deter you from your path.";
    }

    private String getMonthlyOverview(String sunSign, int month) {
        return "A transformative month. Significant changes may occur in your personal or professional life.";
    }

    private String getMonthlyLove(String sunSign) {
        return "Love and romance flourish. Great time for relationships.";
    }

    private String getMonthlyCareer(String sunSign) {
        return "Career prospects brighten. New opportunities may emerge.";
    }

    private String getMonthlyFinance(String sunSign) {
        return "Financial stability strengthens. Good month for investments.";
    }

    private String getMonthlyHealth(String sunSign) {
        return "Health remains good. Maintain your fitness routine.";
    }

    private List<String> getImportantDates(int month, String sunSign) {
        List<String> dates = new ArrayList<>();
        dates.add("10th - Good for starting new ventures");
        dates.add("15th - Financial gains possible");
        dates.add("22nd - Social gatherings favorable");
        return dates;
    }

    private String getMonthlyAdvice(String sunSign) {
        return "Be proactive. The universe is supporting your endeavors. Take calculated risks.";
    }

    private Map<String, String> buildTransit(
            String planet,
            String sign,
            String impact,
            String description,
            String duration) {
        Map<String, String> transit = new LinkedHashMap<>();
        transit.put("planet", planet);
        transit.put("sign", sign);
        transit.put("impact", impact);
        transit.put("description", description);
        transit.put("duration", duration);
        return transit;
    }

    private Map<String, String> buildUpcomingEvent(String event, String date, String advice) {
        Map<String, String> upcoming = new LinkedHashMap<>();
        upcoming.put("event", event);
        upcoming.put("date", date);
        upcoming.put("advice", advice);
        return upcoming;
    }

    private Map<String, Object> buildPhase(
            String phase,
            String duration,
            String effect,
            List<String> remedies) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("phase", phase);
        data.put("duration", duration);
        data.put("effect", effect);
        data.put("remedies", remedies);
        return data;
    }

    private List<String> getAffectedSigns(LocalDate date) {
        String saturnSign = getApproxSaturnTransitSign(date);
        int saturnIndex = getSignIndex(saturnSign);
        return List.of(
                ZODIAC_SIGNS.get(Math.floorMod(saturnIndex - 1, 12)),
                saturnSign,
                ZODIAC_SIGNS.get(Math.floorMod(saturnIndex + 1, 12)),
                ZODIAC_SIGNS.get(Math.floorMod(saturnIndex + 4, 12))
        );
    }

    private Map<String, String> getTransitDurationAndImpact(LocalDate date) {
        Map<String, String> impact = new LinkedHashMap<>();
        impact.put("duration", "Current quarter");
        impact.put("intensity", "Moderate to High");
        impact.put("overallImpact", "Focus on disciplined growth and mindful decisions.");
        return impact;
    }

    private String getTransitRecommendations(LocalDate date) {
        return "Prioritize consistency, avoid impulsive decisions, and review long-term plans.";
    }

    private String getApproxSaturnTransitSign(LocalDate date) {
        int yearsFromBase = date.getYear() - 2023; // 2023 reference near Aquarius transit
        int signOffset = Math.floorDiv(yearsFromBase, 3);
        return ZODIAC_SIGNS.get(Math.floorMod(10 + signOffset, 12)); // Aquarius index = 10
    }

    private String getApproxJupiterTransitSign(LocalDate date) {
        int yearsFromBase = date.getYear() - 2023; // 2023 reference near Aries transit
        return ZODIAC_SIGNS.get(Math.floorMod(yearsFromBase, 12));
    }

    private String getApproxRahuTransitSign(LocalDate date) {
        long monthsFromBase = java.time.temporal.ChronoUnit.MONTHS.between(LocalDate.of(2023, 1, 1), date);
        int signOffset = (int) Math.floorDiv(monthsFromBase, 18); // ~18 months per sign (retrograde)
        return ZODIAC_SIGNS.get(Math.floorMod(11 - signOffset, 12)); // Pisces index = 11 (reverse movement)
    }

    private String normalizeSign(String sign) {
        if (sign == null || sign.isBlank()) {
            return "Aries";
        }
        String s = sign.trim();
        String normalized = Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
        return ZODIAC_SIGNS.contains(normalized) ? normalized : "Aries";
    }

    private int getSignIndex(String sign) {
        String normalized = normalizeSign(sign);
        int index = ZODIAC_SIGNS.indexOf(normalized);
        return index >= 0 ? index : 0;
    }

    private String getSadeSatiCharacteristics(String moonSign) {
        return "Mixed results. Challenges lead to spiritual growth. Focus on dharma (duty).";
    }

    private List<String> getSadeSatiRemedies() {
        return List.of(
                "Wear Blue Sapphire (consult astrologer)",
                "Recite Saturn mantras daily",
                "Fast on Saturdays",
                "Serve elderly people",
                "Practice meditation and yoga"
        );
    }

    private String getNextSadeSatiDate(String moonSign, LocalDate currentDate) {
        int moonIndex = getSignIndex(moonSign);
        int targetSaturnSign = Math.floorMod(moonIndex - 1, 12); // start of Sade Sati
        for (int yearOffset = 1; yearOffset <= 40; yearOffset++) {
            LocalDate checkDate = currentDate.plusYears(yearOffset);
            int saturnIndex = getSignIndex(getApproxSaturnTransitSign(checkDate));
            if (saturnIndex == targetSaturnSign) {
                return LocalDate.of(checkDate.getYear(), 1, 1).toString();
            }
        }
        return currentDate.plusYears(10).toString();
    }

    private String getDhaiyaCharacteristics(String moonSign) {
        return "Minor challenges. Not as intense as Sade Sati. Growth through experience.";
    }

    private List<String> getDhaiyaRemedies() {
        return List.of(
                "Recite Saturn mantras",
                "Meditate regularly",
                "Give to charity",
                "Serve the poor",
                "Practice patience and discipline"
        );
    }

    private String getNextDhaiyaDate(String moonSign, LocalDate currentDate) {
        int moonIndex = getSignIndex(moonSign);
        int targetOne = Math.floorMod(moonIndex + 3, 12);
        int targetTwo = Math.floorMod(moonIndex + 7, 12);
        for (int yearOffset = 1; yearOffset <= 30; yearOffset++) {
            LocalDate checkDate = currentDate.plusYears(yearOffset);
            int saturnIndex = getSignIndex(getApproxSaturnTransitSign(checkDate));
            if (saturnIndex == targetOne || saturnIndex == targetTwo) {
                return LocalDate.of(checkDate.getYear(), 1, 1).toString();
            }
        }
        return currentDate.plusYears(5).toString();
    }

    private String getSignColor(String sunSign) {
        return switch (sunSign.toLowerCase()) {
            case "aries" -> "Red";
            case "taurus" -> "Green";
            case "gemini" -> "Yellow";
            case "cancer" -> "White";
            case "leo" -> "Gold";
            case "virgo" -> "Green";
            case "libra" -> "Blue";
            case "scorpio" -> "Red";
            case "sagittarius" -> "Yellow";
            case "capricorn" -> "Black/Blue";
            case "aquarius" -> "Blue";
            case "pisces" -> "Green";
            default -> "Neutral";
        };
    }

    private String getSignDirection(String sunSign) {
        return switch (sunSign.toLowerCase()) {
            case "aries", "leo", "sagittarius" -> "North";
            case "taurus", "virgo", "capricorn" -> "South";
            case "gemini", "libra", "aquarius" -> "East";
            case "cancer", "scorpio", "pisces" -> "West";
            default -> "Any";
        };
    }

    private String getRandomPrediction() {
        String[] predictions = {
                "new opportunities",
                "positive energy",
                "important decisions",
                "social connections",
                "financial progress"
        };
        return predictions[(int)(Math.random() * predictions.length)];
    }
}
