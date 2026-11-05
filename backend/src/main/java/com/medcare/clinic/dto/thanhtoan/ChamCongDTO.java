package com.medcare.clinic.dto.thanhtoan;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

/** Một dòng trong bảng chấm công chi tiết (một ca đã được phân công). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChamCongDTO {
    private Long id;
    
    @JsonAlias({"phanCongCaId", "bangPhanCongId"})
    private Long bangPhanCongId;

    public Long getPhanCongCaId() {
        return bangPhanCongId;
    }

    public void setPhanCongCaId(Long phanCongCaId) {
        this.bangPhanCongId = phanCongCaId;
    }

    private Long nhanSuId;
    private String tenNhanSu;
    private String ngay;
    private String thu;
    private String tenCa;
    private String thoiGian;
    private String loaiCa;
    private Double heSo;
    private String gioCheckIn;
    private String gioCheckOut;
    /** DUNG_GIO, DI_MUON, VE_SOM, VANG_MAT, LOI_THIET_BI hoặc CHUA_CO_DU_LIEU */
    private String trangThai;
    private String ghiChu;
    /** Ca này có được tính vào thù lao hay không */
    private boolean duocTinhCong;
    private boolean thieuQuetThe;
    private Long khieuNaiId;
    private String maDonKhieuNai;
    private String trangThaiKhieuNai;
}
