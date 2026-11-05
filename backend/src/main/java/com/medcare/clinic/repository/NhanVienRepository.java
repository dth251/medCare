package com.medcare.clinic.repository;

import com.medcare.clinic.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Long> {
    Optional<NhanVien> findByTenDangNhap(String tenDangNhap);
}
