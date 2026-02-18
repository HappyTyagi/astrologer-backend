package com.astro.backend.Repositry;

import com.astro.backend.Entity.CallRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {
    List<CallRecord> findTop200ByOrderByCreatedAtDesc();
    Optional<CallRecord> findTopByCallIdOrderByCreatedAtDesc(String callId);
}
