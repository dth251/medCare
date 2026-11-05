package com.medcare.clinic.mapper;

import com.medcare.clinic.dto.thanhtoan.ThanhToanDtos;
import com.medcare.clinic.entity.NhanSuYTe;
import com.medcare.clinic.entity.PhieuThanhToan;
import com.medcare.clinic.entity.TuanLamViec;
import com.medcare.clinic.service.HoSoMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ThanhToanMapper {

    private final KhieuNaiMapper khieuNaiMapper;

    public ThanhToanMapper(KhieuNaiMapper khieuNaiMapper) {
        this.khieuNaiMapper = khieuNaiMapper;
    }

    public ThanhToanDtos.PhieuLuong toPhieuLuong(PhieuThanhToan p) {
        if (p == null) return null;
        NhanSuYTe ns = p.getNhanSuYTe();
        TuanLamViec tuan = p.getTuanLamViec();

        BigDecimal tienChinh = p.getDonGiaCaApDung()
                .multiply(BigDecimal.valueOf(p.getSoCaChinhGoc()));
        BigDecimal tienOT = p.getDonGiaCaApDung()
                .multiply(p.getHeSoNgoaiGioApDung())
                .multiply(BigDecimal.valueOf(p.getSoCaNgoaiGioGoc()));

        List<ThanhToanDtos.DieuChinh> dcList = new ArrayList<>();
        if (p.getYeuCauKhieuNai() != null && "DA_XU_LY".equals(p.getYeuCauKhieuNai().getTrangThai())) {
            dcList.add(khieuNaiMapper.toDieuChinh(p.getYeuCauKhieuNai()));
        }

        return new ThanhToanDtos.PhieuLuong(
                p.getId(),
                p.getMaPhieu(),
                p.getTrangThai() != null ? p.getTrangThai().name() : null,
                p.getNgayLap(),
                p.getNgayThanhToan(),
                p.getHinhThucThanhToan() != null ? p.getHinhThucThanhToan().name() : null,
                p.getMaGiaoDich(),
                tuan != null ? tuan.getId() : null,
                tuan != null ? tuan.getTen() : null,
                tuan != null ? tuan.getNgayBatDau() : null,
                tuan != null ? tuan.getNgayKetThuc() : null,
                ns != null ? ns.getId() : null,
                ns != null ? HoSoMapper.maNhanSu(ns) : null,
                ns != null ? ns.getHoVaTen() : null,
                ns != null ? HoSoMapper.chucDanh(ns) : null,
                ns != null ? HoSoMapper.hocVi(ns) : null,
                (ns != null && ns.getKhoa() != null) ? ns.getKhoa().getTen() : null,
                p.getSoTaiKhoanNhan(),
                p.getNganHangNhan(),
                p.getChuTaiKhoanNhan(),
                p.getSoCaChinhGoc(),
                p.getSoCaNgoaiGioGoc(),
                p.getDonGiaCaApDung(),
                p.getHeSoNgoaiGioApDung(),
                tienChinh,
                tienOT,
                p.getPhuCapTrachNhiem(),
                p.getThuong(),
                p.getPhat(),
                dcList,
                p.getTongThucLinh(),
                p.getKeToan() != null ? p.getKeToan().getHoVaTen() : null
        );
    }
}
