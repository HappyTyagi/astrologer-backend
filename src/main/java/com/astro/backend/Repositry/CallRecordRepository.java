package com.astro.backend.Repositry;

import com.astro.backend.Entity.CallRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {
    List<CallRecord> findTop200ByOrderByCreatedAtDesc();
}
