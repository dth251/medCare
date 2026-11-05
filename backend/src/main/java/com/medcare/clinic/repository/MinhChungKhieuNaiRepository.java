package com.medcare.clinic.repository;

import com.medcare.clinic.entity.MinhChungKhieuNai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MinhChungKhieuNaiRepository extends JpaRepository<MinhChungKhieuNai, Long> {
    
}
