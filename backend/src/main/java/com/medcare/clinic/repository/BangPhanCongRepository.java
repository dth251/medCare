package com.medcare.clinic.repository;

import com.medcare.clinic.entity.BangPhanCong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BangPhanCongRepository extends JpaRepository<BangPhanCong, Long> {
    Optional<BangPhanCong> findByKhoaIdAndTuanLamViecId(Long khoaId, Long tuanLamViecId);
    List<BangPhanCong> findByTuanLamViecId(Long tuanLamViecId);
}
