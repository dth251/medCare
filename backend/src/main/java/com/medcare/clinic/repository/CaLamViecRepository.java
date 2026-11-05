package com.medcare.clinic.repository;

import com.medcare.clinic.entity.CaLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CaLamViecRepository extends JpaRepository<CaLamViec, Long> {
    Optional<CaLamViec> findByTenCa(String tenCa);
}
