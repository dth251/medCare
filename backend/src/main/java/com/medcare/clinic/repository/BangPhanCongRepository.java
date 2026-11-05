package com.medcare.clinic.repository;

import com.medcare.clinic.entity.BangPhanCong;
import com.medcare.clinic.enums.VaiTroPhanCong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BangPhanCongRepository extends JpaRepository<BangPhanCong, Long> {

    List<BangPhanCong> findByKhoaIdAndTuanLamViecId(Long khoaId, Long tuanLamViecId);

    List<BangPhanCong> findByTuanLamViecId(Long tuanLamViecId);

    List<BangPhanCong> findByCaTrucKhoaId(Long caTrucKhoaId);

    Optional<BangPhanCong> findByCaTrucKhoaIdAndVaiTro(Long caTrucKhoaId, VaiTroPhanCong vaiTro);

    List<BangPhanCong> findByNhanSuYTeId(Long nhanSuId);

    List<BangPhanCong> findByNhanSuYTeIdAndTuanLamViecId(Long nhanSuId, Long tuanId);

    /** Số ca đã được phân công cho một nhân sự trong một ngày (dùng cho giới hạn ca/ngày). */
    long countByNhanSuYTeIdAndCaTrucKhoa_NgayTrongTuan_Id(Long nhanSuId, Long ngayTrongTuanId);

    boolean existsByCaTrucKhoaIdAndNhanSuYTeId(Long caTrucKhoaId, Long nhanSuId);

    boolean existsByCaTrucKhoaIdAndVaiTro(Long caTrucKhoaId, VaiTroPhanCong vaiTro);

    void deleteByCaTrucKhoaIdAndVaiTro(Long caTrucKhoaId, VaiTroPhanCong vaiTro);
}
