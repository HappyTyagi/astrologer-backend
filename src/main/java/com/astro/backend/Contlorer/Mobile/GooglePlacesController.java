package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.ResponseDTO.GooglePlaceDetailsResponse;
import com.astro.backend.ResponseDTO.GooglePlaceSuggestionResponse;
import com.astro.backend.Services.GooglePlacesProxyService;
import com.astro.backend.apiResponse.ApiResponse;
import com.astro.backend.apiResponse.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mobile/places")
@RequiredArgsConstructor
public class GooglePlacesController {

    private final GooglePlacesProxyService googlePlacesProxyService;

    @GetMapping("/autocomplete")
    public ResponseEntity<ApiResponse<List<GooglePlaceSuggestionResponse>>> autocomplete(
            @RequestParam("input") String input,
            @RequestParam(value = "country", defaultValue = "IN") String countryCode
    ) {
        try {
            final List<GooglePlaceSuggestionResponse> suggestions =
                    googlePlacesProxyService.autocomplete(input, countryCode);
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            suggestions,
                            true,
                            "Address suggestions fetched successfully"
                    )
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseUtils.createFailureResponse(ex.getMessage(), false, "INVALID_REQUEST"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ResponseUtils.createFailureResponse(ex.getMessage(), false, "GOOGLE_CONFIG_MISSING"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseUtils.createFailureResponse(
                            "Failed to fetch address suggestions",
                            false,
                            "PLACE_AUTOCOMPLETE_FAILED"
                    ));
        }
    }

    @GetMapping("/details")
    public ResponseEntity<ApiResponse<GooglePlaceDetailsResponse>> details(
            @RequestParam("placeId") String placeId
    ) {
        try {
            final GooglePlaceDetailsResponse details = googlePlacesProxyService.getPlaceDetails(placeId);
            if (details == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseUtils.createFailureResponse(
                                "Place details not found",
                                false,
                                "PLACE_NOT_FOUND"
                        ));
            }
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            details,
                            true,
                            "Place details fetched successfully"
                    )
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseUtils.createFailureResponse(ex.getMessage(), false, "INVALID_REQUEST"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ResponseUtils.createFailureResponse(ex.getMessage(), false, "GOOGLE_CONFIG_MISSING"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseUtils.createFailureResponse(
                            "Failed to fetch place details",
                            false,
                            "PLACE_DETAILS_FAILED"
                    ));
        }
    }

    @GetMapping("/geocode")
    public ResponseEntity<ApiResponse<GooglePlaceDetailsResponse>> geocode(
            @RequestParam("address") String address,
            @RequestParam(value = "country", defaultValue = "IN") String countryCode
    ) {
        try {
            final GooglePlaceDetailsResponse details = googlePlacesProxyService.geocode(address, countryCode);
            if (details == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseUtils.createFailureResponse(
                                "Address not found",
                                false,
                                "ADDRESS_NOT_FOUND"
                        ));
            }
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            details,
                            true,
                            "Address coordinates fetched successfully"
                    )
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseUtils.createFailureResponse(ex.getMessage(), false, "INVALID_REQUEST"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ResponseUtils.createFailureResponse(ex.getMessage(), false, "GOOGLE_CONFIG_MISSING"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseUtils.createFailureResponse(
                            "Failed to geocode address",
                            false,
                            "ADDRESS_GEOCODE_FAILED"
                    ));
        }
    }
}
