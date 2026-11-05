package com.medcare.clinic.repository;

import com.medcare.clinic.entity.PhieuDangKy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhieuDangKyRepository extends JpaRepository<PhieuDangKy, Long> {
    Optional<PhieuDangKy> findByNhanSuYTeIdAndTuanLamViecId(Long nhanSuId, Long tuanLamViecId);
    List<PhieuDangKy> findByTuanLamViecId(Long tuanLamViecId);
}
