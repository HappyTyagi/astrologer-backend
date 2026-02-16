package com.astro.backend.Repositry;

import com.astro.backend.Entity.DistrictMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictMasterRepository extends JpaRepository<DistrictMaster, Long> {
    Optional<DistrictMaster> findByName(String name);
    Optional<DistrictMaster> findByNameIgnoreCase(String name);
    List<DistrictMaster> findByStateIdOrderByName(Long stateId);
    List<DistrictMaster> findByIsActiveOrderByName(Boolean isActive);
}
