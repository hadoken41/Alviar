package com.ordering.system.repository;

import com.ordering.system.entity.Labor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LaborRepository extends JpaRepository<Labor, Long> {
    List<Labor> findByNameContainingIgnoreCase(String name);
    List<Labor> findByRole(String role);
    List<Labor> findByShift(String shift);
}