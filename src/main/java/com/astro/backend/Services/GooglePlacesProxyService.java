package com.astro.backend.Services;

import com.astro.backend.ResponseDTO.GooglePlaceDetailsResponse;
import com.astro.backend.ResponseDTO.GooglePlaceSuggestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GooglePlacesProxyService {

    private final RestTemplate restTemplate;

    @Value("${google.maps.api-key:}")
    private String googleApiKey;

    @Value("${google.maps.places-autocomplete-url:https://maps.googleapis.com/maps/api/place/autocomplete/json}")
    private String placesAutocompleteUrl;

    @Value("${google.maps.places-details-url:https://maps.googleapis.com/maps/api/place/details/json}")
    private String placesDetailsUrl;

    @Value("${google.maps.geocode-url:https://maps.googleapis.com/maps/api/geocode/json}")
    private String geocodeUrl;

    public List<GooglePlaceSuggestionResponse> autocomplete(String input, String countryCode) {
        final String query = safeText(input);
        if (query.length() < 2) {
            return List.of();
        }
        ensureConfigured();

        final String country = normalizeCountry(countryCode);
        final Map<String, Object> root = callApi(
                placesAutocompleteUrl,
                Map.of(
                        "input", query,
                        "key", googleApiKey,
                        "components", "country:" + country.toLowerCase(Locale.ROOT),
                        "language", "en"
                )
        );

        final String status = safeText(root.get("status"));
        if (!status.isEmpty() && !"OK".equalsIgnoreCase(status) && !"ZERO_RESULTS".equalsIgnoreCase(status)) {
            final String error = safeText(root.get("error_message"));
            throw new RuntimeException(error.isEmpty() ? "Google autocomplete failed with status: " + status : error);
        }

        final List<Map<String, Object>> predictions = asMapList(root.get("predictions"));
        final LinkedHashMap<String, GooglePlaceSuggestionResponse> deduped = new LinkedHashMap<>();
        for (Map<String, Object> item : predictions) {
            final String placeId = safeText(item.get("place_id"));
            final String description = safeText(item.get("description"));
            if (placeId.isEmpty() || description.isEmpty()) {
                continue;
            }
            deduped.putIfAbsent(
                    description.toLowerCase(Locale.ROOT),
                    GooglePlaceSuggestionResponse.builder()
                            .placeId(placeId)
                            .description(description)
                            .provider("google")
                            .build()
            );
        }
        return deduped.values().stream().limit(20).toList();
    }

    public GooglePlaceDetailsResponse getPlaceDetails(String placeId) {
        final String cleanedPlaceId = safeText(placeId);
        if (cleanedPlaceId.isEmpty()) {
            throw new IllegalArgumentException("placeId is required");
        }
        ensureConfigured();

        final Map<String, Object> root = callApi(
                placesDetailsUrl,
                Map.of(
                        "place_id", cleanedPlaceId,
                        "fields", "formatted_address,geometry,address_component",
                        "key", googleApiKey
                )
        );

        final String status = safeText(root.get("status"));
        if (!"OK".equalsIgnoreCase(status)) {
            if ("ZERO_RESULTS".equalsIgnoreCase(status) || "NOT_FOUND".equalsIgnoreCase(status)) {
                return null;
            }
            final String error = safeText(root.get("error_message"));
            throw new RuntimeException(error.isEmpty() ? "Google place details failed with status: " + status : error);
        }

        final Map<String, Object> result = asMap(root.get("result"));
        if (result.isEmpty()) {
            return null;
        }
        return parsePlaceResult(result, "google");
    }

    public GooglePlaceDetailsResponse geocode(String address, String countryCode) {
        final String input = safeText(address);
        if (input.length() < 2) {
            throw new IllegalArgumentException("address is required");
        }
        ensureConfigured();

        final String country = normalizeCountry(countryCode);
        final Map<String, Object> root = callApi(
                geocodeUrl,
                Map.of(
                        "address", input,
                        "key", googleApiKey,
                        "components", "country:" + country,
                        "region", country.toLowerCase(Locale.ROOT),
                        "language", "en"
                )
        );

        final String status = safeText(root.get("status"));
        if (!"OK".equalsIgnoreCase(status)) {
            if ("ZERO_RESULTS".equalsIgnoreCase(status)) {
                return null;
            }
            final String error = safeText(root.get("error_message"));
            throw new RuntimeException(error.isEmpty() ? "Google geocode failed with status: " + status : error);
        }

        final List<Map<String, Object>> results = asMapList(root.get("results"));
        if (results.isEmpty()) {
            return null;
        }

        final Map<String, Object> first = results.getFirst();
        return parsePlaceResult(first, "google_geocode");
    }

    private GooglePlaceDetailsResponse parsePlaceResult(Map<String, Object> result, String provider) {
        final Map<String, Object> geometry = asMap(result.get("geometry"));
        final Map<String, Object> location = asMap(geometry.get("location"));
        final List<Map<String, Object>> components = asMapList(result.get("address_components"));

        final String state = componentLongName(components, "administrative_area_level_1");
        final String city = firstNonBlank(
                componentLongName(components, "locality"),
                componentLongName(components, "administrative_area_level_2"),
                componentLongName(components, "sublocality")
        );
        final String pincode = componentLongName(components, "postal_code");

        return GooglePlaceDetailsResponse.builder()
                .formattedAddress(safeText(result.get("formatted_address")))
                .latitude(asDouble(location.get("lat")))
                .longitude(asDouble(location.get("lng")))
                .state(state.isBlank() ? null : state)
                .city(city.isBlank() ? null : city)
                .pincode(pincode.isBlank() ? null : pincode)
                .provider(provider)
                .build();
    }

    private String componentLongName(List<Map<String, Object>> components, String type) {
        for (Map<String, Object> component : components) {
            final List<String> types = asStringList(component.get("types"));
            if (types.contains(type)) {
                final String value = safeText(component.get("long_name"));
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callApi(String url, Map<String, String> queryParams) {
        try {
            final UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            queryParams.forEach(builder::queryParam);
            final String uri = builder.build(true).toUriString();
            final Map<String, Object> body = restTemplate.getForObject(uri, Map.class);
            return body == null ? Map.of() : body;
        } catch (Exception ex) {
            log.error("Google places API call failed for url={}", url, ex);
            throw new RuntimeException("Unable to fetch address data right now");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> casted = new LinkedHashMap<>();
            map.forEach((key, val) -> casted.put(String.valueOf(key), val));
            return casted;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asMapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        final List<Map<String, Object>> mapped = new ArrayList<>();
        for (Object item : list) {
            mapped.add(asMap(item));
        }
        return mapped;
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(String::valueOf)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        final String text = safeText(value);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String safeText(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private String normalizeCountry(String countryCode) {
        final String normalized = safeText(countryCode).toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "IN";
        }
        return normalized;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private void ensureConfigured() {
        if (googleApiKey == null || googleApiKey.trim().isEmpty()) {
            throw new IllegalStateException("GOOGLE_MAPS_API_KEY is not configured on backend");
        }
    }
}
