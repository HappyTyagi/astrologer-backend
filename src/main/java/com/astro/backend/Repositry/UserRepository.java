package com.astro.backend.Repositry;

import com.astro.backend.Entity.User;
import com.astro.backend.EnumFile.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByMobileNumber(String mobileNumber);
    
    boolean existsByMobileNumber(String mobileNumber);

    List<User> findByRoleOrderByCreatedAtDesc(Role role);
}
