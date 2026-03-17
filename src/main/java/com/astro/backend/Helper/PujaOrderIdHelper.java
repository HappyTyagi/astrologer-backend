package com.astro.backend.Helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PujaOrderIdHelper {

    private static final Pattern BOOKING_ID_PATTERN = Pattern.compile("B(\\d+)");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private PujaOrderIdHelper() {
    }

    public static String build(Long userId, Long bookingId) {
        long safeUserId = userId == null || userId < 0 ? 0 : userId;
        long safeBookingId = bookingId == null || bookingId < 0 ? 0 : bookingId;
        return "PUJA-U" + pad(safeUserId, 5) + "-B" + pad(safeBookingId, 6);
    }

    public static Long parseBookingId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Valid puja orderId is required");
        }
        final String normalized = orderId.trim().toUpperCase();

        Matcher bookingMatcher = BOOKING_ID_PATTERN.matcher(normalized);
        String bookingDigits = null;
        while (bookingMatcher.find()) {
            bookingDigits = bookingMatcher.group(1);
        }
        if (bookingDigits != null && !bookingDigits.isBlank()) {
            return Long.parseLong(bookingDigits);
        }

        final String legacyValue = normalized.startsWith("PUJA-")
                ? normalized.substring(5).trim()
                : normalized;
        if (legacyValue.matches("\\d+")) {
            return Long.parseLong(legacyValue);
        }

        Matcher digitsMatcher = NUMBER_PATTERN.matcher(normalized);
        String lastDigits = null;
        while (digitsMatcher.find()) {
            lastDigits = digitsMatcher.group(1);
        }
        if (lastDigits != null) {
            return Long.parseLong(lastDigits);
        }

        throw new IllegalArgumentException("Invalid puja orderId: " + orderId);
    }

    private static String pad(long value, int width) {
        String raw = String.valueOf(value);
        if (raw.length() >= width) {
            return raw;
        }
        return "0".repeat(width - raw.length()) + raw;
    }
}
