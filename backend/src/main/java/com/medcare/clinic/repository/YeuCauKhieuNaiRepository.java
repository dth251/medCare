package com.medcare.clinic.repository;

import com.medcare.clinic.entity.YeuCauKhieuNai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface YeuCauKhieuNaiRepository extends JpaRepository<YeuCauKhieuNai, Long> {

    @Query("SELECT y FROM YeuCauKhieuNai y WHERE y.nhanSu.id = :nhanSuId AND y.bangPhanCong.tuanLamViec.id = :tuanId")
    List<YeuCauKhieuNai> findTheoNhanSuTuan(@Param("nhanSuId") Long nhanSuId, @Param("tuanId") Long tuanId);

    @Query("SELECT y FROM YeuCauKhieuNai y WHERE y.bangPhanCong.tuanLamViec.id = :tuanId")
    List<YeuCauKhieuNai> findTheoTuan(@Param("tuanId") Long tuanId);

    Optional<YeuCauKhieuNai> findByMaYeuCau(String maYeuCau);

    List<YeuCauKhieuNai> findByNhanSuIdOrderByNgayGuiDesc(Long nhanSuId);

    List<YeuCauKhieuNai> findByTrangThaiInOrderByNgayGuiDesc(Collection<String> trangThai);

    List<YeuCauKhieuNai> findAllByOrderByNgayGuiDesc();

    List<YeuCauKhieuNai> findByPhanCongCaId(Long phanCongCaId);

    boolean existsByPhanCongCaIdAndTrangThai(Long phanCongCaId, String trangThai);
}
