package com.astro.backend.Repositry;

import com.astro.backend.Entity.OtpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTransactionRepository extends JpaRepository<OtpTransaction, Long> {

    Optional<OtpTransaction> findByRefNumber(String refNumber);

    Optional<OtpTransaction> findByMobileNumberAndRefNumber(String mobileNumber, String refNumber);

    Optional<OtpTransaction> findByMobileNumberOrderByCreatedAtDesc(String mobileNumber);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
