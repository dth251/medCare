package com.medcare.clinic.mapper;

import com.medcare.clinic.dto.thanhtoan.ChamCongDTO;
import com.medcare.clinic.entity.*;
import com.medcare.clinic.enums.TrangThaiChamCong;
import com.medcare.clinic.service.HoSoMapper;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ChamCongMapper {

    private static final Set<TrangThaiChamCong> KHONG_TINH_CONG = Set.of(
            TrangThaiChamCong.VANG_MAT,
            TrangThaiChamCong.LOI_THIET_BI
    );

    public ChamCongDTO toDto(BangPhanCong bpc, ChamCong cc, YeuCauKhieuNai kn) {
        if (bpc == null) return null;
        CaTrucKhoa ca = bpc.getCaTrucKhoa();
        CaLamViec clv = ca != null ? ca.getCaLamViec() : null;
        NhanSuYTe ns = bpc.getNhanSuYTe();

        boolean khongTinh = cc != null && KHONG_TINH_CONG.contains(cc.getTrangThai());
        boolean thieuQuet = cc != null && (cc.getGioCheckIn() == null || cc.getGioCheckOut() == null);

        return ChamCongDTO.builder()
                .id(cc != null ? cc.getId() : null)
                .bangPhanCongId(bpc.getId())
                .nhanSuId(ns != null ? ns.getId() : null)
                .tenNhanSu(ns != null ? ns.getHoVaTen() : "")
                .ngay((ca != null && ca.getNgayTrongTuan() != null && ca.getNgayTrongTuan().getNgay() != null)
                        ? ca.getNgayTrongTuan().getNgay().toString() : "")
                .thu((ca != null && ca.getNgayTrongTuan() != null && ca.getNgayTrongTuan().getThu() != null)
                        ? ca.getNgayTrongTuan().getThu().name() : "")
                .tenCa(clv != null ? clv.getTenCa() : "")
                .thoiGian(clv != null ? HoSoMapper.khungGio(clv) : "")
                .loaiCa((clv != null && clv.getLoaiCa() != null) ? clv.getLoaiCa().name() : "")
                .heSo((clv != null && clv.getHeSoLuong() != null) ? clv.getHeSoLuong().doubleValue() : 1.0)
                .gioCheckIn(cc != null && cc.getGioCheckIn() != null ? cc.getGioCheckIn().toLocalTime().toString().substring(0, 5) : "--:--")
                .gioCheckOut(cc != null && cc.getGioCheckOut() != null ? cc.getGioCheckOut().toLocalTime().toString().substring(0, 5) : "--:--")
                .trangThai(cc != null && cc.getTrangThai() != null ? cc.getTrangThai().name() : "CHUA_CO_DU_LIEU")
                .ghiChu(cc != null ? cc.getGhiChu() : null)
                .duocTinhCong(!khongTinh)
                .thieuQuetThe(thieuQuet)
                .khieuNaiId(kn != null ? kn.getId() : null)
                .maDonKhieuNai(kn != null ? kn.getMaYeuCau() : null)
                .trangThaiKhieuNai(kn != null ? kn.getTrangThai() : null)
                .build();
    }
}
