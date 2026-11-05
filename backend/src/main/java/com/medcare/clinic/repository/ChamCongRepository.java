package com.medcare.clinic.repository;

import com.medcare.clinic.entity.ChamCong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChamCongRepository extends JpaRepository<ChamCong, Long> {
    Optional<ChamCong> findByBangPhanCongId(Long bangPhanCongId);
    List<ChamCong> findByBangPhanCong_NhanSuYTe_IdAndBangPhanCong_TuanLamViec_Id(Long nhanSuId, Long tuanLamViecId);
}
