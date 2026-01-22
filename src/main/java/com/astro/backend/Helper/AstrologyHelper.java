package com.astro.backend.Helper;

public class AstrologyHelper {

    private static final String[] nakList = {
            "Ashwini","Bharani","Krittika","Rohini","Mrigashirsha","Ardra","Punarvasu","Pushya","Ashlesha",
            "Magha","Purva Phalguni","Uttara Phalguni","Hasta","Chitra","Swati","Vishakha","Anuradha",
            "Jyeshtha","Mula","Purva Ashadha","Uttara Ashadha","Shravana","Dhanishta","Shatabhisha",
            "Purva Bhadrapada","Uttara Bhadrapada","Revati"
    };

    public static String getNakshatraByIndex(int idx) {
        return nakList[idx % 27];
    }

    // ===== UTILITY METHODS =====

    /**
     * Mask mobile number for display security
     * Example: 9876543210 → ****3210
     */
    public static String maskMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.length() < 4) {
            return "****";
        }
        return "****" + mobileNumber.substring(mobileNumber.length() - 4);
    }

    /**
     * Validate mobile number format
     * Must be exactly 10 digits
     */
    public static boolean isValidMobileNumber(String mobileNumber) {
        if (mobileNumber == null) {
            return false;
        }
        return mobileNumber.matches("^[0-9]{10}$");
    }

    /**
     * Validate OTP format
     * Must be exactly 6 digits
     */
    public static boolean isValidOtp(String otp) {
        if (otp == null) {
            return false;
        }
        return otp.matches("^[0-9]{6}$");
    }

    /**
     * Validate name format
     * Must be 2-50 characters
     */
    public static boolean isValidName(String name) {
        if (name == null) {
            return false;
        }
        return name.length() >= 2 && name.length() <= 50;
    }

    /**
     * Sanitize input string
     * Remove leading/trailing spaces
     */
    public static String sanitizeString(String input) {
        if (input == null) {
            return null;
        }
        return input.trim();
    }

    /**
     * Check if reference number is valid
     * Must be non-empty and alphanumeric
     */
    public static boolean isValidRefNumber(String refNumber) {
        if (refNumber == null || refNumber.isEmpty()) {
            return false;
        }
        return refNumber.matches("^[a-zA-Z0-9]+$");
    }
}

