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

}
