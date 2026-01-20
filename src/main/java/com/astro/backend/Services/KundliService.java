package com.astro.backend.Services;


import com.astro.backend.ResponseDTO.KundliResponse;
import com.astro.backend.ResponseDTO.PlanetPosition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swisseph.SweConst;
import swisseph.SweDate;
import swisseph.SwissEph;

import java.util.*;

@Service
@RequiredArgsConstructor
public class KundliService {

    private static final SweDate sweDate = new SweDate();
    private static final SwissEph swissEph = new SwissEph("libs/ephe");

    private static final Map<Integer, String> RASHI_MAP = Map.ofEntries(
            Map.entry(0, "Aries"), Map.entry(1, "Taurus"), Map.entry(2, "Gemini"),
            Map.entry(3, "Cancer"), Map.entry(4, "Leo"), Map.entry(5, "Virgo"),
            Map.entry(6, "Libra"), Map.entry(7, "Scorpio"), Map.entry(8, "Sagittarius"),
            Map.entry(9, "Capricorn"), Map.entry(10, "Aquarius"), Map.entry(11, "Pisces")
    );

    public KundliResponse generateKundli(double lat, double lon, int dd, int mm, int yyyy, double time) {

        // Convert to Julian Day
        SweDate sd = new SweDate(yyyy, mm, dd, time);
        double julDay = sd.getJulDay();

        int[] planets = {SweConst.SE_SUN, SweConst.SE_MOON, SweConst.SE_MARS,
                SweConst.SE_MERCURY, SweConst.SE_JUPITER, SweConst.SE_VENUS,
                SweConst.SE_SATURN, SweConst.SE_TRUE_NODE};

        List<PlanetPosition> planetPositions = new ArrayList<>();

        for (int p : planets) {
            double[] xx = new double[6];
            StringBuffer serr = new StringBuffer();

            swissEph.swe_calc(julDay, p, SweConst.SEFLG_SWIEPH, xx, serr);
            double lonDeg = xx[0];
            int rashi = (int) (lonDeg / 30);

            planetPositions.add(PlanetPosition.builder()
                    .planet(getPlanetName(p))
                    .longitude(lonDeg)
                    .rashi(RASHI_MAP.get(rashi))
                    .build());
        }

        // Compute Ascendant
        double[] cusps = new double[13];
        double[] ascmc = new double[10];

        int flags = SweConst.SEFLG_SWIEPH;
        swissEph.swe_houses(julDay, flags, lat, lon, 'P', cusps, ascmc);


        double ascLong = ascmc[0];
        int ascRashi = (int) (ascLong / 30);

        return KundliResponse.builder()
                .planets(planetPositions)
                .ascendant(RASHI_MAP.get(ascRashi))
                .nakshatra(getNakshatra(planetPositions.get(1).getLongitude()))
                .build();
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
            default -> "Unknown";
        };
    }

    private String getNakshatra(double moonLong) {
        int nak = (int) (moonLong / (360.0 / 27.0));
        String[] list = {"Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashirsha", "Ardra", "Punarvasu",
                "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta",
                "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha",
                "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada",
                "Uttara Bhadrapada", "Revati"};
        return list[nak];
    }
}
