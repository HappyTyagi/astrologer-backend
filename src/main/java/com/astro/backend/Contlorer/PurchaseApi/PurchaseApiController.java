package com.astro.backend.Contlorer.PurchaseApi;

import com.astro.backend.ResponseDTO.PurchaseApi.ProxyErrorResponse;
import com.astro.backend.Services.PurchaseApi.PurchaseApiService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseApiController {
    private final PurchaseApiService purchaseApiService;

    public PurchaseApiController(PurchaseApiService purchaseApiService) {
        this.purchaseApiService = purchaseApiService;
    }

    // ============ PLANETS & EXTENDED ============
    @PostMapping("/planets")
    public ResponseEntity<?> getPlanets(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/planets", body);
    }

    @PostMapping("/planets/extended")
    public ResponseEntity<?> getPlanetsExtended(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/planets/extended", body);
    }

    // ============ DIVISIONAL CHARTS - INFO ============
    @PostMapping("/navamsa-chart-info")
    public ResponseEntity<?> getNavamsaChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/navamsa-chart-info", body);
    }

    @PostMapping("/d2-chart-info")
    public ResponseEntity<?> getD2ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d2-chart-info", body);
    }

    @PostMapping("/d3-chart-info")
    public ResponseEntity<?> getD3ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d3-chart-info", body);
    }

    @PostMapping("/d4-chart-info")
    public ResponseEntity<?> getD4ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d4-chart-info", body);
    }

    @PostMapping("/d5-chart-info")
    public ResponseEntity<?> getD5ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d5-chart-info", body);
    }

    @PostMapping("/d6-chart-info")
    public ResponseEntity<?> getD6ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d6-chart-info", body);
    }

    @PostMapping("/d7-chart-info")
    public ResponseEntity<?> getD7ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d7-chart-info", body);
    }

    @PostMapping("/d8-chart-info")
    public ResponseEntity<?> getD8ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d8-chart-info", body);
    }

    @PostMapping("/d10-chart-info")
    public ResponseEntity<?> getD10ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d10-chart-info", body);
    }

    @PostMapping("/d11-chart-info")
    public ResponseEntity<?> getD11ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d11-chart-info", body);
    }

    @PostMapping("/d12-chart-info")
    public ResponseEntity<?> getD12ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d12-chart-info", body);
    }

    @PostMapping("/d16-chart-info")
    public ResponseEntity<?> getD16ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d16-chart-info", body);
    }

    @PostMapping("/d20-chart-info")
    public ResponseEntity<?> getD20ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d20-chart-info", body);
    }

    @PostMapping("/d24-chart-info")
    public ResponseEntity<?> getD24ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d24-chart-info", body);
    }

    @PostMapping("/d27-chart-info")
    public ResponseEntity<?> getD27ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d27-chart-info", body);
    }

    @PostMapping("/d30-chart-info")
    public ResponseEntity<?> getD30ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d30-chart-info", body);
    }

    @PostMapping("/d40-chart-info")
    public ResponseEntity<?> getD40ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d40-chart-info", body);
    }

    @PostMapping("/d60-chart-info")
    public ResponseEntity<?> getD60ChartInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d60-chart-info", body);
    }

    // ============ SHADBALA (PLANETARY STRENGTHS) ============
    @PostMapping("/shadbala/break-up")
    public ResponseEntity<?> getShadbalaBreaKup(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/shadbala/break-up", body);
    }

    @PostMapping("/shadbala/summary")
    public ResponseEntity<?> getShadbalasSummary(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/shadbala/summary", body);
    }

    @PostMapping("/shadbala/sthana-bala")
    public ResponseEntity<?> getShadbalaSthannaBala(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/shadbala/sthana-bala", body);
    }

    @PostMapping("/shadbala/dig-bala")
    public ResponseEntity<?> getShadbalDigBala(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/shadbala/dig-bala", body);
    }

    @PostMapping("/shadbala/kaala-bala")
    public ResponseEntity<?> getShadbalKaalaBala(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/shadbala/kaala-bala", body);
    }

    @PostMapping("/shadbala/cheshta-bala")
    public ResponseEntity<?> getShadbalChestaBala(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/shadbala/cheshta-bala", body);
    }

    @PostMapping("/shadbala/naisargika-bala")
    public ResponseEntity<?> getShadbalNaisargikaBala(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/shadbala/naisargika-bala", body);
    }

    @PostMapping("/shadbala/drig-bala")
    public ResponseEntity<?> getShadbalDrigBala(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/shadbala/drig-bala", body);
    }

    // ============ MATCH MAKING ============
    @PostMapping("/match-making/ashtakoot-score")
    public ResponseEntity<?> getAshtakootScore(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/match-making/ashtakoot-score", body);
    }

    // ============ VIMSOTTARI DASA ============
    @PostMapping("/vimsottari/maha-dasas")
    public ResponseEntity<?> getVimsottariMahaDasas(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/vimsottari/maha-dasas", body);
    }

    @PostMapping("/vimsottari/maha-dasas-and-antar-dasas")
    public ResponseEntity<?> getVimsottariMahaDasasAndAntarDasas(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/vimsottari/maha-dasas-and-antar-dasas", body);
    }

    @PostMapping("/vimsottari/dasa-information")
    public ResponseEntity<?> getVimsottariDasaInformation(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/vimsottari/dasa-information", body);
    }

    // ============ WESTERN ASTROLOGY ============
    @PostMapping("/western/planets")
    public ResponseEntity<?> getWesternPlanets(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/western/planets", body);
    }

    @PostMapping("/western/houses")
    public ResponseEntity<?> getWesternHouses(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/western/houses", body);
    }

    @PostMapping("/western/aspects")
    public ResponseEntity<?> getWesternAspects(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/western/aspects", body);
    }

    @PostMapping("/western/natal-wheel-chart")
    public ResponseEntity<?> getWesternNatalWheelChart(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/western/natal-wheel-chart", body);
    }

    // ============ TIMING & MUHURAT ============
    @PostMapping("/brahma-muhurat")
    public ResponseEntity<?> getBrahmaMuhurat(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/brahma-muhurat", body);
    }

    @PostMapping("/abhijit-muhurat")
    public ResponseEntity<?> getAbhijitMuhurat(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/abhijit-muhurat", body);
    }

    @PostMapping("/amrit-kaal")
    public ResponseEntity<?> getAmritKaal(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/amrit-kaal", body);
    }

    @PostMapping("/rahu-kalam")
    public ResponseEntity<?> getRahuKalam(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/rahu-kalam", body);
    }

    @PostMapping("/gulika-kalam")
    public ResponseEntity<?> getGulikaKalam(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/gulika-kalam", body);
    }

    @PostMapping("/yama-gandam")
    public ResponseEntity<?> getYamaGandam(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/yama-gandam", body);
    }

    @PostMapping("/dur-muhurat")
    public ResponseEntity<?> getDurMuhurat(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/dur-muhurat", body);
    }

    @PostMapping("/good-bad-times")
    public ResponseEntity<?> getGoodBadTimes(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/good-bad-times", body);
    }

    // ============ TIMINGS & DURATIONS ============
    @PostMapping("/tithi-durations")
    public ResponseEntity<?> getTithiDurations(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/tithi-durations", body);
    }

    @PostMapping("/nakshatra-durations")
    public ResponseEntity<?> getNakshatraDurations(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/nakshatra-durations", body);
    }

    @PostMapping("/karana-durations")
    public ResponseEntity<?> getKaranaDurations(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/karana-durations", body);
    }

    @PostMapping("/yoga-durations")
    public ResponseEntity<?> getYogaDurations(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/yoga-durations", body);
    }

    @PostMapping("/choghadiya-timings")
    public ResponseEntity<?> getChoghadiyaTimings(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/choghadiya-timings", body);
    }

    @PostMapping("/hora-timings")
    public ResponseEntity<?> getHoraTimings(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/hora-timings", body);
    }

    // ============ INFORMATION & DETAILS ============
    @PostMapping("/aayanam")
    public ResponseEntity<?> getAayanam(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/aayanam", body);
    }

    @PostMapping("/vedicweekday")
    public ResponseEntity<?> getVedicWeekday(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/vedicweekday", body);
    }

    @PostMapping("/geo-details")
    public ResponseEntity<?> getGeoDetails(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/geo-details", body);
    }

    @PostMapping("/timezone-with-dst")
    public ResponseEntity<?> getTimezoneWithDst(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/timezone-with-dst", body);
    }

    @PostMapping("/rituinfo")
    public ResponseEntity<?> getRituInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/rituinfo", body);
    }

    @PostMapping("/samvatinfo")
    public ResponseEntity<?> getSamvatInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/samvatinfo", body);
    }

    @PostMapping("/lunarmonthinfo")
    public ResponseEntity<?> getLunarMonthInfo(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/lunarmonthinfo", body);
    }

    @PostMapping("/varjyam")
    public ResponseEntity<?> getVarjyam(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/varjyam", body);
    }

    // ============ SVG CHART ENDPOINTS - DIVISIONAL ============
    @PostMapping("/navamsa-chart-svg-code")
    public ResponseEntity<?> getNavamsaChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/navamsa-chart-svg-code", body);
    }

    @PostMapping("/navamsa-chart-url")
    public ResponseEntity<?> getNavamsaChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/navamsa-chart-url", body);
    }

    @PostMapping("/d2-chart-svg-code")
    public ResponseEntity<?> getD2ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d2-chart-svg-code", body);
    }

    @PostMapping("/d2-chart-url")
    public ResponseEntity<?> getD2ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d2-chart-url", body);
    }

    @PostMapping("/d3-chart-svg-code")
    public ResponseEntity<?> getD3ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d3-chart-svg-code", body);
    }

    @PostMapping("/d3-chart-url")
    public ResponseEntity<?> getD3ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d3-chart-url", body);
    }

    @PostMapping("/d4-chart-svg-code")
    public ResponseEntity<?> getD4ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d4-chart-svg-code", body);
    }

    @PostMapping("/d4-chart-url")
    public ResponseEntity<?> getD4ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d4-chart-url", body);
    }

    @PostMapping("/d5-chart-svg-code")
    public ResponseEntity<?> getD5ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d5-chart-svg-code", body);
    }

    @PostMapping("/d5-chart-url")
    public ResponseEntity<?> getD5ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d5-chart-url", body);
    }

    @PostMapping("/d6-chart-svg-code")
    public ResponseEntity<?> getD6ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d6-chart-svg-code", body);
    }

    @PostMapping("/d6-chart-url")
    public ResponseEntity<?> getD6ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d6-chart-url", body);
    }

    @PostMapping("/d7-chart-svg-code")
    public ResponseEntity<?> getD7ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d7-chart-svg-code", body);
    }

    @PostMapping("/d7-chart-url")
    public ResponseEntity<?> getD7ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d7-chart-url", body);
    }

    @PostMapping("/d8-chart-svg-code")
    public ResponseEntity<?> getD8ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d8-chart-svg-code", body);
    }

    @PostMapping("/d8-chart-url")
    public ResponseEntity<?> getD8ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d8-chart-url", body);
    }

    @PostMapping("/d10-chart-svg-code")
    public ResponseEntity<?> getD10ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d10-chart-svg-code", body);
    }

    @PostMapping("/d10-chart-url")
    public ResponseEntity<?> getD10ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d10-chart-url", body);
    }

    @PostMapping("/d11-chart-svg-code")
    public ResponseEntity<?> getD11ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d11-chart-svg-code", body);
    }

    @PostMapping("/d11-chart-url")
    public ResponseEntity<?> getD11ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d11-chart-url", body);
    }

    @PostMapping("/d12-chart-svg-code")
    public ResponseEntity<?> getD12ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d12-chart-svg-code", body);
    }

    @PostMapping("/d12-chart-url")
    public ResponseEntity<?> getD12ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d12-chart-url", body);
    }

    @PostMapping("/d16-chart-svg-code")
    public ResponseEntity<?> getD16ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d16-chart-svg-code", body);
    }

    @PostMapping("/d16-chart-url")
    public ResponseEntity<?> getD16ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d16-chart-url", body);
    }

    @PostMapping("/d20-chart-svg-code")
    public ResponseEntity<?> getD20ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d20-chart-svg-code", body);
    }

    @PostMapping("/d20-chart-url")
    public ResponseEntity<?> getD20ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d20-chart-url", body);
    }

    @PostMapping("/d24-chart-svg-code")
    public ResponseEntity<?> getD24ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d24-chart-svg-code", body);
    }

    @PostMapping("/d24-chart-url")
    public ResponseEntity<?> getD24ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d24-chart-url", body);
    }

    @PostMapping("/d27-chart-svg-code")
    public ResponseEntity<?> getD27ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d27-chart-svg-code", body);
    }

    @PostMapping("/d27-chart-url")
    public ResponseEntity<?> getD27ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d27-chart-url", body);
    }

    @PostMapping("/d30-chart-svg-code")
    public ResponseEntity<?> getD30ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d30-chart-svg-code", body);
    }

    @PostMapping("/d30-chart-url")
    public ResponseEntity<?> getD30ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d30-chart-url", body);
    }

    @PostMapping("/d40-chart-svg-code")
    public ResponseEntity<?> getD40ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d40-chart-svg-code", body);
    }

    @PostMapping("/d40-chart-url")
    public ResponseEntity<?> getD40ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d40-chart-url", body);
    }

    @PostMapping("/d45-chart-svg-code")
    public ResponseEntity<?> getD45ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d45-chart-svg-code", body);
    }

    @PostMapping("/d45-chart-url")
    public ResponseEntity<?> getD45ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d45-chart-url", body);
    }

    @PostMapping("/d60-chart-svg-code")
    public ResponseEntity<?> getD60ChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d60-chart-svg-code", body);
    }

    @PostMapping("/d60-chart-url")
    public ResponseEntity<?> getD60ChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/d60-chart-url", body);
    }

    @PostMapping("/horoscope-chart-svg-code")
    public ResponseEntity<?> getHoroscopeChartSvgCode(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/horoscope-chart-svg-code", body);
    }

    @PostMapping("/horoscope-chart-url")
    public ResponseEntity<?> getHoroscopeChartUrl(@RequestBody(required = false) JsonNode body) {
        return callPurchaseApi("/horoscope-chart-url", body);
    }

    // ============ INTERNAL HELPER ============
    private ResponseEntity<?> callPurchaseApi(String path, @RequestBody(required = false) JsonNode body) {
        try {
            ResponseEntity<String> response = purchaseApiService.post(path, body);
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(response.getHeaders());
            return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
        } catch (HttpStatusCodeException ex) {
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(ex.getResponseHeaders());
            return new ResponseEntity<>(ex.getResponseBodyAsString(), headers, ex.getStatusCode());
        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(new ProxyErrorResponse(false, "Failed to call Free Astrology API"));
        }
    }
}
