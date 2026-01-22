package com.astro.backend.Repositry;

import com.astro.backend.Entity.GenderMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GenderMasterRepository extends JpaRepository<GenderMaster, Long> {
    Optional<GenderMaster> findByName(String name);
    List<GenderMaster> findByIsActiveOrderByName(Boolean isActive);
}
