package com.astro.backend.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MuhuratService {

    /**
     * Find auspicious muhurat (timing) for various life events
     * @param eventType Type of event (marriage, business, journey, etc.)
     * @param startDate Date from which to find muhurat
     * @param duration Duration in days to search
     * @return List of auspicious timings
     */
    public List<MuhuratSlot> findAuspiciousMuhurat(String eventType, LocalDate startDate, int duration) {
        try {
            List<MuhuratSlot> auspiciousTimings = new ArrayList<>();

            for (int day = 0; day < duration; day++) {
                LocalDate checkDate = startDate.plusDays(day);
                
                if (isValidMuhuratDay(checkDate, eventType)) {
                    List<LocalTime> timeSlots = getAuspiciousTimeSlots(checkDate, eventType);
                    
                    for (LocalTime time : timeSlots) {
                        auspiciousTimings.add(MuhuratSlot.builder()
                                .date(checkDate)
                                .time(time)
                                .event(eventType)
                                .nakshatra(getNakshatraForDate(checkDate))
                                .tithi(getTithiForDate(checkDate))
                                .dayOfWeek(checkDate.getDayOfWeek().toString())
                                .duration("2 hours")
                                .auspiciousityScore(calculateAuspiciousityScore(checkDate, time, eventType))
                                .build());
                    }
                }
            }

            // Sort by auspiciousness score
            auspiciousTimings.sort((a, b) -> b.auspiciousityScore - a.auspiciousityScore);
            return auspiciousTimings.stream().limit(10).toList();

        } catch (Exception e) {
            log.error("Error finding muhurat", e);
            throw new RuntimeException("Failed to find muhurat: " + e.getMessage());
        }
    }

    /**
     * Check if a day is suitable for the given event
     */
    private boolean isValidMuhuratDay(LocalDate date, String eventType) {
        LocalDate today = LocalDate.now();
        
        // Cannot schedule for past dates
        if (date.isBefore(today)) {
            return false;
        }

        String tithi = getTithiForDate(date);
        int dayOfWeek = date.getDayOfWeek().getValue();

        return switch (eventType.toLowerCase()) {
            case "marriage" -> isGoodForMarriage(tithi, dayOfWeek);
            case "business" -> isGoodForBusiness(tithi, dayOfWeek);
            case "journey" -> isGoodForJourney(tithi, dayOfWeek);
            case "property" -> isGoodForProperty(tithi, dayOfWeek);
            case "education" -> isGoodForEducation(tithi, dayOfWeek);
            case "medical" -> isGoodForMedical(tithi, dayOfWeek);
            default -> true;
        };
    }

    private boolean isGoodForMarriage(String tithi, int dayOfWeek) {
        // Good tithis: 2, 5, 7, 10, 11, 13 (Avoid 4, 8, 9, 12, 14, 15)
        // Good days: Sunday, Wednesday, Thursday, Friday (Avoid Saturday)
        String[] goodTithis = {"Dwitiya", "Panchami", "Saptami", "Dashami", "Ekadashi", "Trayodashi"};
        int[] goodDays = {1, 3, 4, 5}; // Sun, Wed, Thu, Fri

        return Arrays.asList(goodTithis).contains(tithi) && Arrays.stream(goodDays).anyMatch(d -> d == dayOfWeek);
    }

    private boolean isGoodForBusiness(String tithi, int dayOfWeek) {
        // Good tithis: 1, 2, 3, 6, 7, 10, 11, 13
        String[] goodTithis = {"Pratipada", "Dwitiya", "Tritiya", "Shashthi", "Saptami", "Dashami", "Ekadashi", "Trayodashi"};
        int[] goodDays = {1, 2, 3, 4, 5}; // Not Saturday, Sunday

        return Arrays.asList(goodTithis).contains(tithi) && Arrays.stream(goodDays).anyMatch(d -> d == dayOfWeek);
    }

    private boolean isGoodForJourney(String tithi, int dayOfWeek) {
        // Avoid amavasya, purnima and ekadashi
        String[] badTithis = {"Amavasya", "Purnima", "Ekadashi"};
        int[] badDays = {7}; // Avoid Saturday

        return !Arrays.asList(badTithis).contains(tithi) && Arrays.stream(badDays).noneMatch(d -> d == dayOfWeek);
    }

    private boolean isGoodForProperty(String tithi, int dayOfWeek) {
        // Good tithis: 1, 2, 5, 7, 10, 11
        String[] goodTithis = {"Pratipada", "Dwitiya", "Panchami", "Saptami", "Dashami", "Ekadashi"};
        int[] goodDays = {1, 2, 3, 4, 5}; // Not Saturday

        return Arrays.asList(goodTithis).contains(tithi) && Arrays.stream(goodDays).anyMatch(d -> d == dayOfWeek);
    }

    private boolean isGoodForEducation(String tithi, int dayOfWeek) {
        // Good tithis: 1, 2, 5, 7, 10, 11, 13
        String[] goodTithis = {"Pratipada", "Dwitiya", "Panchami", "Saptami", "Dashami", "Ekadashi", "Trayodashi"};
        int[] goodDays = {1, 2, 3, 4, 5}; // Not Saturday

        return Arrays.asList(goodTithis).contains(tithi) && Arrays.stream(goodDays).anyMatch(d -> d == dayOfWeek);
    }

    private boolean isGoodForMedical(String tithi, int dayOfWeek) {
        // Avoid eclipse days, purnima, amavasya
        String[] badTithis = {"Amavasya", "Purnima"};

        return !Arrays.asList(badTithis).contains(tithi);
    }

    /**
     * Get auspicious time slots for a given day
     */
    private List<LocalTime> getAuspiciousTimeSlots(LocalDate date, String eventType) {
        List<LocalTime> slots = new ArrayList<>();

        // Simplified: Add morning and evening slots
        // In real implementation, calculate based on planetary hours
        
        LocalTime morningStart = LocalTime.of(6, 0);
        LocalTime afternoonStart = LocalTime.of(12, 0);
        LocalTime eveningStart = LocalTime.of(17, 0);

        slots.add(morningStart);
        slots.add(afternoonStart);
        slots.add(eveningStart);

        return slots;
    }

    /**
     * Get nakshatra for a given date (simplified)
     */
    private String getNakshatraForDate(LocalDate date) {
        String[] nakshatras = {
                "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashirsha", "Ardra", "Punarvasu",
                "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta",
                "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha",
                "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada",
                "Uttara Bhadrapada", "Revati"
        };

        int dayOfYear = date.getDayOfYear();
        return nakshatras[dayOfYear % nakshatras.length];
    }

    /**
     * Get tithi for a given date (simplified)
     */
    private String getTithiForDate(LocalDate date) {
        String[] tithis = {
                "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami", "Shashthi", "Saptami",
                "Ashtami", "Navami", "Dashami", "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi",
                "Purnima"
        };

        int dayOfMonth = date.getDayOfMonth();
        return tithis[dayOfMonth % tithis.length];
    }

    /**
     * Calculate auspiciousness score (0-100)
     */
    private int calculateAuspiciousityScore(LocalDate date, LocalTime time, String eventType) {
        int score = 70;

        // Bonus for morning
        if (time.getHour() >= 6 && time.getHour() <= 9) {
            score += 10;
        }

        // Bonus for evening
        if (time.getHour() >= 17 && time.getHour() <= 19) {
            score += 5;
        }

        // Bonus for auspicious days
        if (date.getDayOfWeek().getValue() == 4) { // Thursday
            score += 5;
        }

        return Math.min(score, 100);
    }

    /**
     * Find auspicious dates for a month
     */
    public Map<String, Object> getMonthlyAuspiciousDates(LocalDate monthStart, String eventType) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<LocalDate> dates = new ArrayList<>();

        LocalDate current = monthStart;
        for (int day = 0; day < 30; day++) {
            if (isValidMuhuratDay(current, eventType)) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }

        result.put("event", eventType);
        result.put("month", monthStart.getMonth().toString());
        result.put("auspiciousDates", dates);
        result.put("totalDays", dates.size());
        result.put("recommendation", "Best dates for " + eventType + ": " + 
                (dates.isEmpty() ? "No auspicious dates found" : dates.stream().limit(5).toList()));

        return result;
    }

    /**
     * Muhurat Slot DTO
     */
    public static class MuhuratSlot {
        public LocalDate date;
        public LocalTime time;
        public String event;
        public String nakshatra;
        public String tithi;
        public String dayOfWeek;
        public String duration;
        public int auspiciousityScore;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private LocalDate date;
            private LocalTime time;
            private String event;
            private String nakshatra;
            private String tithi;
            private String dayOfWeek;
            private String duration;
            private int auspiciousityScore;

            public Builder date(LocalDate date) {
                this.date = date;
                return this;
            }

            public Builder time(LocalTime time) {
                this.time = time;
                return this;
            }

            public Builder event(String event) {
                this.event = event;
                return this;
            }

            public Builder nakshatra(String nakshatra) {
                this.nakshatra = nakshatra;
                return this;
            }

            public Builder tithi(String tithi) {
                this.tithi = tithi;
                return this;
            }

            public Builder dayOfWeek(String dayOfWeek) {
                this.dayOfWeek = dayOfWeek;
                return this;
            }

            public Builder duration(String duration) {
                this.duration = duration;
                return this;
            }

            public Builder auspiciousityScore(int score) {
                this.auspiciousityScore = score;
                return this;
            }

            public MuhuratSlot build() {
                MuhuratSlot slot = new MuhuratSlot();
                slot.date = this.date;
                slot.time = this.time;
                slot.event = this.event;
                slot.nakshatra = this.nakshatra;
                slot.tithi = this.tithi;
                slot.dayOfWeek = this.dayOfWeek;
                slot.duration = this.duration;
                slot.auspiciousityScore = this.auspiciousityScore;
                return slot;
            }
        }
    }
}
