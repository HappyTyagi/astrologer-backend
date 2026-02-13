package com.astro.backend.Services;

import com.astro.backend.Entity.Puja;
import com.astro.backend.Repositry.PujaSlotRepository;
import com.astro.backend.RequestDTO.PujaSlotMasterRequest;
import com.astro.backend.Repositry.PujaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PujaDataInitializationService implements CommandLineRunner {

    private final PujaRepository pujaRepository;
    private final PujaSlotRepository pujaSlotRepository;
    private final PujaService pujaService;

    @Override
    public void run(String... args) {
        initializeUpcomingPujas();
        initializeDefaultSlotsForUpcomingPujas();
    }

    private void initializeUpcomingPujas() {
        LocalDate today = LocalDate.now();
        List<Puja> seeds = List.of(
                buildPuja(
                        "Sarva Karya Siddhi Puja",
                        "Remove obstacles and invite success in work and life",
                        1100,
                        90,
                        "Success",
                        "https://images.unsplash.com/photo-1614676471928-2ed0ad1061a4?q=80&w=1200&auto=format&fit=crop",
                        "Career growth, business success, obstacle removal",
                        "Ganesh sthapana, sankalp, mantra jaap, aarti",
                        today.minusDays(1),
                        today.plusDays(21),
                        10
                ),
                buildPuja(
                        "Maha Mrityunjaya Jaap",
                        "For health protection, healing and long life blessings",
                        2100,
                        120,
                        "Health",
                        "https://images.unsplash.com/photo-1609766857041-ed402ea8069a?q=80&w=1200&auto=format&fit=crop",
                        "Health stability, peace, recovery support",
                        "Rudrabhishek, mantra jaap, havan, prasad",
                        today,
                        today.plusDays(30),
                        9
                ),
                buildPuja(
                        "Lakshmi Kuber Puja",
                        "Attract prosperity, wealth flow and financial balance",
                        1800,
                        80,
                        "Wealth",
                        "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=1200&auto=format&fit=crop",
                        "Money stability, savings growth, business support",
                        "Kalash sthapana, Lakshmi pujan, Kuber mantra",
                        today.plusDays(1),
                        today.plusDays(25),
                        8
                ),
                buildPuja(
                        "Navgrah Shanti Puja",
                        "Balance planetary effects for smoother life events",
                        2500,
                        140,
                        "Planetary",
                        "https://images.unsplash.com/photo-1518568814500-bf0f8d125f46?q=80&w=1200&auto=format&fit=crop",
                        "Planetary peace, reduced dosha effects, clarity",
                        "Navgrah mantra, offerings, shanti havan",
                        today.plusDays(2),
                        today.plusDays(35),
                        7
                )
        );

        int inserted = 0;
        for (Puja seed : seeds) {
            if (pujaRepository.findByNameIgnoreCase(seed.getName()).isPresent()) {
                continue;
            }
            pujaRepository.save(seed);
            inserted++;
        }

        if (inserted > 0) {
            log.info("Inserted {} upcoming puja entries", inserted);
        } else {
            log.info("Upcoming puja entries already exist. Skipping seed.");
        }
    }

    private void initializeDefaultSlotsForUpcomingPujas() {
        LocalDate today = LocalDate.now();
        List<Puja> pujas = pujaRepository.findByIsActiveTrue();
        int slotSeededPujaCount = 0;

        for (Puja puja : pujas) {
            if (puja.getId() == null) {
                continue;
            }
            if (puja.getStatus() != null && "INACTIVE".equalsIgnoreCase(puja.getStatus())) {
                continue;
            }

            long futureSlotCount = pujaSlotRepository.countByPujaIdAndSlotTimeAfter(
                    puja.getId(),
                    LocalDateTime.now()
            );
            if (futureSlotCount > 0) {
                continue;
            }

            LocalDate startDate = puja.getPopupStartDate() == null
                    ? today
                    : (puja.getPopupStartDate().isBefore(today) ? today : puja.getPopupStartDate());
            LocalDate endDate = puja.getPopupEndDate() == null
                    ? today.plusDays(30)
                    : puja.getPopupEndDate();

            if (endDate.isBefore(startDate)) {
                continue;
            }

            PujaSlotMasterRequest request = new PujaSlotMasterRequest();
            request.setPujaId(puja.getId());
            request.setAstrologerId(puja.getAstrologerId());
            request.setStartDate(startDate.toString());
            request.setEndDate(endDate.toString());
            request.setDayStartTime("09:00");
            request.setDayEndTime("20:00");
            request.setGapMinutes(30);
            request.setMaxBookings(1);
            request.setIsRecurring(false);
            request.setRecurringPattern(null);

            try {
                pujaService.generateSlotsFromMaster(request);
                slotSeededPujaCount++;
            } catch (Exception ex) {
                log.warn("Failed to generate default slots for pujaId {}: {}", puja.getId(), ex.getMessage());
            }
        }

        if (slotSeededPujaCount > 0) {
            log.info("Generated default future slots for {} puja(s)", slotSeededPujaCount);
        } else {
            log.info("Default future puja slots already present. Skipping slot generation.");
        }
    }

    private Puja buildPuja(
            String name,
            String description,
            double price,
            int durationMinutes,
            String category,
            String imageUrl,
            String benefits,
            String rituals,
            LocalDate popupStartDate,
            LocalDate popupEndDate,
            int popupPriority
    ) {
        return Puja.builder()
                .name(name)
                .description(description)
                .price(price)
                .durationMinutes(durationMinutes)
                .category(category)
                .image(imageUrl)
                .benefits(benefits)
                .rituals(rituals)
                .status("ACTIVE")
                .isFeatured(true)
                .featureExpiry(LocalDateTime.now().plusDays(30))
                .popupEnabled(true)
                .popupStartDate(popupStartDate)
                .popupEndDate(popupEndDate)
                .popupPriority(popupPriority)
                .popupLabel("Upcoming")
                .isActive(true)
                .build();
    }
}
