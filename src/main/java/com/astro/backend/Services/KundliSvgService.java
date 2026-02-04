package com.astro.backend.Services;

import org.springframework.stereotype.Service;
import swisseph.*;

import java.util.*;

@Service
public class KundliSvgService {

    private static final SwissEph swe = new SwissEph("libs/ephe");

    // =========================
    // HOUSE → SVG CENTER POSITIONS
    // =========================
    private static final int[][] HOUSE_POS = {
            {},                 // 0 dummy
            {350, 170},         // 1
            {470, 240},         // 2
            {500, 350},         // 3
            {350, 470},         // 4
            {230, 470},         // 5
            {200, 350},         // 6
            {230, 240},         // 7
            {350, 560},         // 8
            {470, 560},         // 9
            {560, 470},         // 10
            {580, 350},         // 11
            {560, 240}          // 12
    };

    public String generateKundliSvg(
            int year, int month, int day,
            int hour, int minute, int second,
            double latitude, double longitude,
            double timezone
    ) {

        try {
            // ===== Julian Day =====
            SweDate sd = new SweDate(
                    year, month, day,
                    hour + minute / 60.0 + second / 3600.0
            );
            double jd = sd.getJulDay() - timezone / 24.0;
            int flags = SweConst.SEFLG_SWIEPH;

            Map<Integer, List<String>> housePlanets = new HashMap<>();

            // ===== Ascendant SIGN =====
            double[] asc = new double[6];
            swe.swe_calc(jd, SweConst.SE_ASC, flags, asc, new StringBuffer());
            int ascSign = ((int) (asc[0] / 30)) + 1;

            // Ascendant marker
            housePlanets.computeIfAbsent(1, k -> new ArrayList<>()).add("As");

            // ===== Planets =====
            String[] names = {"Ra", "Ma", "Me", "Su", "Sa", "Ju", "Ve", "Ke", "Mo"};
            int[] ids = {
                    SweConst.SE_TRUE_NODE,
                    SweConst.SE_MARS,
                    SweConst.SE_MERCURY,
                    SweConst.SE_SUN,
                    SweConst.SE_SATURN,
                    SweConst.SE_JUPITER,
                    SweConst.SE_VENUS,
                    SweConst.SE_TRUE_NODE,
                    SweConst.SE_MOON
            };

            for (int i = 0; i < names.length; i++) {
                double[] xx = new double[6];
                swe.swe_calc(jd, ids[i], flags, xx, new StringBuffer());

                double lonp = xx[0];
                if (names[i].equals("Ke")) {
                    lonp = (lonp + 180) % 360;
                }

                int planetSign = ((int) (lonp / 30)) + 1;

                // SIGN → HOUSE (North Indian rule)
                int house = ((planetSign - ascSign + 12) % 12) + 1;

                housePlanets.computeIfAbsent(house, k -> new ArrayList<>()).add(names[i]);
            }

            return drawSvg(housePlanets, ascSign);

        } catch (Exception e) {
            return errorSvg(e.getMessage());
        }
    }

    // =========================
    // SVG DRAWING
    // =========================
    private String drawSvg(Map<Integer, List<String>> data, int ascSign) {

        StringBuilder svg = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <svg width="700" height="700" xmlns="http://www.w3.org/2000/svg">
                <rect width="100%" height="100%" fill="#f5e6d3"/>
                """);

        // ===== GRID =====
        svg.append("""
                <polygon points="350,70 630,350 350,630 70,350"
                         fill="none" stroke="black" stroke-width="2"/>

                <line x1="70" y1="350" x2="630" y2="350" stroke="black"/>
                <line x1="350" y1="70" x2="350" y2="630" stroke="black"/>

                <line x1="70" y1="350" x2="350" y2="70" stroke="black"/>
                <line x1="350" y1="70" x2="630" y2="350" stroke="black"/>
                <line x1="630" y1="350" x2="350" y2="630" stroke="black"/>
                <line x1="350" y1="630" x2="70" y2="350" stroke="black"/>
                """);

        // ===== HOUSE + RASHI NUMBERS =====
        for (int house = 1; house <= 12; house++) {
            int[] p = HOUSE_POS[house];

            // Rashi number rotation
            int rashi = ((ascSign + house - 2) % 12) + 1;

            // House number (small, black)
            svg.append(String.format(
                    "<text x='%d' y='%d' font-size='12' fill='black' text-anchor='middle'>H%d</text>",
                    p[0], p[1] - 18, house
            ));

            // Rashi number (bold)
            svg.append(String.format(
                    "<text x='%d' y='%d' font-size='16' fill='black' font-weight='bold' text-anchor='middle'>%d</text>",
                    p[0], p[1] - 2, rashi
            ));
        }

        // ===== PLANETS =====
        data.forEach((house, planets) -> {
            int[] p = HOUSE_POS[house];
            svg.append(String.format(
                    "<text x='%d' y='%d' font-size='20' fill='#7a0000' " +
                            "font-weight='bold' text-anchor='middle'>%s</text>",
                    p[0], p[1] + 18, String.join(" ", planets)
            ));
        });

        svg.append("</svg>");
        return svg.toString();
    }

    private String errorSvg(String msg) {
        return "<svg width='700' height='700'>" +
                "<text x='50%' y='50%' fill='red'>" + msg + "</text></svg>";
    }
}
