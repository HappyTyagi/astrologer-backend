package com.astro.backend.Repositry;

import com.astro.backend.Entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserMobileNumber(String userMobileNumber);

    long countByUserMobileNumber(String userMobileNumber);
}
