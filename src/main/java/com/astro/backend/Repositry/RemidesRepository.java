package com.astro.backend.Repositry;

import com.astro.backend.Entity.Remides;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RemidesRepository extends JpaRepository<Remides, Long> {
    List<Remides> findByUserId(Long userId);
}
