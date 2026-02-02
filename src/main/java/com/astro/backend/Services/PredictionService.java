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

        transitAnalysis.put("date", date.toString());
        transitAnalysis.put("birthChart", birthChart);
        transitAnalysis.put("majorTransits", getMajorTransits(date));
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

        boolean isSadeSati = isCurrenltyInSadeSati(moonSign, currentDate);
        
        sadeSati.put("moonSign", moonSign);
        sadeSati.put("inSadeSati", isSadeSati);
        
        if (isSadeSati) {
            sadeSati.put("phase", getCurrentSadeSatiPhase(moonSign, currentDate));
            sadeSati.put("expectedDuration", "7.5 years");
            sadeSati.put("characteristics", getSadeSatiCharacteristics(moonSign));
            sadeSati.put("remedies", getSadeSatiRemedies());
            sadeSati.put("advice", "Remain patient, practice spirituality, and maintain discipline");
        } else {
            sadeSati.put("nextSadeSatiDate", getNextSadeSatiDate(moonSign, currentDate));
            sadeSati.put("message", "You are not in Sade Sati period. Enjoy this peaceful phase.");
        }

        return sadeSati;
    }

    /**
     * Dhaiya (2.5 year Saturn transit) analysis
     */
    public Map<String, Object> getDhaiyaAnalysis(String moonSign, LocalDate currentDate) {
        Map<String, Object> dhaiya = new LinkedHashMap<>();

        boolean isDhaiya = isCurrentlyInDhaiya(moonSign, currentDate);
        
        dhaiya.put("moonSign", moonSign);
        dhaiya.put("inDhaiya", isDhaiya);
        
        if (isDhaiya) {
            dhaiya.put("duration", "2.5 years");
            dhaiya.put("characteristics", getDhaiyaCharacteristics(moonSign));
            dhaiya.put("affectedLife", new String[]{"Career", "Relationships", "Finance", "Health"});
            dhaiya.put("remedies", getDhaiyaRemedies());
            dhaiya.put("advice", "Stay focused and positive. This too shall pass.");
        } else {
            dhaiya.put("nextDhaiyaDate", getNextDhaiyaDate(moonSign, currentDate));
            dhaiya.put("message", "You are not in Dhaiya period.");
        }

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

    private List<String> getMajorTransits(LocalDate date) {
        return List.of(
                "Mercury transiting through Gemini - Communication enhancement",
                "Venus in Libra - Love and relationships favorable",
                "Mars in Aries - Increased energy and motivation"
        );
    }

    private List<String> getAffectedSigns(LocalDate date) {
        return List.of("Aries", "Leo", "Sagittarius", "Gemini", "Libra", "Aquarius");
    }

    private Map<String, String> getTransitDurationAndImpact(LocalDate date) {
        Map<String, String> impact = new LinkedHashMap<>();
        impact.put("duration", "30 days");
        impact.put("intensity", "Moderate");
        impact.put("overallImpact", "Positive growth expected");
        return impact;
    }

    private String getTransitRecommendations(LocalDate date) {
        return "Use this transit period to launch new projects. Favorable for career advancement.";
    }

    private boolean isCurrenltyInSadeSati(String moonSign, LocalDate date) {
        // Simplified logic - in real implementation, calculate based on Saturn position
        return false;
    }

    private String getCurrentSadeSatiPhase(String moonSign, LocalDate date) {
        return "First phase (Rising Saturn) - Challenges and changes";
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
        return currentDate.plusYears(10).toString();
    }

    private boolean isCurrentlyInDhaiya(String moonSign, LocalDate currentDate) {
        // Simplified logic
        return false;
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
