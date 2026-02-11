package com.astro.backend.Repositry;

import com.astro.backend.Entity.RemidesCart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RemidesCartRepository extends JpaRepository<RemidesCart, Long> {

    @EntityGraph(attributePaths = {"remides"})
    List<RemidesCart> findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(Long userId);

    Optional<RemidesCart> findByUserIdAndRemides_IdAndIsActiveTrue(Long userId, Long remidesId);

    List<RemidesCart> findByUserId(Long userId);
}
