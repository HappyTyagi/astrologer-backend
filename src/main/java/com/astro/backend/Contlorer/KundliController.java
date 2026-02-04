package com.astro.backend.Contlorer;

import com.astro.backend.RequestDTO.KundliSvgRequest;
import com.astro.backend.RequestDTO.PlanetaryPositionRequest;
import com.astro.backend.ResponseDTO.PlanetaryPositionResponse;
import com.astro.backend.Services.KundliSvgService;
import com.astro.backend.Services.PlanetaryCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kundli")
@CrossOrigin(origins = "*")
public class KundliController {

    @Autowired
    private KundliSvgService kundliSvgService;
    
    @Autowired
    private PlanetaryCalculationService planetaryCalculationService;

    /**
     * Generate Kundli SVG Chart
     * POST /api/kundli/generate-svg
     * 
     * Request Body:
     * {
     *   "year": 1990,
     *   "month": 5,
     *   "date": 15,
     *   "hours": 14,
     *   "minutes": 30,
     *   "seconds": 0,
     *   "latitude": 28.7041,
     *   "longitude": 77.1025,
     *   "timezone": 5.5,
     *   "ayanamsha": "lahiri",
     *   "observation_point": "Delhi"
     * }
     */
    @PostMapping(value = "/generate-svg", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> generateKundliSvg(@RequestBody KundliSvgRequest request) {
        try {
            if (request.getYear() == 0 || request.getMonth() == 0 || request.getDate() == 0) {
                return ResponseEntity.badRequest()
                    .body("<error>Invalid date parameters. Year, month, and date are required.</error>");
            }
            
            String svgData = kundliSvgService.generateKundliSvg(
                request.getYear(),
                request.getMonth(),
                request.getDate(),
                request.getHours(),
                request.getMinutes(),
                request.getSeconds(),
                request.getLatitude(),
                request.getLongitude(),
                request.getTimezone()
            );
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(svgData);
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("<error>Error generating Kundli SVG: " + e.getMessage() + "</error>");
        }
    }

    /**
     * Simplified endpoint for quick SVG generation with defaults
     * POST /api/kundli/generate-svg-simple
     * 
     * Request Body:
     * {
     *   "year": 1990,
     *   "month": 5,
     *   "date": 15,
     *   "hours": 14,
     *   "minutes": 30
     * }
     */
    @PostMapping(value = "/generate-svg-simple", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> generateKundliSvgSimple(@RequestBody KundliSvgRequest request) {
        try {
            // Use defaults for missing parameters
            double latitude = request.getLatitude() != 0 ? request.getLatitude() : 28.7041; // Delhi
            double longitude = request.getLongitude() != 0 ? request.getLongitude() : 77.1025;
            double timezone = request.getTimezone() != 0 ? request.getTimezone() : 5.5;
            
            String svgData = kundliSvgService.generateKundliSvg(
                request.getYear(),
                request.getMonth(),
                request.getDate(),
                request.getHours(),
                request.getMinutes(),
                request.getSeconds(),
                latitude,
                longitude,
                timezone
            );
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(svgData);
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("<error>Error generating Kundli SVG: " + e.getMessage() + "</error>");
        }
    }

    /**
     * Calculate Planetary Positions with House Numbers
     * POST /api/kundli/planetary-positions
     * 
     * Request Body:
     * {
     *   "year": 2000,
     *   "month": 8,
     *   "date": 14,
     *   "hours": 3,
     *   "minutes": 29,
     *   "seconds": 0,
     *   "latitude": 27.183333,
     *   "longitude": 78.016667,
     *   "timezone": 5.5,
     *   "config": {
     *     "observation_point": "topocentric",
     *     "ayanamsha": "lahiri"
     *   }
     * }
     * 
     * Optional: Add userId query parameter to associate with user
     */
    @PostMapping(value = "/planetary-positions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlanetaryPositionResponse> calculatePlanetaryPositions(
            @RequestBody PlanetaryPositionRequest request,
            @RequestParam(required = false) Long userId) {
        try {
            // Validate required fields
            if (request.getYear() == null || request.getMonth() == null || request.getDate() == null) {
                return ResponseEntity.badRequest().build();
            }
            
            // Set defaults for missing parameters
            if (request.getHours() == null) request.setHours(0);
            if (request.getMinutes() == null) request.setMinutes(0);
            if (request.getSeconds() == null) request.setSeconds(0);
            if (request.getLatitude() == null) request.setLatitude(28.7041); // Default: Delhi
            if (request.getLongitude() == null) request.setLongitude(77.1025);
            if (request.getTimezone() == null) request.setTimezone(5.5);
            
            // Set default config if not provided
            if (request.getConfig() == null) {
                request.setConfig(PlanetaryPositionRequest.Config.builder()
                        .observationPoint("topocentric")
                        .ayanamsha("lahiri")
                        .build());
            }
            
            // Calculate planetary positions
            PlanetaryPositionResponse response = planetaryCalculationService
                    .calculatePlanetaryPositions(request, userId);
            
            // Save SVG to file using planetary data and get URL
            String svgUrl = planetaryCalculationService.saveSvgToFile(response);
            
            // Add SVG URL to response
            response.setSvgUrl(svgUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
