package com.medcare.clinic.repository;

import com.medcare.clinic.entity.ThanhVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ThanhVienRepository extends JpaRepository<ThanhVien, Long> {
    Optional<ThanhVien> findByTenDangNhap(String tenDangNhap);
}
