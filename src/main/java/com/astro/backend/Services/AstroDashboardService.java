package com.astro.backend.Services;

import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.User;
import com.astro.backend.Helper.AstrologyHelper;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.ResponseDTO.AstroDashboardResponse;
import com.astro.backend.ResponseDTO.KundliResponse;
import com.astro.backend.ResponseDTO.PlanetPosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AstroDashboardService {

    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final KundliService kundliService;

    public AstroDashboardResponse getDashboardByMobile(String mobileNo) {
        String mobile = AstrologyHelper.sanitizeString(mobileNo);

        if (!AstrologyHelper.isValidMobileNumber(mobile)) {
            return AstroDashboardResponse.builder()
                    .status(false)
                    .message("Invalid mobile number")
                    .build();
        }

        Optional<MobileUserProfile> profileOpt = mobileUserProfileRepository.findByMobileNumber(mobile);
        if (profileOpt.isEmpty()) {
            return AstroDashboardResponse.builder()
                    .status(false)
                    .message("Profile not found")
                    .build();
        }

        MobileUserProfile profile = profileOpt.get();

        if (profile.getDateOfBirth() == null || profile.getDateOfBirth().isEmpty()) {
            return AstroDashboardResponse.builder()
                    .status(false)
                    .message("Date of birth is missing")
                    .build();
        }

        if (profile.getLatitude() == null || profile.getLongitude() == null) {
            return AstroDashboardResponse.builder()
                    .status(false)
                    .message("Location is missing")
                    .build();
        }

        LocalDate dob;
        try {
            dob = LocalDate.parse(profile.getDateOfBirth(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return AstroDashboardResponse.builder()
                    .status(false)
                    .message("Invalid date of birth format")
                    .build();
        }

        double time = parseBirthTimeToDouble(profile.getBirthTime(), profile.getBirthAmPm());
        if (Double.isNaN(time)) {
            time = 12.00; // Default time when birth time is not stored
        }

        KundliResponse kundli = kundliService.generateKundli(
                profile.getLatitude(),
                profile.getLongitude(),
                dob.getDayOfMonth(),
                dob.getMonthValue(),
                dob.getYear(),
                time
        );

        String sunSign = getPlanetRashi(kundli.getPlanets(), "Sun");
        String moonSign = getPlanetRashi(kundli.getPlanets(), "Moon");
        String lagnaSign = kundli.getAscendant();
        String nakshatra = kundli.getNakshatra();

        LuckyInfo luckyInfo = getLuckyInfo(moonSign != null ? moonSign : sunSign);

        AstroDashboardResponse response = AstroDashboardResponse.builder()
                .status(true)
                .message("Dashboard data fetched successfully")
                .userId(profile.getId())
                .name(profile.getName())
                .mobileNo(profile.getMobileNumber())
                .sunSign(sunSign)
                .moonSign(moonSign)
                .lagnaSign(lagnaSign)
                .nakshatra(nakshatra)
                .luckyColor(luckyInfo.color)
                .luckyNumber(luckyInfo.number)
                .luckyDay(luckyInfo.day)
                .luckyGemstone(luckyInfo.gemstone)
                .auspiciousTime(luckyInfo.auspiciousTime)
            .build();

        log.info("Astro dashboard response: {}", response);
        return response;
    }

    private String getPlanetRashi(List<PlanetPosition> planets, String planetName) {
        if (planets == null) {
            return null;
        }
        return planets.stream()
                .filter(p -> planetName.equalsIgnoreCase(p.getPlanet()))
                .map(PlanetPosition::getRashi)
                .findFirst()
                .orElse(null);
    }

    private double parseBirthTimeToDouble(String birthTime, String amPm) {
        if (birthTime == null || birthTime.isEmpty() || amPm == null || amPm.isEmpty()) {
            return Double.NaN;
        }
        try {
            String[] parts = birthTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            String meridian = amPm.trim().toUpperCase();
            if (hour == 12) {
                hour = 0;
            }
            if ("PM".equals(meridian)) {
                hour += 12;
            }

            return hour + (minute / 100.0);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private LuckyInfo getLuckyInfo(String sign) {
        if (sign == null) {
            return new LuckyInfo("White", 7, "Thursday", "Pearl", "07:00-09:00");
        }
        return switch (sign.toLowerCase()) {
            case "aries" -> new LuckyInfo("Red", 9, "Tuesday", "Ruby", "06:00-08:00");
            case "taurus" -> new LuckyInfo("Green", 6, "Friday", "Emerald", "09:00-11:00");
            case "gemini" -> new LuckyInfo("Yellow", 5, "Wednesday", "Emerald", "10:00-12:00");
            case "cancer" -> new LuckyInfo("White", 2, "Monday", "Pearl", "08:00-10:00");
            case "leo" -> new LuckyInfo("Orange", 1, "Sunday", "Ruby", "07:00-09:00");
            case "virgo" -> new LuckyInfo("Green", 5, "Wednesday", "Emerald", "09:00-11:00");
            case "libra" -> new LuckyInfo("Pink", 6, "Friday", "Diamond", "10:00-12:00");
            case "scorpio" -> new LuckyInfo("Maroon", 9, "Tuesday", "Red Coral", "06:00-08:00");
            case "sagittarius" -> new LuckyInfo("Purple", 3, "Thursday", "Yellow Sapphire", "08:00-10:00");
            case "capricorn" -> new LuckyInfo("Blue", 8, "Saturday", "Blue Sapphire", "07:00-09:00");
            case "aquarius" -> new LuckyInfo("Blue", 4, "Saturday", "Amethyst", "09:00-11:00");
            case "pisces" -> new LuckyInfo("Sea Green", 3, "Thursday", "Yellow Sapphire", "10:00-12:00");
            default -> new LuckyInfo("White", 7, "Thursday", "Pearl", "07:00-09:00");
        };
    }

    private static class LuckyInfo {
        private final String color;
        private final Integer number;
        private final String day;
        private final String gemstone;
        private final String auspiciousTime;

        private LuckyInfo(String color, Integer number, String day, String gemstone, String auspiciousTime) {
            this.color = color;
            this.number = number;
            this.day = day;
            this.gemstone = gemstone;
            this.auspiciousTime = auspiciousTime;
        }
    }
}
