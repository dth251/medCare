package com.medcare.clinic.repository;

import com.medcare.clinic.entity.TuanLamViec;
import com.medcare.clinic.enums.TrangThaiTuan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TuanLamViecRepository extends JpaRepository<TuanLamViec, Long> {
    List<TuanLamViec> findByTrangThai(TrangThaiTuan trangThai);

    List<TuanLamViec> findAllByOrderByNgayBatDauDesc();

    Optional<TuanLamViec> findByNamLamViecIdAndTen(Long namLamViecId, String ten);

    @Query("SELECT t FROM TuanLamViec t WHERE t.ngayBatDau <= :date AND t.ngayKetThuc >= :date")
    Optional<TuanLamViec> timTheoNgay(@Param("date") LocalDate date);
}
