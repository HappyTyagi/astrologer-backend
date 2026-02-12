package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.DistrictMaster;
import com.astro.backend.Entity.StateMaster;
import com.astro.backend.Entity.User;
import com.astro.backend.EnumFile.Role;
import com.astro.backend.Repositry.DistrictMasterRepository;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.RemidesPurchaseRepository;
import com.astro.backend.Repositry.StateMasterRepository;
import com.astro.backend.Repositry.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/web/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final StateMasterRepository stateMasterRepository;
    private final DistrictMasterRepository districtMasterRepository;
    private final PujaBookingRepository pujaBookingRepository;
    private final RemidesPurchaseRepository remidesPurchaseRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<?> listRegisteredUsers() {
        List<MobileUserProfile> profiles = mobileUserProfileRepository.findAll();
        Set<Long> stateIds = profiles.stream()
                .map(MobileUserProfile::getStateMasterId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Set<Long> districtIds = profiles.stream()
                .map(MobileUserProfile::getDistrictMasterId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Map<Long, String> stateNameMap = stateMasterRepository.findAllById(stateIds).stream()
                .collect(Collectors.toMap(StateMaster::getId, StateMaster::getName, (a, b) -> a));
        Map<Long, String> districtNameMap = districtMasterRepository.findAllById(districtIds).stream()
                .collect(Collectors.toMap(DistrictMaster::getId, DistrictMaster::getName, (a, b) -> a));

        List<Map<String, Object>> rows = profiles.stream().map(profile -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", profile.getId());
            row.put("userId", profile.getUserId());
            row.put("name", profile.getName());
            row.put("mobileNumber", profile.getMobileNumber());
            row.put("email", profile.getEmail());
            row.put("dateOfBirth", profile.getDateOfBirth());
            row.put("birthTime", profile.getBirthTime());
            row.put("stateMasterId", profile.getStateMasterId());
            row.put("districtMasterId", profile.getDistrictMasterId());
            row.put("stateName", stateNameMap.getOrDefault(profile.getStateMasterId(), ""));
            row.put("districtName", districtNameMap.getOrDefault(profile.getDistrictMasterId(), ""));
            row.put("isActive", !Boolean.FALSE.equals(profile.getIsActive()));
            row.put("isProfileComplete", profile.getIsProfileComplete());
            row.put("role", "USER");
            row.put("createdAt", profile.getCreatedAt());
            row.put("updatedAt", profile.getUpdatedAt());
            return row;
        }).toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/summary")
    public ResponseEntity<?> userSummary() {
        List<MobileUserProfile> profiles = mobileUserProfileRepository.findAll();
        long activeUsers = profiles.stream().filter(p -> !Boolean.FALSE.equals(p.getIsActive())).count();
        long totalUsers = profiles.size();

        long remedyBookings = remidesPurchaseRepository.count();
        long pujaBookings = pujaBookingRepository.count();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", true);
        resp.put("totalRegisteredUsers", totalUsers);
        resp.put("activeUsers", activeUsers);
        resp.put("inactiveUsers", totalUsers - activeUsers);
        resp.put("totalRemedyBookings", remedyBookings);
        resp.put("totalPujaBookings", pujaBookings);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/upcoming-birthdays")
    public ResponseEntity<?> upcomingBirthdays(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(defaultValue = "10") Integer limit) {
        int safeDays = days == null ? 7 : Math.max(1, Math.min(days, 30));
        int safeLimit = limit == null ? 10 : Math.max(1, Math.min(limit, 100));
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(safeDays - 1L);

        List<Map<String, Object>> items = mobileUserProfileRepository.findAll().stream()
                .filter(p -> !Boolean.FALSE.equals(p.getIsActive()))
                .map(profile -> buildUpcomingBirthdayRow(profile, today, windowEnd))
                .filter(row -> row != null)
                .sorted(Comparator
                        .comparing((Map<String, Object> row) -> (String) row.get("upcomingBirthdayDate"))
                        .thenComparing(row -> String.valueOf(row.get("name"))))
                .limit(safeLimit)
                .toList();

        Map<String, List<Map<String, Object>>> byDate = items.stream()
                .collect(Collectors.groupingBy(
                        row -> String.valueOf(row.get("upcomingBirthdayDate")),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("days", safeDays);
        response.put("limit", safeLimit);
        response.put("total", items.size());
        response.put("items", items);
        response.put("byDate", byDate);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-password")
    public ResponseEntity<?> updateAdminPassword(@RequestBody UpdatePasswordRequest request) {
        String principalEmail = String.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        User user = userRepository.findByEmail(principalEmail).orElse(null);

        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", false,
                    "message", "Access denied"
            ));
        }

        String oldPassword = request.getOldPassword() == null ? "" : request.getOldPassword().trim();
        String newPassword = request.getNewPassword() == null ? "" : request.getNewPassword().trim();
        String confirmPassword = request.getConfirmPassword() == null ? "" : request.getConfirmPassword().trim();

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", false,
                    "message", "Old password, new password and confirm password are required"
            ));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", false,
                    "message", "New password and confirm password do not match"
            ));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", false,
                    "message", "New password must be at least 6 characters"
            ));
        }

        String storedPassword = user.getPassword() == null ? "" : user.getPassword();
        boolean oldPasswordMatched;
        try {
            oldPasswordMatched = passwordEncoder.matches(oldPassword, storedPassword);
        } catch (Exception ignored) {
            oldPasswordMatched = false;
        }
        if (!oldPasswordMatched && storedPassword.equals(oldPassword)) {
            oldPasswordMatched = true;
        }

        if (!oldPasswordMatched) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", false,
                    "message", "Old password is incorrect"
            ));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Password updated successfully"
        ));
    }

    @Data
    private static class UpdatePasswordRequest {
        private String oldPassword;
        private String newPassword;
        private String confirmPassword;
    }

    private Map<String, Object> buildUpcomingBirthdayRow(
            MobileUserProfile profile,
            LocalDate today,
            LocalDate windowEnd) {
        LocalDate dob = parseDateOfBirth(profile.getDateOfBirth());
        if (dob == null) return null;

        LocalDate upcoming = nextBirthdayDate(dob, today);
        if (upcoming.isBefore(today) || upcoming.isAfter(windowEnd)) return null;

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", profile.getId());
        row.put("userId", profile.getUserId());
        row.put("name", profile.getName());
        row.put("mobileNumber", profile.getMobileNumber());
        row.put("email", profile.getEmail());
        row.put("dateOfBirth", profile.getDateOfBirth());
        row.put("upcomingBirthdayDate", upcoming.toString());
        row.put("daysLeft", java.time.temporal.ChronoUnit.DAYS.between(today, upcoming));
        return row;
    }

    private LocalDate parseDateOfBirth(String value) {
        if (value == null || value.isBlank()) return null;
        String dob = value.trim();
        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );
        for (DateTimeFormatter fmt : formats) {
            try {
                return LocalDate.parse(dob, fmt);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private LocalDate nextBirthdayDate(LocalDate dob, LocalDate today) {
        MonthDay birthDay = MonthDay.from(dob);
        LocalDate candidate = safeAtYear(birthDay, today.getYear());
        if (candidate.isBefore(today)) {
            candidate = safeAtYear(birthDay, today.getYear() + 1);
        }
        return candidate;
    }

    private LocalDate safeAtYear(MonthDay monthDay, int year) {
        try {
            return monthDay.atYear(year);
        } catch (Exception ignored) {
            if (monthDay.getMonthValue() == 2 && monthDay.getDayOfMonth() == 29) {
                return LocalDate.of(year, 2, 28);
            }
            throw ignored;
        }
    }
}
