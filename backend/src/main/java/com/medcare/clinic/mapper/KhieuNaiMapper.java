package com.medcare.clinic.mapper;

import com.medcare.clinic.dto.thanhtoan.ThanhToanDtos;
import com.medcare.clinic.entity.*;
import com.medcare.clinic.service.HoSoMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class KhieuNaiMapper {

    public ThanhToanDtos.KhieuNai toDto(YeuCauKhieuNai k) {
        if (k == null) return null;
        NhanSuYTe ns = k.getNhanSu();
        BangPhanCong bpc = k.getBangPhanCong();
        ChamCong cc = k.getChamCong();
        CaTrucKhoa ca = bpc != null ? bpc.getCaTrucKhoa() : null;
        CaLamViec clv = ca != null ? ca.getCaLamViec() : null;
        TuanLamViec tuan = bpc != null ? bpc.getTuanLamViec() : null;

        ThanhToanDtos.DieuChinh dc = null;
        if ("DA_XU_LY".equals(k.getTrangThai())) {
            dc = toDieuChinh(k);
        }

        LocalDate ngay = (ca != null && ca.getNgayTrongTuan() != null) ? ca.getNgayTrongTuan().getNgay() : null;
        String thu = (ca != null && ca.getNgayTrongTuan() != null && ca.getNgayTrongTuan().getThu() != null)
                ? ca.getNgayTrongTuan().getThu().name() : null;

        return new ThanhToanDtos.KhieuNai(
                k.getId(),
                k.getMaYeuCau(),
                k.getTrangThai(),
                k.getNgayGui(),
                k.getNgayXuLy(),
                k.getNoiDung(),
                k.getDsMinhChung(),
                k.getLyDoPhanHoi(),
                ns != null ? ns.getId() : null,
                ns != null ? HoSoMapper.maNhanSu(ns) : null,
                ns != null ? ns.getHoVaTen() : null,
                (ns != null && ns.getKhoa() != null) ? ns.getKhoa().getTen() : null,
                tuan != null ? tuan.getId() : null,
                tuan != null ? tuan.getTen() : null,
                bpc != null ? bpc.getId() : null,
                bpc != null && bpc.getVaiTro() != null ? bpc.getVaiTro().name() : null,
                bpc != null && bpc.isDieuDongNgoaiDangKy(),
                ngay,
                thu,
                clv != null ? clv.getTenCa() : null,
                clv != null ? HoSoMapper.khungGio(clv) : null,
                clv != null && clv.getLoaiCa() != null ? clv.getLoaiCa().name() : null,
                clv != null ? clv.getHeSoLuong() : BigDecimal.ONE,
                cc != null && cc.getTrangThai() != null ? cc.getTrangThai().name() : null,
                cc != null && cc.getGioCheckIn() != null ? cc.getGioCheckIn().toLocalTime().toString().substring(0, 5) : "--:--",
                cc != null && cc.getGioCheckOut() != null ? cc.getGioCheckOut().toLocalTime().toString().substring(0, 5) : "--:--",
                cc != null ? cc.getGhiChu() : null,
                dc,
                k.getNguoiXuLy() != null ? k.getNguoiXuLy().getHoVaTen() : null
        );
    }

    public ThanhToanDtos.DieuChinh toDieuChinh(YeuCauKhieuNai k) {
        if (k == null) return null;
        return new ThanhToanDtos.DieuChinh(
                k.getId(),
                k.getMaYeuCau(),
                k.getSoCaChinhDieuChinh(),
                k.getSoCaNgoaiGioDieuChinh(),
                k.getPhuCapBoSung(),
                k.getKhauTru(),
                k.getSoTienDieuChinh(),
                k.getLyDo()
        );
    }
}

