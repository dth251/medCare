package com.medcare.clinic.repository;

import com.medcare.clinic.entity.PhanCongCa;
import com.medcare.clinic.enums.VaiTroPhanCong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhanCongCaRepository extends JpaRepository<PhanCongCa, Long> {
    List<PhanCongCa> findByBangPhanCongId(Long bangPhanCongId);
    List<PhanCongCa> findByCaTrucKhoaId(Long caTrucKhoaId);
    Optional<PhanCongCa> findByCaTrucKhoaIdAndVaiTro(Long caTrucKhoaId, VaiTroPhanCong vaiTro);
    List<PhanCongCa> findByNhanSuYTeId(Long nhanSuId);
    List<PhanCongCa> findByNhanSuYTeIdAndBangPhanCong_TuanLamViec_Id(Long nhanSuId, Long tuanId);
    List<PhanCongCa> findByBangPhanCong_TuanLamViec_Id(Long tuanId);
    /** Số ca đã được phân công cho một nhân sự trong một ngày (dùng cho giới hạn ca/ngày). */
    long countByNhanSuYTeIdAndCaTrucKhoa_NgayTrongTuan_Id(Long nhanSuId, Long ngayTrongTuanId);
}
