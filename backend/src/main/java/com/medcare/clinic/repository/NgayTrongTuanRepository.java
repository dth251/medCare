package com.medcare.clinic.repository;

import com.medcare.clinic.entity.NgayTrongTuan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface NgayTrongTuanRepository extends JpaRepository<NgayTrongTuan, Long> {
    Optional<NgayTrongTuan> findByTuanLamViecIdAndNgay(Long tuanId, LocalDate ngay);
}
