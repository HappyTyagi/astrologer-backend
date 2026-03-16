package com.astro.backend.Services;

import com.astro.backend.Entity.NakshatraMaster;
import com.astro.backend.Repositry.NakshatraMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NakshatraMasterSourceSyncService implements CommandLineRunner {

    private static final String BASE_URL = "https://www.drikpanchang.com";
    private static final String LIST_URL = BASE_URL + "/tutorials/nakshatra/nakshatra.html";
    private static final int MAX_FETCH_ATTEMPTS = 3;
    private static final long FETCH_RETRY_DELAY_MS = 1_000L;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36";
    private static final Map<String, String> LEGACY_NAME_ALIASES = Map.of(
            "ashwini", "ashvini"
    );

    private final NakshatraMasterRepository nakshatraMasterRepository;

    @Override
    public void run(String... args) {
        synchronizeNakshatraMasterFromSource();
    }

    private void synchronizeNakshatraMasterFromSource() {
        try {
            List<NakshatraPayload> canonicalItems = fetchCanonicalNakshatras();
            if (canonicalItems.isEmpty()) {
                log.warn("Nakshatra source sync skipped because no canonical items were fetched.");
                return;
            }

            List<NakshatraMaster> existingItems =
                    nakshatraMasterRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
            Map<String, NakshatraMaster> existingBySlug = new HashMap<>();
            Map<String, NakshatraMaster> existingByName = new HashMap<>();
            for (NakshatraMaster existingItem : existingItems) {
                indexExistingItem(existingBySlug, existingByName, existingItem);
            }

            Set<Long> usedIds = new HashSet<>();
            List<NakshatraMaster> rowsToSave = new ArrayList<>();
            int insertedCount = 0;
            int updatedCount = 0;

            for (int index = 0; index < canonicalItems.size(); index++) {
                NakshatraPayload canonical = canonicalItems.get(index);
                NakshatraMaster target =
                        resolveExisting(existingBySlug, existingByName, existingItems, usedIds, canonical, index);

                boolean isNew = target == null;
                if (target == null) {
                    target = new NakshatraMaster();
                }

                boolean changed = applyCanonicalData(target, canonical);
                if (isNew || changed) {
                    rowsToSave.add(target);
                    if (isNew) {
                        insertedCount++;
                    } else {
                        updatedCount++;
                    }
                }

                if (target.getId() != null) {
                    usedIds.add(target.getId());
                }
            }

            if (rowsToSave.isEmpty()) {
                log.info("Nakshatra master is already in sync with source. Total rows: {}", existingItems.size());
                return;
            }

            nakshatraMasterRepository.saveAll(rowsToSave);
            log.info(
                    "Nakshatra master source sync completed. Inserted {}, updated {}, canonical rows {}.",
                    insertedCount,
                    updatedCount,
                    canonicalItems.size()
            );
        } catch (Exception ex) {
            log.error("Error syncing Nakshatra master from source: {}", ex.getMessage(), ex);
        }
    }

    private List<NakshatraPayload> fetchCanonicalNakshatras() throws IOException {
        Document listDocument = fetchDocument(LIST_URL);
        Elements rows = listDocument.select(".dpEventTable > .dpFlex");
        List<NakshatraPayload> payloads = new ArrayList<>();

        for (Element row : rows) {
            Elements cells = row.select("> .dpTableCell.dpLeftContent.dpFlexEqual");
            if (cells.size() < 2) {
                continue;
            }

            Element englishLink = cells.get(0).selectFirst("a.dpTableLink");
            if (englishLink == null) {
                continue;
            }

            String detailUrl = absoluteUrl(englishLink.attr("href"));
            try {
                payloads.add(fetchDetailPayload(detailUrl));
            } catch (Exception itemEx) {
                log.warn("Failed to fetch Nakshatra details from {}: {}", detailUrl, itemEx.getMessage());
            }
        }

        return payloads;
    }

    private NakshatraPayload fetchDetailPayload(String detailUrl) throws IOException {
        Document englishDocument = fetchDocument(detailUrl);
        Document hindiDocument = fetchDocument(detailUrl + "?lang=hi");

        String slug = slugFromUrl(detailUrl);
        String nameEn = cleanEnglishTitle(textOf(englishDocument.selectFirst("h2.dpPageShortTitle")), slug);
        String nameHi = cleanHindiTitle(textOf(hindiDocument.selectFirst("h2.dpPageShortTitle")));
        String imageUrl = sanitizeImageUrl(
                absoluteUrl(attributeOf(englishDocument.selectFirst("figure.dpFreeSizeFigure img"), "src"))
        );

        Map<String, String> englishSections = extractSections(englishDocument);
        Map<String, String> hindiSections = extractSections(hindiDocument);
        validateDetailPayload(detailUrl, imageUrl, englishSections, hindiSections);

        return new NakshatraPayload(
                slug,
                nameEn,
                nameHi,
                detailUrl,
                imageUrl,
                buildDescription(englishSections, false),
                buildDescription(hindiSections, true)
        );
    }

    private Document fetchDocument(String url) throws IOException {
        IOException lastException = null;
        for (int attempt = 1; attempt <= MAX_FETCH_ATTEMPTS; attempt++) {
            try {
                return Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(30_000)
                        .get();
            } catch (IOException ex) {
                lastException = ex;
                if (attempt == MAX_FETCH_ATTEMPTS) {
                    throw ex;
                }
                log.warn(
                        "Retrying Nakshatra fetch for {} after attempt {} failed: {}",
                        url,
                        attempt,
                        ex.getMessage()
                );
                sleepBeforeRetry();
            }
        }
        throw lastException;
    }

    private Map<String, String> extractSections(Document document) {
        Map<String, String> sections = new LinkedHashMap<>();
        for (Element paragraph : document.select(".dpPageContentWrapper p.dpContent")) {
            String plainText = paragraph.text().trim();
            if (plainText.isBlank()) {
                continue;
            }

            Element strong = paragraph.selectFirst("strong");
            String label = strong == null ? "section_" + sections.size() : strong.text().trim();
            String content = plainText;
            if (!label.isBlank() && plainText.startsWith(label)) {
                content = plainText.substring(label.length()).trim();
                content = content.replaceFirst("^[-:]\\s*", "");
            }

            sections.put(label, content.trim());
        }
        return sections;
    }

    private String buildDescription(Map<String, String> sections, boolean hindi) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            String value = entry.getValue() == null ? null : entry.getValue().trim();
            if (value == null || value.isBlank()) {
                continue;
            }

            String label = entry.getKey() == null ? "" : entry.getKey().trim();
            if (label.isBlank() || label.startsWith("section_")) {
                if (lines.isEmpty()) {
                    appendLabeledLine(lines, hindi ? "विवरण" : "Description", value);
                } else {
                    appendLine(lines, value);
                }
                continue;
            }

            appendLabeledLine(lines, label, value);
        }
        return String.join("\n", lines);
    }

    private void appendLine(List<String> lines, String value) {
        if (value == null) {
            return;
        }
        String cleaned = value.trim();
        if (!cleaned.isBlank()) {
            lines.add(cleaned);
        }
    }

    private void appendLabeledLine(List<String> lines, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        appendLine(lines, label + ": " + value.trim());
    }

    private void indexExistingItem(
            Map<String, NakshatraMaster> existingBySlug,
            Map<String, NakshatraMaster> existingByName,
            NakshatraMaster existingItem
    ) {
        if (existingItem.getSlug() != null && !existingItem.getSlug().isBlank()) {
            existingBySlug.putIfAbsent(existingItem.getSlug().trim().toLowerCase(), existingItem);
        }

        putNameIndex(existingByName, existingItem.getName(), existingItem);
        putNameIndex(existingByName, existingItem.getHiName(), existingItem);

        if (existingItem.getName() != null) {
            String legacyAlias = LEGACY_NAME_ALIASES.get(normalize(existingItem.getName()));
            if (legacyAlias != null) {
                existingBySlug.putIfAbsent(legacyAlias, existingItem);
            }
        }
    }

    private void putNameIndex(Map<String, NakshatraMaster> existingByName, String value, NakshatraMaster existingItem) {
        String normalized = normalize(value);
        if (normalized != null) {
            existingByName.putIfAbsent(normalized, existingItem);
        }
    }

    private NakshatraMaster resolveExisting(
            Map<String, NakshatraMaster> existingBySlug,
            Map<String, NakshatraMaster> existingByName,
            List<NakshatraMaster> existingItems,
            Set<Long> usedIds,
            NakshatraPayload canonical,
            int index
    ) {
        NakshatraMaster existing = existingBySlug.get(canonical.slug());
        if (existing == null) {
            existing = existingByName.get(normalize(canonical.nameEn()));
        }
        if (existing == null) {
            existing = existingByName.get(normalize(canonical.nameHi()));
        }

        if (existing != null && existing.getId() != null && !usedIds.contains(existing.getId())) {
            return existing;
        }

        if (index < existingItems.size()) {
            NakshatraMaster fallback = existingItems.get(index);
            if (fallback.getId() == null || !usedIds.contains(fallback.getId())) {
                return fallback;
            }
        }

        return null;
    }

    private boolean applyCanonicalData(NakshatraMaster target, NakshatraPayload canonical) {
        boolean changed = false;

        changed |= setIfHasTextDifferent(target.getSlug(), canonical.slug(), target::setSlug);
        changed |= setIfHasTextDifferent(target.getName(), canonical.nameEn(), target::setName);
        changed |= setIfHasTextDifferent(target.getHiName(), canonical.nameHi(), target::setHiName);
        changed |= setIfHasTextDifferent(target.getImage(), canonical.imageUrl(), target::setImage);
        changed |= setIfHasTextDifferent(target.getDescription(), canonical.descriptionEn(), target::setDescription);
        changed |= setIfHasTextDifferent(target.getDescriptionHi(), canonical.descriptionHi(), target::setDescriptionHi);
        changed |= setIfHasTextDifferent(target.getSourceUrl(), canonical.sourceUrl(), target::setSourceUrl);
        changed |= setIfDifferent(target.getIsActive(), true, target::setIsActive);

        return changed;
    }

    private <T> boolean setIfDifferent(T oldValue, T newValue, java.util.function.Consumer<T> setter) {
        if (Objects.equals(oldValue, newValue)) {
            return false;
        }
        setter.accept(newValue);
        return true;
    }

    private boolean setIfHasTextDifferent(
            String oldValue,
            String newValue,
            java.util.function.Consumer<String> setter
    ) {
        if (newValue == null || newValue.isBlank()) {
            return false;
        }
        return setIfDifferent(oldValue, newValue.trim(), setter);
    }

    private String cleanEnglishTitle(String rawTitle, String fallbackSlug) {
        String fallbackName = toTitleCase(fallbackSlug.replace('-', ' '));
        if (rawTitle == null || rawTitle.isBlank()) {
            return fallbackName;
        }

        String cleaned = rawTitle.trim();
        if (cleaned.toLowerCase().endsWith(" nakshatra")) {
            cleaned = cleaned.substring(0, cleaned.length() - " nakshatra".length()).trim();
        }
        return cleaned.isBlank() ? fallbackName : cleaned;
    }

    private String cleanHindiTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return null;
        }

        String cleaned = rawTitle.trim();
        if (cleaned.endsWith(" नक्षत्र")) {
            cleaned = cleaned.substring(0, cleaned.length() - " नक्षत्र".length()).trim();
        }
        return cleaned;
    }

    private String toTitleCase(String value) {
        String[] parts = value.split("\\s+");
        List<String> formatted = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            formatted.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
        }
        return String.join(" ", formatted);
    }

    private String absoluteUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return null;
        }
        if (pathOrUrl.startsWith("data:")) {
            return pathOrUrl;
        }
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        return BASE_URL + pathOrUrl;
    }

    private String sanitizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || imageUrl.startsWith("data:")) {
            return imageUrl;
        }
        return imageUrl.replaceFirst("(?i)(\\.(?:png|jpg|jpeg|gif|svg|webp))\\.+$", "$1");
    }

    private String attributeOf(Element element, String attributeName) {
        if (element == null) {
            return null;
        }
        return element.attr(attributeName);
    }

    private String textOf(Element element) {
        if (element == null) {
            return null;
        }
        return element.text();
    }

    private String slugFromUrl(String url) {
        String cleaned = url.substring(url.lastIndexOf('/') + 1);
        if (cleaned.endsWith(".html")) {
            cleaned = cleaned.substring(0, cleaned.length() - 5);
        }
        if (cleaned.endsWith("-nakshatra")) {
            cleaned = cleaned.substring(0, cleaned.length() - "-nakshatra".length());
        }
        return cleaned.trim().toLowerCase();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.toLowerCase()
                .replace('w', 'v')
                .replaceAll("[^\\p{L}\\p{Nd}]+", "");
    }

    private void validateDetailPayload(
            String detailUrl,
            String imageUrl,
            Map<String, String> englishSections,
            Map<String, String> hindiSections
    ) throws IOException {
        boolean hasEnglishContent = imageUrl != null && !imageUrl.isBlank() || !englishSections.isEmpty();
        boolean hasHindiContent = !hindiSections.isEmpty();
        if (hasEnglishContent && hasHindiContent) {
            return;
        }
        throw new IOException("Incomplete content fetched from source: " + detailUrl);
    }

    private void sleepBeforeRetry() throws IOException {
        try {
            Thread.sleep(FETCH_RETRY_DELAY_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while retrying Nakshatra source fetch", ex);
        }
    }

    private record NakshatraPayload(
            String slug,
            String nameEn,
            String nameHi,
            String sourceUrl,
            String imageUrl,
            String descriptionEn,
            String descriptionHi
    ) {
    }
}
