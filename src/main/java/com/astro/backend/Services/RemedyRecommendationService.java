package com.astro.backend.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RemedyRecommendationService {

    /**
     * Generate remedy recommendations based on detected doshas
     */
    public Map<String, Object> generateRemedies(Map<String, Boolean> doshas, Map<String, String> planetaryPositions) {
        try {
            Map<String, Object> remedies = new LinkedHashMap<>();
            
            // Process each dosha
            if (doshas.getOrDefault("Mangal Dosha", false)) {
                remedies.put("Mangal Dosha Remedies", getRemedialsForMangalDosha());
            }
            
            if (doshas.getOrDefault("Kaal Sarp Dosha", false)) {
                remedies.put("Kaal Sarp Dosha Remedies", getRemedialsForKaalSarpDosha());
            }
            
            if (doshas.getOrDefault("Pitru Dosha", false)) {
                remedies.put("Pitru Dosha Remedies", getRemedialsForPitruDosha());
            }
            
            if (doshas.getOrDefault("Grahan Dosha", false)) {
                remedies.put("Grahan Dosha Remedies", getRemedialsForGrahanDosha());
            }

            // Add general prosperity remedies
            remedies.put("General Prosperity", getGeneralRemedies());
            
            // Add planet-specific remedies
            remedies.put("Planet-Specific Strengthening", getPlanetRemedies(planetaryPositions));

            return remedies;

        } catch (Exception e) {
            log.error("Error generating remedies", e);
            throw new RuntimeException("Failed to generate remedies: " + e.getMessage());
        }
    }

    private Map<String, Object> getRemedialsForMangalDosha() {
        Map<String, Object> remedial = new LinkedHashMap<>();
        
        remedial.put("Gemstones", List.of(
                new RemedyItem("Red Coral (Moonga)", "3-6 carats", "Ring finger or thumb", "Energetic, Bold"),
                new RemedyItem("Red Tourmaline", "2-3 carats", "Ring finger", "Protective energy")
        ));
        
        remedial.put("Rudraksha", List.of(
                new RemedyItem("6 Mukhi Rudraksha", "1 bead", "Wear or meditate", "Mars energy balance"),
                new RemedyItem("12 Mukhi Rudraksha", "1 bead", "Wear or meditate", "Courage & strength")
        ));
        
        remedial.put("Mantras", List.of(
                new RemedyItem("Hanuman Chalisa", "Read daily or listen", "Morning/Evening", "Courage & protection"),
                new RemedyItem("Mars Mantra: Om Bhom Bhaumaya Namah", "108 times", "Tuesday mornings", "Mars propitiation"),
                new RemedyItem("Kartikeya Mantra", "108 times", "Any auspicious day", "Divine warrior energy")
        ));
        
        remedial.put("Fasting", List.of(
                new RemedyItem("Tuesday Fast", "Every Tuesday", "Complete or partial", "Mars appeasement"),
                new RemedyItem("Fast on Ekadashi", "Twice a month", "With devotion", "General spiritual growth")
        ));
        
        remedial.put("Donations", List.of(
                new RemedyItem("Red items", "Tuesday", "Clothes, flowers, food", "Mars satisfaction"),
                new RemedyItem("Gold", "To temples", "As per capacity", "Solar strength")
        ));
        
        remedial.put("Rituals", List.of(
                new RemedyItem("Hanuman Pooja", "Every Tuesday", "With devotion", "Mars mitigation"),
                new RemedyItem("Rudra Abhishek", "Annually", "Full ritual", "Karmic cleansing")
        ));
        
        return remedial;
    }

    private Map<String, Object> getRemedialsForKaalSarpDosha() {
        Map<String, Object> remedial = new LinkedHashMap<>();
        
        remedial.put("Gemstones", List.of(
                new RemedyItem("Gomed (Hessonite)", "3-6 carats", "Middle finger", "Rahu control"),
                new RemedyItem("Cat's Eye (Lehsunia)", "3-5 carats", "Little finger", "Ketu management")
        ));
        
        remedial.put("Rudraksha", List.of(
                new RemedyItem("8 Mukhi Rudraksha", "1 bead", "Wear daily", "Rahu balance"),
                new RemedyItem("7 Mukhi Rudraksha", "1 bead", "Wear daily", "Ketu balance")
        ));
        
        remedial.put("Mantras", List.of(
                new RemedyItem("Rahu Mantra: Om Bhram Bhreem Bhraum Sah Rahave Namah", "108 times", "Saturday", "Rahu appeasement"),
                new RemedyItem("Ketu Mantra: Om Kem Ketave Namah", "108 times", "Saturday", "Ketu management"),
                new RemedyItem("Nag Matasya Mantra", "40 days", "Morning", "Serpent appeasement")
        ));
        
        remedial.put("Fasting", List.of(
                new RemedyItem("Saturday Fast", "Every Saturday", "Partial or full", "Saturn/Rahu control")
        ));
        
        remedial.put("Rituals", List.of(
                new RemedyItem("Nag Puja", "Nag Panchami", "Full ritual", "Kaal Sarp mitigation"),
                new RemedyItem("Abhishek at Varanasi", "Once in lifetime", "Special ritual", "Spiritual cleansing")
        ));
        
        return remedial;
    }

    private Map<String, Object> getRemedialsForPitruDosha() {
        Map<String, Object> remedial = new LinkedHashMap<>();
        
        remedial.put("Gemstones", List.of(
                new RemedyItem("Yellow Sapphire", "3-6 carats", "Index finger", "Jupiter strengthening"),
                new RemedyItem("Ruby", "3-6 carats", "Ring finger", "Solar strengthening")
        ));
        
        remedial.put("Mantras", List.of(
                new RemedyItem("Pitru Gayatri Mantra", "108 times", "Daily", "Ancestral blessings"),
                new RemedyItem("Om Pitrubhyo Namah", "Recite 7 times", "Daily", "Respectful ancestors"),
                new RemedyItem("Surya Mantra", "108 times", "Sunrise", "Karmic cleansing")
        ));
        
        remedial.put("Rituals", List.of(
                new RemedyItem("Tarpan", "Monthly or yearly", "Water offering", "Ancestor appeasement"),
                new RemedyItem("Shradh Ceremony", "Yearly", "During Pitru Paksh", "Ancestral blessings"),
                new RemedyItem("Kanyadaan", "If applicable", "Donations to education", "Pitru satisfaction")
        ));
        
        remedial.put("Donations", List.of(
                new RemedyItem("Food items", "Daily if possible", "To poor/temples", "Karmic benefit"),
                new RemedyItem("Education support", "Yearly", "Help a poor child", "Ancestral wishes fulfillment")
        ));
        
        return remedial;
    }

    private Map<String, Object> getRemedialsForGrahanDosha() {
        Map<String, Object> remedial = new LinkedHashMap<>();
        
        remedial.put("Gemstones", List.of(
                new RemedyItem("Pearl", "2-5 carats", "Ring finger", "Moon strengthening"),
                new RemedyItem("Ruby", "3-6 carats", "Ring finger", "Sun strengthening")
        ));
        
        remedial.put("Mantras", List.of(
                new RemedyItem("Chandra Mantra", "108 times", "Monday evening", "Moon balance"),
                new RemedyItem("Aditya Mantra", "108 times", "Sunday morning", "Sun strengthening"),
                new RemedyItem("Rahu/Ketu Mantras", "As per dosha", "Regular recitation", "Eclipse neutralization")
        ));
        
        remedial.put("Rituals", List.of(
                new RemedyItem("Eclipse Fasting", "During eclipses", "Complete fast", "Karmic cleansing"),
                new RemedyItem("Chandrayaan", "Monthly", "On Purnima", "Moon gratitude"),
                new RemedyItem("Chandi Path", "Full ritual", "During difficult times", "Protective energy")
        ));
        
        return remedial;
    }

    private Map<String, Object> getGeneralRemedies() {
        Map<String, Object> remedial = new LinkedHashMap<>();
        
        remedial.put("Daily Practices", List.of(
                new RemedyItem("Meditation", "15-30 mins daily", "Morning is best", "Mental clarity & peace"),
                new RemedyItem("Yoga/Pranayama", "20-30 mins daily", "Any time", "Physical & mental health"),
                new RemedyItem("Reading Bhagavad Gita", "Daily chapters", "Any time", "Spiritual guidance"),
                new RemedyItem("Charity", "As per capacity", "Daily if possible", "Karmic improvement")
        ));
        
        remedial.put("Weekly Practices", List.of(
                new RemedyItem("Temple Visit", "Once weekly", "Any temple", "Spiritual connection"),
                new RemedyItem("Fasting", "One day/week", "Monday/Saturday", "Body cleansing"),
                new RemedyItem("Chanting Mantras", "1 hour", "Any day", "Vibrational uplift")
        ));
        
        remedial.put("Colors & Direction", List.of(
                new RemedyItem("Wear white/cream", "Daily", "Purity & peace", "Spiritual growth"),
                new RemedyItem("Sleep with head North", "Daily", "Energetic alignment", "Better health"),
                new RemedyItem("Keep water north of bed", "Daily", "Vastu benefit", "Prosperity")
        ));
        
        return remedial;
    }

    private Map<String, Object> getPlanetRemedies(Map<String, String> planetaryPositions) {
        Map<String, Object> planetRemedies = new LinkedHashMap<>();
        
        // Sun
        planetRemedies.put("Sun Strengthening", List.of(
                new RemedyItem("Gemstone: Ruby", "3-6 carats", "Ring finger", "Leadership & confidence"),
                new RemedyItem("Mantra: Om Suryaya Namah", "108 times Sunday", "Morning practice", "Solar energy"),
                new RemedyItem("Fast: Sundays", "Partial/full", "Weekly", "Sun appeasement"),
                new RemedyItem("Donate: Gold/Jaggery", "Sunday", "To temples/poor", "Solar activation")
        ));
        
        // Moon
        planetRemedies.put("Moon Strengthening", List.of(
                new RemedyItem("Gemstone: Pearl", "2-5 carats", "Ring/little finger", "Emotional balance"),
                new RemedyItem("Mantra: Om Chandaya Namah", "108 times Monday", "Evening practice", "Lunar energy"),
                new RemedyItem("Fast: Mondays", "Partial/full", "Weekly", "Moon appeasement"),
                new RemedyItem("Donate: White items", "Monday", "To temples/poor", "Lunar activation")
        ));
        
        // Mercury
        planetRemedies.put("Mercury Strengthening", List.of(
                new RemedyItem("Gemstone: Emerald", "2-5 carats", "Little/middle finger", "Communication"),
                new RemedyItem("Mantra: Om Budhaya Namah", "108 times Wednesday", "Morning", "Mercury energy"),
                new RemedyItem("Fast: Wednesdays", "Partial/full", "Weekly", "Mercury appeasement"),
                new RemedyItem("Donate: Green items", "Wednesday", "To temples/poor", "Mercury activation")
        ));
        
        // Jupiter
        planetRemedies.put("Jupiter Strengthening", List.of(
                new RemedyItem("Gemstone: Yellow Sapphire", "3-6 carats", "Index finger", "Wisdom & fortune"),
                new RemedyItem("Mantra: Om Gurave Namah", "108 times Thursday", "Morning", "Jupiter energy"),
                new RemedyItem("Fast: Thursdays", "Partial/full", "Weekly", "Jupiter appeasement"),
                new RemedyItem("Donate: Gold/Yellow items", "Thursday", "To temples/poor", "Jupiter activation")
        ));
        
        // Venus
        planetRemedies.put("Venus Strengthening", List.of(
                new RemedyItem("Gemstone: Diamond", "0.5-2 carats", "Ring finger", "Love & beauty"),
                new RemedyItem("Mantra: Om Shukraya Namah", "108 times Friday", "Evening", "Venus energy"),
                new RemedyItem("Fast: Fridays", "Partial/full", "Weekly", "Venus appeasement"),
                new RemedyItem("Donate: White/pink items", "Friday", "To temples/poor", "Venus activation")
        ));
        
        // Saturn
        planetRemedies.put("Saturn Strengthening", List.of(
                new RemedyItem("Gemstone: Blue Sapphire", "3-6 carats", "Middle finger", "Discipline & longevity"),
                new RemedyItem("Mantra: Om Shanaishcharaya Namah", "108 times Saturday", "Evening", "Saturn energy"),
                new RemedyItem("Fast: Saturdays", "Partial/full", "Weekly", "Saturn appeasement"),
                new RemedyItem("Donate: Black/blue items", "Saturday", "To temples/poor", "Saturn activation")
        ));
        
        return planetRemedies;
    }

    public static class RemedyItem {
        public String name;
        public String details;
        public String timing;
        public String benefit;

        public RemedyItem(String name, String details, String timing, String benefit) {
            this.name = name;
            this.details = details;
            this.timing = timing;
            this.benefit = benefit;
        }
    }
}
