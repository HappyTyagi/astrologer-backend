package com.astro.backend.Repositry;

import com.astro.backend.Entity.VedicAstrologyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VedicAstrologyRuleRepository extends JpaRepository<VedicAstrologyRule, Long> {

    boolean existsByRuleCode(String ruleCode);

    @Query("select r.ruleCode from VedicAstrologyRule r")
    List<String> findAllRuleCodes();
}
