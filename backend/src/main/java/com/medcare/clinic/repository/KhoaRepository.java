package com.medcare.clinic.repository;

import com.medcare.clinic.entity.Khoa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface KhoaRepository extends JpaRepository<Khoa, Long> {
    Optional<Khoa> findByMaKhoa(String maKhoa);
}
