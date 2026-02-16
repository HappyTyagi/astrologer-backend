package com.astro.backend.Repositry;

import com.astro.backend.Entity.EmailOtpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailOtpTransactionRepository extends JpaRepository<EmailOtpTransaction, Long> {
    Optional<EmailOtpTransaction> findFirstByRefNumberAndEmailOrderByIdDesc(String refNumber, String email);
}

