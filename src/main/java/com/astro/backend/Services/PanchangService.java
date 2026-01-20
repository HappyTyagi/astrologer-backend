package com.astro.backend.Services;


import com.astro.backend.Helper.AstrologyHelper;
import com.astro.backend.ResponseDTO.PanchangResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swisseph.SweConst;
import swisseph.SweDate;
import swisseph.SwissEph;

import java.time.*;

@Service
@RequiredArgsConstructor
public class PanchangService {

    private final SwissEph swe = new SwissEph("libs/ephe");

    private final String[] tithiNames = {
            "Pratipada", "Dvitiya", "Tritiya", "Chaturthi", "Panchami",
            "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
            "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi",
            "Purnima/Amavasya"
    };

    private final String[] varas = {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    private final String[] yogaNames = {
            "Vishkumbha", "Preeti", "Ayushman", "Saubhagya", "Shobhana",
            "Atiganda", "Sukarma", "Dhriti", "Shoola", "Ganda",
            "Vriddhi", "Dhruva", "Vyaghata", "Harshana", "Vajra",
            "Siddhi", "Vyatipata", "Variyan", "Parigha", "Shiva",
            "Siddha", "Sadhya", "Shubha", "Shukla", "Brahma",
            "Indra", "Vaidhriti"
    };

    private final String[] karanaNames = {
            "Bava", "Balava", "Kaulava", "Taitila", "Garaja",
            "Vanija", "Vishti", "Shakuni", "Chatushpada", "Naga"
    };

    public PanchangResponse getPanchang(double lat, double lon, int dd, int mm, int yyyy,
                                        int hour, int minute, String tz) {

        ZoneId zone = ZoneId.of(tz);
        LocalDateTime ldt = LocalDateTime.of(yyyy, mm, dd, hour, minute);
        ZonedDateTime zdt = ldt.atZone(zone);
        ZonedDateTime utc = zdt.withZoneSameInstant(ZoneOffset.UTC);

        double timeDecimal = utc.getHour() + (utc.getMinute() / 60.0);
        SweDate sd = new SweDate(utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth(), timeDecimal);
        double julDay = sd.getJulDay();

        double[] sun = new double[6];
        double[] moon = new double[6];

        // ✔ FIX: initialize error buffer correctly
        StringBuffer serr = new StringBuffer();

        // ✔ Sun & Moon calculation with proper error buffer
        swe.swe_calc(julDay, SweConst.SE_SUN, SweConst.SEFLG_SWIEPH, sun, serr);
        swe.swe_calc(julDay, SweConst.SE_MOON, SweConst.SEFLG_SWIEPH, moon, serr);

        double sunLon = sun[0];
        double moonLon = moon[0];

        // TITHI
        double tithiRaw = (moonLon - sunLon) % 360;
        if (tithiRaw < 0) tithiRaw += 360;
        int tithiNumber = (int) Math.floor(tithiRaw / 12);
        String tithi = tithiNames[tithiNumber % 15];

        // VARA
        DayOfWeek dow = zdt.getDayOfWeek();
        String vara = varas[dow.getValue() % 7];

        // NAKSHATRA
        int nak = (int) (moonLon / (360.0 / 27.0));
        String nakshatra = AstrologyHelper.getNakshatraByIndex(nak);

        // YOGA
        double yogaRaw = sunLon + moonLon;
        int yogaIndex = (int) Math.floor(yogaRaw / (360.0 / 27.0)) % 27;
        String yoga = yogaNames[yogaIndex];

        // KARANA
        double karanaRaw = tithiRaw / 6.0;
        int karanaIndex = (int) karanaRaw % 60;
        String karana = karanaNames[karanaIndex % 10];

        // SUNRISE & SUNSET (Static Placeholder)
        String sunrise = "06:00";
        String sunset = "18:00";

        return PanchangResponse.builder()
                .tithi(tithi)
                .vara(vara)
                .nakshatra(nakshatra)
                .yoga(yoga)
                .karana(karana)
                .sunrise(sunrise)
                .sunset(sunset)
                .build();
    }

}
