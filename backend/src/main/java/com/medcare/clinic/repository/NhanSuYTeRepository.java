package com.medcare.clinic.repository;

import com.medcare.clinic.entity.NhanSuYTe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NhanSuYTeRepository extends JpaRepository<NhanSuYTe, Long> {
    Optional<NhanSuYTe> findByTenDangNhap(String tenDangNhap);
    List<NhanSuYTe> findByKhoaId(Long khoaId);
}
