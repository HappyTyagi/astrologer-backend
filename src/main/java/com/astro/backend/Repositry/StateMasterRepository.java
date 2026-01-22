package com.astro.backend.Repositry;

import com.astro.backend.Entity.StateMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StateMasterRepository extends JpaRepository<StateMaster, Long> {
    Optional<StateMaster> findByName(String name);
    Optional<StateMaster> findByCode(String code);
    List<StateMaster> findByIsActiveOrderByName(Boolean isActive);
}
