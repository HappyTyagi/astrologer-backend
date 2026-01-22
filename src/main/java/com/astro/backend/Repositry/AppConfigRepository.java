package com.astro.backend.Repositry;

import com.astro.backend.Entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {

    Optional<AppConfig> findByConfigKey(String configKey);

    List<AppConfig> findByCategory(String category);

    List<AppConfig> findByIsActive(Boolean isActive);

    List<AppConfig> findByCategoryAndIsActive(String category, Boolean isActive);
}
