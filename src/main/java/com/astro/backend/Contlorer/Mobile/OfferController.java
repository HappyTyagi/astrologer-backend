package com.astro.backend.Contlorer.Mobile;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mobile/offers")
public class OfferController {
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getOffers() {
        List<Map<String, Object>> offers = Arrays.asList(
            Map.of(
                "title", "9 Mukhi Rudraksha",
                "description", "Removes fear, builds strength, & shields you from negativity",
                "imageUrl", "https://yourcdn.com/images/rudraksha9.png",
                "type", "Remedies",
                "buttonText", "Shop Now"
            ),
            Map.of(
                "title", "Green Aventurine Bracelet",
                "description", "Attracts prosperity, luck & positive energy for growth!",
                "imageUrl", "https://yourcdn.com/images/green_aventurine.png",
                "type", "Remedies",
                "buttonText", "Shop Now"
            ),
            Map.of(
                "title", "1-14 Mukhi Rudraksha Mala",
                "description", "Blessings of Health, Wealth & Spiritual Growth",
                "imageUrl", "https://yourcdn.com/images/rudraksha_mala.png",
                "type", "Remedies",
                "buttonText", "Shop Now"
            ),
            Map.of(
                "title", "Embrace Devotion and Blessings with Ekadashi Puja",
                "description", "Participate in Spiritual Puja from Home",
                "imageUrl", "https://yourcdn.com/images/ekadashi_puja.png",
                "type", "Puja",
                "buttonText", "Book Now"
            )
        );
        return ResponseEntity.ok(offers);
    }
}
