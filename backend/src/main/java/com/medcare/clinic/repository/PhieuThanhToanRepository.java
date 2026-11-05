package com.medcare.clinic.repository;

import com.medcare.clinic.entity.PhieuThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhieuThanhToanRepository extends JpaRepository<PhieuThanhToan, Long> {
    Optional<PhieuThanhToan> findByNhanSuYTeIdAndTuanLamViecId(Long nhanSuId, Long tuanId);
    List<PhieuThanhToan> findByTuanLamViecId(Long tuanId);
    List<PhieuThanhToan> findByNhanSuYTeId(Long nhanSuId);
    List<PhieuThanhToan> findByNhanSuYTeIdOrderByNgayLapDesc(Long nhanSuId);
}
