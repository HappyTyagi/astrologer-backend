package com.astro.backend.Contlorer;


import com.astro.backend.ResponseDTO.KundliResponse;
import com.astro.backend.ResponseDTO.PanchangResponse;
import com.astro.backend.Services.KundliService;
import com.astro.backend.Services.PanchangService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/astrology")
@RequiredArgsConstructor
public class AstrologyController {

    private final KundliService kundliService;
    private final PanchangService panchangService;

    @GetMapping("/kundli")
    public KundliResponse kundli(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam int dd,
            @RequestParam int mm,
            @RequestParam int yyyy,
            @RequestParam double time // HH.MM (e.g., 14.30)
    ) {
        return kundliService.generateKundli(lat, lon, dd, mm, yyyy, time);
    }



    @GetMapping("/panchang")
    public PanchangResponse panchang(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam int dd,
            @RequestParam int mm,
            @RequestParam int yyyy,
            @RequestParam int hour,
            @RequestParam int min,
            @RequestParam String tz // e.g. "Asia/Kolkata"
    ) {
        return panchangService.getPanchang(lat, lon, dd, mm, yyyy, hour, min, tz);
    }


}
