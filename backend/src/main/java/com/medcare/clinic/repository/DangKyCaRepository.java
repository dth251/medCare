package com.medcare.clinic.repository;

import com.medcare.clinic.entity.DangKyCa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DangKyCaRepository extends JpaRepository<DangKyCa, Long> {
    List<DangKyCa> findByPhieuDangKyId(Long phieuDangKyId);

    @Query("SELECT d FROM DangKyCa d WHERE d.caTrucKhoa.id = :caTrucKhoaId")
    List<DangKyCa> findByCaTrucKhoaId(@Param("caTrucKhoaId") Long caTrucKhoaId);

    @Query("SELECT d.phieuDangKy.nhanSuYTe.id FROM DangKyCa d WHERE d.caTrucKhoa.id = :caTrucKhoaId")
    List<Long> findNhanSuIdsDangKy(@Param("caTrucKhoaId") Long caTrucKhoaId);

    @Query("SELECT COUNT(d) > 0 FROM DangKyCa d WHERE d.caTrucKhoa.id = :caTrucKhoaId AND d.phieuDangKy.nhanSuYTe.id = :nhanSuId")
    boolean daDangKy(@Param("nhanSuId") Long nhanSuId, @Param("caTrucKhoaId") Long caTrucKhoaId);
}
