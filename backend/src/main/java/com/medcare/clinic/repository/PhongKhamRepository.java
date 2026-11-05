package com.medcare.clinic.repository;

import com.medcare.clinic.entity.PhongKham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PhongKhamRepository extends JpaRepository<PhongKham, Long> {
    
}
