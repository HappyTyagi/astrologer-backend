package com.astro.backend.Repositry;

import com.astro.backend.Entity.AdminNotificationDispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminNotificationDispatchRepository extends JpaRepository<AdminNotificationDispatch, Long> {
    List<AdminNotificationDispatch> findTop200ByOrderByCreatedAtDesc();
}
