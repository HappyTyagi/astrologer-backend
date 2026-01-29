package com.astro.backend.Repositry;

import com.astro.backend.Entity.MobileUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MobileUserProfileRepository extends JpaRepository<MobileUserProfile, Long> {

    /**
     * Find mobile user profile by user ID
     */
    Optional<MobileUserProfile> findByUserId(Long userId);



    /**
     * Find mobile user profile by mobile number
     */
    Optional<MobileUserProfile> findByMobileNumber(String mobileNumber);

    /**
     * Check if mobile user profile exists for a user
     */
    boolean existsByUserId(Long userId);

    /**
     * Find by device ID
     */
    Optional<MobileUserProfile> findByDeviceId(String deviceId);

    /**
     * Find by referral code
     */
    Optional<MobileUserProfile> findByReferralCode(String referralCode);

    /**
     * Delete by user ID
     */
    void deleteByUserId(Long userId);
}
