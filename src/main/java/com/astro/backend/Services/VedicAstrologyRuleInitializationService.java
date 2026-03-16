package com.astro.backend.Services;

import com.astro.backend.Entity.VedicAstrologyRule;
import com.astro.backend.Repositry.VedicAstrologyRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class VedicAstrologyRuleInitializationService implements CommandLineRunner {

    private final VedicAstrologyRuleRepository vedicAstrologyRuleRepository;

    private static final List<AstroPoint> PLANETS = List.of(
            new AstroPoint("SU", "Sun", "सूर्य", "authority, vitality and fatherly karma", "अधिकार, जीवनशक्ति और पितृ कर्म", List.of(7)),
            new AstroPoint("MO", "Moon", "चंद्र", "mind, emotions and nourishment", "मन, भावनाएं और पोषण", List.of(7)),
            new AstroPoint("MA", "Mars", "मंगल", "courage, force and technical drive", "साहस, शक्ति और तकनीकी प्रवृत्ति", List.of(4, 7, 8)),
            new AstroPoint("ME", "Mercury", "बुध", "intellect, speech and adaptability", "बुद्धि, वाणी और अनुकूलन", List.of(7)),
            new AstroPoint("JU", "Jupiter", "गुरु", "wisdom, grace and expansion", "ज्ञान, कृपा और विस्तार", List.of(5, 7, 9)),
            new AstroPoint("VE", "Venus", "शुक्र", "relationships, pleasure and refinement", "संबंध, सुख और परिष्कार", List.of(7)),
            new AstroPoint("SA", "Saturn", "शनि", "discipline, duty and karmic delay", "अनुशासन, कर्तव्य और कर्मजन्य विलंब", List.of(3, 7, 10)),
            new AstroPoint("RA", "Rahu", "राहु", "ambition, obsession and unconventional growth", "महत्वाकांक्षा, आसक्ति और असामान्य वृद्धि", List.of(5, 7, 9)),
            new AstroPoint("KE", "Ketu", "केतु", "detachment, insight and spiritual severance", "विरक्ति, अंतर्दृष्टि और आध्यात्मिक वैराग्य", List.of(5, 7, 9))
    );

    private static final List<HouseInfo> HOUSES = List.of(
            new HouseInfo(1, "1st House", "प्रथम भाव", "self, body and life direction", "स्वभाव, शरीर और जीवन की दिशा"),
            new HouseInfo(2, "2nd House", "द्वितीय भाव", "wealth, speech and family values", "धन, वाणी और परिवार"),
            new HouseInfo(3, "3rd House", "तृतीय भाव", "effort, siblings and courage", "प्रयास, भाई-बहन और पराक्रम"),
            new HouseInfo(4, "4th House", "चतुर्थ भाव", "home, mother, property and inner comfort", "माता, गृह, संपत्ति और मानसिक शांति"),
            new HouseInfo(5, "5th House", "पंचम भाव", "intelligence, children, mantra and creativity", "बुद्धि, संतान, मंत्र और रचनात्मकता"),
            new HouseInfo(6, "6th House", "षष्ठ भाव", "debts, disease, service and enemies", "ऋण, रोग, सेवा और शत्रु"),
            new HouseInfo(7, "7th House", "सप्तम भाव", "marriage, agreements and partnerships", "विवाह, समझौते और साझेदारी"),
            new HouseInfo(8, "8th House", "अष्टम भाव", "transformation, secrecy, longevity and occult matters", "परिवर्तन, रहस्य, आयु और गूढ़ विषय"),
            new HouseInfo(9, "9th House", "नवम भाव", "dharma, luck, teachers and higher wisdom", "धर्म, भाग्य, गुरु और उच्च ज्ञान"),
            new HouseInfo(10, "10th House", "दशम भाव", "career, karma, status and authority", "कर्म, करियर, प्रतिष्ठा और अधिकार"),
            new HouseInfo(11, "11th House", "एकादश भाव", "gains, networks and fulfilment of desires", "लाभ, नेटवर्क और इच्छापूर्ति"),
            new HouseInfo(12, "12th House", "द्वादश भाव", "loss, retreat, foreign lands and moksha", "व्यय, एकांत, विदेश और मोक्ष")
    );

    private static final List<SignInfo> SIGNS = List.of(
            new SignInfo("AR", "Aries", "मेष", "initiative, courage and fiery movement", "पहल, साहस और अग्नि ऊर्जा"),
            new SignInfo("TA", "Taurus", "वृषभ", "stability, resources and grounded growth", "स्थिरता, संसाधन और स्थिर विकास"),
            new SignInfo("GE", "Gemini", "मिथुन", "communication, curiosity and versatility", "संवाद, जिज्ञासा और बहुमुखी स्वभाव"),
            new SignInfo("CA", "Cancer", "कर्क", "care, feeling and protective instincts", "पोषण, भावना और सुरक्षा"),
            new SignInfo("LE", "Leo", "सिंह", "radiance, authority and creative self-expression", "तेज, अधिकार और रचनात्मक अभिव्यक्ति"),
            new SignInfo("VI", "Virgo", "कन्या", "analysis, order and service orientation", "विश्लेषण, व्यवस्था और सेवा भाव"),
            new SignInfo("LI", "Libra", "तुला", "balance, negotiation and relationships", "संतुलन, सामंजस्य और संबंध"),
            new SignInfo("SC", "Scorpio", "वृश्चिक", "intensity, secrecy and deep transformation", "तीव्रता, रहस्य और गहरा परिवर्तन"),
            new SignInfo("SG", "Sagittarius", "धनु", "faith, guidance and philosophical expansion", "श्रद्धा, मार्गदर्शन और दार्शनिक विस्तार"),
            new SignInfo("CP", "Capricorn", "मकर", "discipline, duty and structured progress", "अनुशासन, कर्तव्य और संरचित प्रगति"),
            new SignInfo("AQ", "Aquarius", "कुंभ", "systems, ideals and collective thinking", "प्रणाली, आदर्श और सामूहिक सोच"),
            new SignInfo("PI", "Pisces", "मीन", "compassion, surrender and imagination", "करुणा, समर्पण और कल्पना")
    );

    private static final List<NakshatraInfo> NAKSHATRAS = List.of(
            new NakshatraInfo("ASW", "Ashwini", "अश्विनी", "quick beginnings, healing impulses and movement", "तेज़ शुरुआत, उपचार की प्रवृत्ति और गति"),
            new NakshatraInfo("BHA", "Bharani", "भरणी", "discipline, restraint and karmic responsibility", "अनुशासन, संयम और कर्मिक जिम्मेदारी"),
            new NakshatraInfo("KRI", "Krittika", "कृत्तिका", "purification, sharpness and decisive action", "शुद्धि, तीक्ष्णता और निर्णायक कार्य"),
            new NakshatraInfo("ROH", "Rohini", "रोहिणी", "growth, fertility and graceful attraction", "वृद्धि, उर्वरता और आकर्षण"),
            new NakshatraInfo("MRI", "Mrigashira", "मृगशीर्ष", "search, curiosity and soft exploration", "खोज, जिज्ञासा और कोमल अन्वेषण"),
            new NakshatraInfo("ARD", "Ardra", "आर्द्रा", "storm, release and emotional catharsis", "आंधी, मुक्तिकरण और भावनात्मक शुद्धि"),
            new NakshatraInfo("PUN", "Punarvasu", "पुनर्वसु", "renewal, recovery and return to balance", "नवीनीकरण, पुनर्प्राप्ति और संतुलन"),
            new NakshatraInfo("PUS", "Pushya", "पुष्य", "nourishment, dharma and protective support", "पोषण, धर्म और संरक्षण"),
            new NakshatraInfo("ASH", "Ashlesha", "आश्लेषा", "binding, strategy and hidden currents", "बंधन, रणनीति और छिपे प्रवाह"),
            new NakshatraInfo("MAG", "Magha", "मघा", "lineage, authority and ancestral pride", "वंश, अधिकार और पितृ परंपरा"),
            new NakshatraInfo("PPH", "Purva Phalguni", "पूर्व फाल्गुनी", "pleasure, relaxation and creative display", "आनंद, विश्राम और रचनात्मक प्रदर्शन"),
            new NakshatraInfo("UPH", "Uttara Phalguni", "उत्तर फाल्गुनी", "agreements, support and sustainable alliances", "समझौते, सहयोग और स्थायी संबंध"),
            new NakshatraInfo("HAS", "Hasta", "हस्त", "skill, dexterity and practical control", "कौशल, निपुणता और व्यवहारिक नियंत्रण"),
            new NakshatraInfo("CHI", "Chitra", "चित्रा", "design, charisma and visible craftsmanship", "रचना, आकर्षण और कौशल प्रदर्शन"),
            new NakshatraInfo("SWA", "Swati", "स्वाती", "independence, wind-like movement and trade", "स्वतंत्रता, वायु जैसा प्रवाह और व्यापार"),
            new NakshatraInfo("VIS", "Vishakha", "विशाखा", "focus, ambition and milestone pursuit", "एकाग्रता, महत्वाकांक्षा और लक्ष्य साधना"),
            new NakshatraInfo("ANU", "Anuradha", "अनुराधा", "friendship, devotion and organised growth", "मित्रता, भक्ति और संगठित वृद्धि"),
            new NakshatraInfo("JYE", "Jyeshtha", "ज्येष्ठा", "seniority, protection and intense responsibility", "वरिष्ठता, संरक्षण और तीव्र जिम्मेदारी"),
            new NakshatraInfo("MUL", "Mula", "मूल", "roots, destruction and truth-seeking", "मूल, विघटन और सत्य की खोज"),
            new NakshatraInfo("PSH", "Purva Ashadha", "पूर्वाषाढ़ा", "assertion, inspiration and emotional victory", "दृढ़ता, प्रेरणा और भावनात्मक विजय"),
            new NakshatraInfo("USH", "Uttara Ashadha", "उत्तराषाढ़ा", "lasting success, duty and noble resolve", "स्थायी सफलता, कर्तव्य और उच्च संकल्प"),
            new NakshatraInfo("SHR", "Shravana", "श्रवण", "listening, learning and transmission of wisdom", "श्रवण, सीखना और ज्ञान का संचार"),
            new NakshatraInfo("DHA", "Dhanishta", "धनिष्ठा", "rhythm, wealth and coordinated action", "लय, धन और समन्वित कर्म"),
            new NakshatraInfo("SAT", "Shatabhisha", "शतभिषा", "healing, isolation and hidden diagnosis", "चिकित्सा, एकांत और गुप्त निदान"),
            new NakshatraInfo("PBH", "Purva Bhadrapada", "पूर्व भाद्रपद", "intensity, tapas and radical insight", "तीव्रता, तप और गहरी अंतर्दृष्टि"),
            new NakshatraInfo("UBH", "Uttara Bhadrapada", "उत्तर भाद्रपद", "stability, depth and wise restraint", "स्थिरता, गहराई और संयम"),
            new NakshatraInfo("REV", "Revati", "रेवती", "completion, guidance and compassionate closure", "पूर्णता, मार्गदर्शन और करुणामय समापन")
    );

    private static final List<StrengthState> STRENGTH_STATES = List.of(
            new StrengthState("EX", "Exalted", "उच्च", "shows refined confidence and gives its agenda with greater clarity", "अधिक स्पष्टता और परिष्कृत शक्ति के साथ फल देता है"),
            new StrengthState("DE", "Debilitated", "नीच", "struggles to express its nature cleanly and may create compensatory lessons", "अपने स्वभाव को सहज रूप में नहीं दे पाता और सुधारात्मक पाठ देता है"),
            new StrengthState("OW", "Own Sign", "स्वराशि", "acts with natural ownership, steadiness and self-directed results", "स्वामित्व, स्थिरता और स्वाभाविक परिणाम देता है"),
            new StrengthState("FR", "Friendly Sign", "मित्र राशि", "finds cooperation and expresses its agenda with support", "सहयोग पाकर अपना कार्य अपेक्षाकृत सहजता से करता है"),
            new StrengthState("NE", "Neutral Sign", "सम राशि", "works in a moderate way and depends more on aspects and lordship", "मध्यम रूप से कार्य करता है और दृष्टि व भावेश पर अधिक निर्भर रहता है"),
            new StrengthState("EN", "Enemy Sign", "शत्रु राशि", "feels resistance and may delay or complicate its natural agenda", "विरोध अनुभव करता है और अपने स्वाभाविक फल में देरी या जटिलता ला सकता है")
    );

    @Override
    public void run(String... args) {
        initializeRules();
    }

    private void initializeRules() {
        try {
            List<VedicAstrologyRule> existingRules = vedicAstrologyRuleRepository.findAll();
            Map<String, VedicAstrologyRule> existingByCode = new HashMap<>();
            for (VedicAstrologyRule existingRule : existingRules) {
                existingByCode.put(existingRule.getRuleCode(), existingRule);
            }

            List<VedicAstrologyRule> canonicalRules = generateRules();
            List<VedicAstrologyRule> rulesToSave = new ArrayList<>();
            int insertedCount = 0;
            int updatedCount = 0;

            for (VedicAstrologyRule canonicalRule : canonicalRules) {
                VedicAstrologyRule existingRule = existingByCode.get(canonicalRule.getRuleCode());
                if (existingRule == null) {
                    rulesToSave.add(canonicalRule);
                    insertedCount++;
                    continue;
                }

                if (isDifferent(existingRule, canonicalRule)) {
                    canonicalRule.setId(existingRule.getId());
                    canonicalRule.setCreatedAt(existingRule.getCreatedAt());
                    canonicalRule.setUpdatedAt(existingRule.getUpdatedAt());
                    canonicalRule.setIsActive(existingRule.getIsActive() == null ? true : existingRule.getIsActive());
                    rulesToSave.add(canonicalRule);
                    updatedCount++;
                }
            }

            if (rulesToSave.isEmpty()) {
                log.info("Vedic astrology rules already in sync. Total count: {}", existingRules.size());
                return;
            }

            vedicAstrologyRuleRepository.saveAll(rulesToSave);
            log.info(
                    "Vedic astrology rules initialization completed. Inserted {}, updated {}. Total canonical rules: {}.",
                    insertedCount,
                    updatedCount,
                    canonicalRules.size()
            );
        } catch (Exception ex) {
            log.error("Error initializing Vedic astrology rules: {}", ex.getMessage(), ex);
        }
    }

    private List<VedicAstrologyRule> generateRules() {
        List<VedicAstrologyRule> rules = new ArrayList<>();
        Set<String> generatedCodes = new HashSet<>();

        generatePlanetInHouseRules(rules, generatedCodes);
        generatePlanetInSignRules(rules, generatedCodes);
        generateHouseLordInHouseRules(rules, generatedCodes);
        generateHouseLordInSignRules(rules, generatedCodes);
        generatePlanetAspectHouseRules(rules, generatedCodes);
        generatePlanetAspectPlanetRules(rules, generatedCodes);
        generateConjunctionRules(rules, generatedCodes);
        generatePlanetInNakshatraRules(rules, generatedCodes);
        generateDashaRules(rules, generatedCodes);
        generateDashaHouseActivationRules(rules, generatedCodes);
        generateTransitFromAscendantRules(rules, generatedCodes);
        generateTransitFromMoonRules(rules, generatedCodes);
        generateStrengthInHouseRules(rules, generatedCodes);

        return rules;
    }

    private void generatePlanetInHouseRules(List<VedicAstrologyRule> rules, Set<String> existingCodes) {
        for (AstroPoint planet : PLANETS) {
            for (HouseInfo house : HOUSES) {
                String code = "PIH-" + planet.code + "-H" + house.number;
                addRule(
                        rules,
                        existingCodes,
                        code,
                        "Planetary Placement",
                        "Planet in House",
                        planet.nameEn + " in " + house.nameEn,
                        house.nameHi + " में " + planet.nameHi,
                        "When " + planet.nameEn + " occupies the " + house.nameEn.toLowerCase()
                                + ", Vedic interpretation connects " + planet.themeEn + " with " + house.themeEn
                                + ". Strong dignity, clean lordship and benefic support improve outcomes, while affliction can create stress in the same life areas.",
                        "जब " + planet.nameHi + " " + house.nameHi + " में स्थित होता है, वैदिक व्याख्या "
                                + planet.themeHi + " को " + house.themeHi + " से जोड़ती है। ग्रह की शक्ति, शुभ प्रभाव और भावेश का समर्थन अच्छे परिणाम बढ़ाते हैं, जबकि पीड़ा होने पर इन्हीं क्षेत्रों में तनाव दिख सकता है।",
                        "type=planet_in_house;planet=" + planet.nameEn + ";house=" + house.number
                );
            }
        }
    }

    private void generatePlanetInSignRules(List<VedicAstrologyRule> rules, Set<String> existingCodes) {
        for (AstroPoint planet : PLANETS) {
            for (SignInfo sign : SIGNS) {
                String code = "PIS-" + planet.code + "-" + sign.code;
                addRule(
                        rules,
                        existingCodes,
                        code,
                        "Planetary Placement",
                        "Planet in Sign",
                        planet.nameEn + " in " + sign.nameEn,
                        sign.nameHi + " में " + planet.nameHi,
                        "When " + planet.nameEn + " moves through " + sign.nameEn + ", its themes of "
                                + planet.themeEn + " are expressed through " + sign.natureEn
                                + ". The final result depends on dignity, aspects and house placement, but the sign colors how the planet delivers its agenda.",
                        "जब " + planet.nameHi + " " + sign.nameHi + " में होता है, तब " + planet.themeHi
                                + " के फल " + sign.natureHi + " के माध्यम से व्यक्त होते हैं। अंतिम परिणाम ग्रह बल, दृष्टि और भाव स्थिति पर निर्भर रहते हैं, पर राशि ग्रह की अभिव्यक्ति का स्वर तय करती है।",
                        "type=planet_in_sign;planet=" + planet.nameEn + ";sign=" + sign.nameEn
                );
            }
        }
    }

    private void generateHouseLordInHouseRules(List<VedicAstrologyRule> rules, Set<String> existingCodes) {
        for (HouseInfo source : HOUSES) {
            for (HouseInfo target : HOUSES) {
                String code = "HLH-H" + source.number + "-H" + target.number;
                addRule(
                        rules,
                        existingCodes,
                        code,
                        "House Lordship",
                        "House Lord in House",
                        source.nameEn + " Lord in " + target.nameEn,
                        source.nameHi + " का स्वामी " + target.nameHi + " में",
                        "For the relevant ascendant, when the lord of the " + source.nameEn.toLowerCase()
                                + " occupies the " + target.nameEn.toLowerCase()
                                + ", matters of " + source.themeEn + " seek expression through " + target.themeEn
                                + ". This rule becomes stronger when the lord is dignified and free from heavy affliction.",
                        "संबंधित लग्न के लिए जब " + source.nameHi + " का स्वामी " + target.nameHi + " में जाता है, तब "
                                + source.themeHi + " के विषय " + target.themeHi + " के माध्यम से फल देते हैं। यह नियम तब अधिक प्रभावी होता है जब भावेश शक्तिशाली हो और अधिक पीड़ित न हो।",
                        "type=house_lord_in_house;sourceHouse=" + source.number + ";targetHouse=" + target.number
                );
            }
        }
    }

    private void generateHouseLordInSignRules(List<VedicAstrologyRule> rules, Set<String> existingCodes) {
        for (HouseInfo source : HOUSES) {
            for (SignInfo sign : SIGNS) {
                String code = "HLS-H" + source.number + "-" + sign.code;
                addRule(
                        rules,
                        existingCodes,
                        code,
                        "House Lordship",
                        "House Lord in Sign",
                        source.nameEn + " Lord in " + sign.nameEn,
                        source.nameHi + " का स्वामी " + sign.nameHi + " में",
                        "For the relevant ascendant, when the lord of the " + source.nameEn.toLowerCase()
                                + " occupies " + sign.nameEn + ", the affairs of " + source.themeEn
                                + " are filtered through " + sign.natureEn + ". The sign modifies the style, speed and emotional tone of the house lord's results.",
                        "संबंधित लग्न के लिए जब " + source.nameHi + " का स्वामी " + sign.nameHi + " में होता है, तब "
                                + source.themeHi + " के विषय " + sign.natureHi + " के माध्यम से फलित होते हैं। राशि भावेश के फल की शैली, गति और भावनात्मक रंग तय करती है।",
                        "type=house_lord_in_sign;sourceHouse=" + source.number + ";sign=" + sign.nameEn
                );
            }
        }
    }

    private void generatePlanetAspectHouseRules(List<VedicAstrologyRule> rules, Set<String> existingCodes) {
        for (AstroPoint planet : PLANETS) {
            for (HouseInfo sourceHouse : HOUSES) {
                for (Integer aspectOffset : planet.aspectOffsets) {
                    HouseInfo targetHouse = houseByNumber(wrapHouse(sourceHouse.number, aspectOffset));
                    String code = "PAH-" + planet.code + "-H" + sourceHouse.number + "-A" + aspectOffset;
                    addRule(
                            rules,
                            existingCodes,
                            code,
                            "Planetary Aspect",
                            "Planet Aspect on House",
                            planet.nameEn + " " + aspectLabelEn(aspectOffset) + " on " + targetHouse.nameEn + " from " + sourceHouse.nameEn,
                            planet.nameHi + " " + sourceHouse.nameHi + " से " + targetHouse.nameHi + " पर " + aspectLabelHi(aspectOffset) + " डालता है",
                            "Placed in the " + sourceHouse.nameEn.toLowerCase() + ", " + planet.nameEn + " casts its "
                                    + aspectLabelEn(aspectOffset).toLowerCase() + " onto the " + targetHouse.nameEn.toLowerCase()
                                    + ". This channels " + planet.themeEn + " from " + sourceHouse.themeEn
                                    + " into the affairs of " + targetHouse.themeEn + ".",
                            sourceHouse.nameHi + " में स्थित " + planet.nameHi + " अपनी " + aspectLabelHi(aspectOffset)
                                    + " द्वारा " + targetHouse.nameHi + " को प्रभावित करता है। इससे " + planet.themeHi
                                    + " की ऊर्जा " + sourceHouse.themeHi + " से उठकर " + targetHouse.themeHi + " में सक्रिय होती है।",
                            "type=planet_aspect_house;planet=" + planet.nameEn + ";fromHouse=" + sourceHouse.number + ";toHouse=" + targetHouse.number
                    );
                }
            }
        }
    }

    private void generatePlanetAspectPlanetRules(List<VedicAstrologyRule> rules, Set<String> existingCodes) {
        for (AstroPoint sourcePlanet : PLANETS) {
            for (HouseInfo sourceHouse : HOUSES) {
                for (Integer aspectOffset : sourcePlanet.aspectOffsets) {
                    HouseInfo targetHouse = houseByNumber(wrapHouse(sourceHouse.number, aspectOffset));
                    for (AstroPoint targetPlanet : PLANETS) {
                        if (sourcePlanet.code.equals(targetPlanet.code)) {
                            continue;
                        }
                        String code = "PAP-" + sourcePlanet.code + "-" + targetPlanet.code + "-H" + sourceHouse.number + "-A" + aspectOffset;
                        addRule(
                                rules,
                                existingCodes,
                                code,
                                "Planetary Aspect",
                                "Planet Aspect on Planet",
                                sourcePlanet.nameEn + " aspects " + targetPlanet.nameEn + " from " + sourceHouse.nameEn + " to " + targetHouse.nameEn,
                                sourcePlanet.nameHi + " " + sourceHouse.nameHi + " से " + targetHouse.nameHi + " में स्थित " + targetPlanet.nameHi + " को देखता है",
                                "When " + sourcePlanet.nameEn + " in the " + sourceHouse.nameEn.toLowerCase()
                                        + " aspects " + targetPlanet.nameEn + " in the " + targetHouse.nameEn.toLowerCase()
                                        + ", the themes of " + sourcePlanet.themeEn + " modify, energise or challenge the significations of "
                                        + targetPlanet.themeEn + ". The final result depends on strength, dignity and house ownership.",
                                "जब " + sourcePlanet.nameHi + " " + sourceHouse.nameHi + " से " + targetHouse.nameHi
                                        + " में स्थित " + targetPlanet.nameHi + " को देखता है, तब " + sourcePlanet.themeHi
                                        + " की ऊर्जा " + targetPlanet.themeHi + " के फल को बदलती, प्रेरित करती या चुनौती देती है। अंतिम परिणाम शक्ति, गरिमा और स्वामित्व पर निर्भर करता है।",
                                "type=planet_aspect_planet;sourcePlanet=" + sourcePlanet.nameEn + ";targetPlanet=" + targetPlanet.nameEn
                        );
                    }
                }
            }
        }
    }

    private void generateConjunctionRules(List<VedicAstrologyRule> rules, Set<String> existingCodes) {
        for (int i = 0; i < PLANETS.size(); i++) {
            for (int j = i + 1; j < PLANETS.size(); j++) {
                AstroPoint first = PLANETS.get(i);
                AstroPoint second = PLANETS.get(j);
                for (HouseInfo house : HOUSES) {
                    String code = "CONJ-" + first.code + "-" + second.code + "-H" + house.number;
                    addRule(
                            rules,
                            existingCodes,
                            code,
                            "Planetary Combination",
                            "Conjunction in House",
                            first.nameEn + "-" + second.nameEn + " conjunction in " + house.nameEn,
                            house.nameHi + " में " + first.nameHi + "-" + second.nameHi + " युति",
                            "A conjunction of " + first.nameEn + " and " + second.nameEn + " in the " + house.nameEn.toLowerCase()
                                    + " blends " + first.themeEn + " with " + second.themeEn + " through the house matters of "
                                    + house.themeEn + ". Harmony or stress depends on dignity, combustion, closeness and supporting aspects.",
                            house.nameHi + " में " + first.nameHi + " और " + second.nameHi + " की युति "
                                    + first.themeHi + " तथा " + second.themeHi + " को " + house.themeHi
                                    + " के माध्यम से मिलाती है। शुभता या तनाव ग्रह बल, युति की निकटता, दग्धता और दृष्टि पर निर्भर करेगा।",
                            "type=conjunction;planetOne=" + first.nameEn + ";planetTwo=" + second.nameEn + ";house=" + house.number
                    );
                }
            }
        }
    }

    private void generatePlanetInNakshatraRules(List<VedicAstrologyRule> rules, Set<String> existingCodes) {
        for (AstroPoint planet : PLANETS) {
            for (NakshatraInfo nakshatra : NAKSHATRAS) {
                String code = "PIN-" + planet.code + "-" + nakshatra.code;
                addRule(
                        rules,
                        existingCodes,
                        code,
                        "Nakshatra Rule",
                        "Planet in Nakshatra",
                        planet.nameEn + " in " + nakshatra.nameEn,
                        nakshatra.nameHi + " में " + planet.nameHi,
                        "When " + planet.nameEn + " occupies " + nakshatra.nameEn + ", its agenda of "
                                + planet.themeEn + " is filtered through themes of " + nakshatra.natureEn
                                + ". Nakshatra placement refines behavioural style, timing sensitivity and subtle motivation behind the planet.",
                        "जब " + planet.nameHi + " " + nakshatra.nameHi + " में स्थित होता है, तब " + planet.themeHi
                                + " के फल " + nakshatra.natureHi + " के माध्यम से व्यक्त होते हैं। नक्षत्र स्थिति ग्रह की सूक्ष्म प्रेरणा, व्यवहार शैली और परिणाम की बारीकी को तय करती है।",
                        "type=planet_in_nakshatra;planet=" + planet.nameEn + ";nakshatra=" + nakshatra.nameEn
                );
            }
        }
    }

    private void generateDashaRules(List<VedicAstrologyRule> rules, Set<String> existingCodes) {
        for (AstroPoint maha : PLANETS) {
            for (AstroPoint antara : PLANETS) {
                String code = "DASHA-" + maha.code + "-" + antara.code;
                addRule(
                        rules,
                        existingCodes,
                        code,
                        "Dasha Rule",
                        "Mahadasha and Antardasha",
                        maha.nameEn + " Mahadasha with " + antara.nameEn + " Antardasha",
                        maha.nameHi + " महादशा में " + antara.nameHi + " अंतरदशा",
                        "During " + maha.nameEn + " Mahadasha with " + antara.nameEn + " Antardasha, long-cycle themes of "
                                + maha.themeEn + " operate through the sub-period style of " + antara.themeEn
                                + ". Results strengthen when both planets are dignified and connected to supportive houses.",
                        maha.nameHi + " महादशा में " + antara.nameHi + " अंतरदशा के दौरान "
                                + maha.themeHi + " के दीर्घकालिक विषय " + antara.themeHi
                                + " की उप-अवधि शैली से फलित होते हैं। जब दोनों ग्रह बलवान और शुभ भावों से जुड़े हों तो परिणाम अधिक सशक्त होते हैं।",
                        "type=dasha;mahadasha=" + maha.nameEn + ";antardasha=" + antara.nameEn
                );
            }
        }
    }

    private void generateDashaHouseActivationRules(List<VedicAstrologyRule> rules, Set<String> existingCodes) {
        for (AstroPoint maha : PLANETS) {
            for (AstroPoint antara : PLANETS) {
                for (HouseInfo house : HOUSES) {
                    String code = "DHH-" + maha.code + "-" + antara.code + "-H" + house.number;
                    addRule(
                            rules,
                            existingCodes,
                            code,
                            "Dasha Rule",
                            "Dasha with House Activation",
                            maha.nameEn + " Mahadasha / " + antara.nameEn + " Antardasha activating " + house.nameEn,
                            maha.nameHi + " महादशा / " + antara.nameHi + " अंतरदशा में " + house.nameHi + " सक्रिय",
                            "If the " + house.nameEn.toLowerCase() + " is prominently activated during "
                                    + maha.nameEn + " Mahadasha and " + antara.nameEn + " Antardasha, themes of "
                                    + house.themeEn + " come to the forefront while the planets deliver their shared karmic agenda.",
                            "यदि " + maha.nameHi + " महादशा और " + antara.nameHi + " अंतरदशा के दौरान "
                                    + house.nameHi + " प्रमुख रूप से सक्रिय हो, तो " + house.themeHi
                                    + " के विषय आगे आते हैं और दोनों ग्रह अपने संयुक्त कर्मफल देते हैं।",
                            "type=dasha_house_activation;mahadasha=" + maha.nameEn + ";antardasha=" + antara.nameEn + ";house=" + house.number
                    );
                }
            }
        }
    }

    private void generateTransitFromAscendantRules(List<VedicAstrologyRule> rules, Set<String> existingCodes) {
        for (AstroPoint planet : PLANETS) {
            for (HouseInfo house : HOUSES) {
                String code = "TFA-" + planet.code + "-H" + house.number;
                addRule(
                        rules,
                        existingCodes,
                        code,
                        "Transit Rule",
                        "Transit from Ascendant",
                        "Transit of " + planet.nameEn + " through " + house.nameEn + " from Ascendant",
                        "लग्न से " + house.nameHi + " में " + planet.nameHi + " का गोचर",
                        "When " + planet.nameEn + " transits the " + house.nameEn.toLowerCase() + " from the ascendant, it activates "
                                + house.themeEn + " through themes of " + planet.themeEn + ". Transit results are temporary and should always be read with natal strength and running dasha.",
                        "जब लग्न से " + house.nameHi + " में " + planet.nameHi + " का गोचर होता है, तब "
                                + house.themeHi + " के विषय " + planet.themeHi + " के माध्यम से सक्रिय होते हैं। गोचर के फल अस्थायी होते हैं और उन्हें जन्मकुंडली तथा चल रही दशा के साथ पढ़ना चाहिए।",
                        "type=transit_from_ascendant;planet=" + planet.nameEn + ";house=" + house.number
                );
            }
        }
    }

    private void generateTransitFromMoonRules(List<VedicAstrologyRule> rules, Set<String> existingCodes) {
        for (AstroPoint planet : PLANETS) {
            for (HouseInfo house : HOUSES) {
                String code = "TFM-" + planet.code + "-H" + house.number;
                addRule(
                        rules,
                        existingCodes,
                        code,
                        "Transit Rule",
                        "Transit from Moon",
                        "Transit of " + planet.nameEn + " through " + house.nameEn + " from Moon",
                        "चंद्र से " + house.nameHi + " में " + planet.nameHi + " का गोचर",
                        "When assessed from the natal Moon, a transit of " + planet.nameEn + " through the "
                                + house.nameEn.toLowerCase() + " influences emotional processing, mental comfort and lived experience of "
                                + house.themeEn + ".",
                        "जन्म चंद्र से देखा जाए तो " + house.nameHi + " में " + planet.nameHi + " का गोचर "
                                + house.themeHi + " के अनुभव, मनोभाव और भावनात्मक प्रतिक्रिया को प्रभावित करता है।",
                        "type=transit_from_moon;planet=" + planet.nameEn + ";house=" + house.number
                );
            }
        }
    }

    private void generateStrengthInHouseRules(List<VedicAstrologyRule> rules, Set<String> existingCodes) {
        for (AstroPoint planet : PLANETS) {
            for (StrengthState state : STRENGTH_STATES) {
                for (HouseInfo house : HOUSES) {
                    String code = "PSH-" + planet.code + "-" + state.code + "-H" + house.number;
                    addRule(
                            rules,
                            existingCodes,
                            code,
                            "Planetary Strength",
                            "Strength State in House",
                            state.nameEn + " " + planet.nameEn + " in " + house.nameEn,
                            house.nameHi + " में " + state.nameHi + " " + planet.nameHi,
                            "A " + state.nameEn.toLowerCase() + " " + planet.nameEn + " in the " + house.nameEn.toLowerCase()
                                    + " " + state.effectEn + ". It therefore modifies " + house.themeEn
                                    + " through the planet's significations of " + planet.themeEn + ".",
                            house.nameHi + " में " + state.nameHi + " " + planet.nameHi + " "
                                    + state.effectHi + "। इसलिए " + planet.themeHi + " की ऊर्जा " + house.themeHi
                                    + " को उसी अनुरूप प्रभावित करती है।",
                            "type=planet_strength_in_house;planet=" + planet.nameEn + ";state=" + state.nameEn + ";house=" + house.number
                    );
                }
            }
        }
    }

    private void addRule(
            List<VedicAstrologyRule> rules,
            Set<String> generatedCodes,
            String ruleCode,
            String category,
            String subcategory,
            String title,
            String titleHindi,
            String descriptionEn,
            String descriptionHi,
            String tags
    ) {
        if (!generatedCodes.add(ruleCode)) {
            return;
        }
        rules.add(
                VedicAstrologyRule.builder()
                        .ruleCode(ruleCode)
                        .category(category)
                        .subcategory(subcategory)
                        .title(title)
                        .titleHindi(titleHindi)
                        .descriptionEn(descriptionEn)
                        .descriptionHi(descriptionHi)
                        .tags(tags)
                        .isActive(true)
                        .build()
        );
    }

    private boolean isDifferent(VedicAstrologyRule existingRule, VedicAstrologyRule canonicalRule) {
        return !Objects.equals(existingRule.getCategory(), canonicalRule.getCategory())
                || !Objects.equals(existingRule.getSubcategory(), canonicalRule.getSubcategory())
                || !Objects.equals(existingRule.getTitle(), canonicalRule.getTitle())
                || !Objects.equals(existingRule.getTitleHindi(), canonicalRule.getTitleHindi())
                || !Objects.equals(existingRule.getDescriptionEn(), canonicalRule.getDescriptionEn())
                || !Objects.equals(existingRule.getDescriptionHi(), canonicalRule.getDescriptionHi())
                || !Objects.equals(existingRule.getTags(), canonicalRule.getTags())
                || !Objects.equals(existingRule.getIsActive(), canonicalRule.getIsActive());
    }

    private HouseInfo houseByNumber(int houseNumber) {
        return HOUSES.get(houseNumber - 1);
    }

    private int wrapHouse(int startHouse, int aspectOffset) {
        return ((startHouse + aspectOffset - 2) % 12) + 1;
    }

    private String aspectLabelEn(int aspectOffset) {
        return switch (aspectOffset) {
            case 3 -> "3rd aspect";
            case 4 -> "4th aspect";
            case 5 -> "5th aspect";
            case 7 -> "7th aspect";
            case 8 -> "8th aspect";
            case 9 -> "9th aspect";
            case 10 -> "10th aspect";
            default -> aspectOffset + "th aspect";
        };
    }

    private String aspectLabelHi(int aspectOffset) {
        return switch (aspectOffset) {
            case 3 -> "तृतीय दृष्टि";
            case 4 -> "चतुर्थ दृष्टि";
            case 5 -> "पंचम दृष्टि";
            case 7 -> "सप्तम दृष्टि";
            case 8 -> "अष्टम दृष्टि";
            case 9 -> "नवम दृष्टि";
            case 10 -> "दशम दृष्टि";
            default -> aspectOffset + "वीं दृष्टि";
        };
    }

    private static final class AstroPoint {
        private final String code;
        private final String nameEn;
        private final String nameHi;
        private final String themeEn;
        private final String themeHi;
        private final List<Integer> aspectOffsets;

        private AstroPoint(String code, String nameEn, String nameHi, String themeEn, String themeHi, List<Integer> aspectOffsets) {
            this.code = code;
            this.nameEn = nameEn;
            this.nameHi = nameHi;
            this.themeEn = themeEn;
            this.themeHi = themeHi;
            this.aspectOffsets = aspectOffsets;
        }
    }

    private static final class HouseInfo {
        private final int number;
        private final String nameEn;
        private final String nameHi;
        private final String themeEn;
        private final String themeHi;

        private HouseInfo(int number, String nameEn, String nameHi, String themeEn, String themeHi) {
            this.number = number;
            this.nameEn = nameEn;
            this.nameHi = nameHi;
            this.themeEn = themeEn;
            this.themeHi = themeHi;
        }
    }

    private static final class SignInfo {
        private final String code;
        private final String nameEn;
        private final String nameHi;
        private final String natureEn;
        private final String natureHi;

        private SignInfo(String code, String nameEn, String nameHi, String natureEn, String natureHi) {
            this.code = code;
            this.nameEn = nameEn;
            this.nameHi = nameHi;
            this.natureEn = natureEn;
            this.natureHi = natureHi;
        }
    }

    private static final class NakshatraInfo {
        private final String code;
        private final String nameEn;
        private final String nameHi;
        private final String natureEn;
        private final String natureHi;

        private NakshatraInfo(String code, String nameEn, String nameHi, String natureEn, String natureHi) {
            this.code = code;
            this.nameEn = nameEn;
            this.nameHi = nameHi;
            this.natureEn = natureEn;
            this.natureHi = natureHi;
        }
    }

    private static final class StrengthState {
        private final String code;
        private final String nameEn;
        private final String nameHi;
        private final String effectEn;
        private final String effectHi;

        private StrengthState(String code, String nameEn, String nameHi, String effectEn, String effectHi) {
            this.code = code;
            this.nameEn = nameEn;
            this.nameHi = nameHi;
            this.effectEn = effectEn;
            this.effectHi = effectHi;
        }
    }
}
