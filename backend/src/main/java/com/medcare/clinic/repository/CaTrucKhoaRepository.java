package com.medcare.clinic.repository;

import com.medcare.clinic.entity.CaTrucKhoa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaTrucKhoaRepository extends JpaRepository<CaTrucKhoa, Long> {
    List<CaTrucKhoa> findByKhoaId(Long khoaId);

    Optional<CaTrucKhoa> findByKhoaIdAndNgayTrongTuanIdAndCaLamViecId(Long khoaId, Long ngayTrongTuanId, Long caLamViecId);

    @Query("SELECT c FROM CaTrucKhoa c WHERE c.khoa.id = :khoaId AND c.ngayTrongTuan.tuanLamViec.id = :tuanId " +
           "ORDER BY c.ngayTrongTuan.ngay, c.caLamViec.gioBatDau")
    List<CaTrucKhoa> timTheoKhoaTuan(@Param("khoaId") Long khoaId, @Param("tuanId") Long tuanId);

    @Query("SELECT c FROM CaTrucKhoa c WHERE c.ngayTrongTuan.tuanLamViec.id = :tuanId")
    List<CaTrucKhoa> timTheoTuan(@Param("tuanId") Long tuanId);
}
