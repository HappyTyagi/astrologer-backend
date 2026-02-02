package com.astro.backend.Services;

import com.astro.backend.Entity.DistrictMaster;
import com.astro.backend.Entity.StateMaster;
import com.astro.backend.Repositry.DistrictMasterRepository;
import com.astro.backend.Repositry.StateMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationCoordinatesService {

    private final StateMasterRepository stateMasterRepository;
    private final DistrictMasterRepository districtMasterRepository;

    /**
     * Get coordinates for a state (uses capital city coordinates)
     * @param stateId State ID
     * @return double[] {latitude, longitude} or null if not found
     */
    public double[] getStateCoordinates(Long stateId) {
        if (stateId == null || stateId <= 0) {
            return null;
        }

        Optional<StateMaster> stateOpt = stateMasterRepository.findById(stateId);
        if (stateOpt.isPresent()) {
            StateMaster state = stateOpt.get();
            if (state.getLatitude() != null && state.getLongitude() != null) {
                log.info("Using state coordinates for {}: lat={}, lon={}", state.getName(), state.getLatitude(), state.getLongitude());
                return new double[]{state.getLatitude(), state.getLongitude()};
            }
        }
        return null;
    }

    /**
     * Get coordinates for a district (uses district center coordinates)
     * @param districtId District ID
     * @return double[] {latitude, longitude} or null if not found
     */
    public double[] getDistrictCoordinates(Long districtId) {
        if (districtId == null || districtId <= 0) {
            return null;
        }

        Optional<DistrictMaster> districtOpt = districtMasterRepository.findById(districtId);
        if (districtOpt.isPresent()) {
            DistrictMaster district = districtOpt.get();
            if (district.getLatitude() != null && district.getLongitude() != null) {
                log.info("Using district coordinates for {}: lat={}, lon={}", district.getName(), district.getLatitude(), district.getLongitude());
                return new double[]{district.getLatitude(), district.getLongitude()};
            }
        }
        return null;
    }

    /**
     * Get coordinates with priority: District > State > Default GMT
     * @param districtId District ID (optional)
     * @param stateId State ID (optional)
     * @return double[] {latitude, longitude}
     */
    public double[] getCoordinatesWithFallback(Long districtId, Long stateId) {
        // Try district first (most accurate)
        double[] districtCoords = getDistrictCoordinates(districtId);
        if (districtCoords != null) {
            return districtCoords;
        }

        // Fallback to state
        double[] stateCoords = getStateCoordinates(stateId);
        if (stateCoords != null) {
            return stateCoords;
        }

        // Default to GMT (0, 0) for planetary calculations without location
        log.warn("No coordinates found for district={}, state={}. Using GMT default.", districtId, stateId);
        return new double[]{0.0, 0.0};
    }
}
