package com.medcare.clinic.mapper;

import com.medcare.clinic.dto.phancong.PhanCongDtos;
import com.medcare.clinic.entity.BangPhanCong;
import com.medcare.clinic.entity.NhanSuYTe;
import com.medcare.clinic.service.HoSoMapper;
import org.springframework.stereotype.Component;

@Component
public class BangPhanCongMapper {

    public PhanCongDtos.Slot toSlot(BangPhanCong bpc) {
        if (bpc == null || bpc.getNhanSuYTe() == null) return null;
        NhanSuYTe ns = bpc.getNhanSuYTe();
        return new PhanCongDtos.Slot(
                bpc.getId(),
                ns.getId(),
                HoSoMapper.maNhanSu(ns),
                ns.getHoVaTen(),
                bpc.getVaiTro() != null ? bpc.getVaiTro().name() : null,
                bpc.isDieuDongNgoaiDangKy(),
                bpc.getGhiChu()
        );
    }

    public PhanCongDtos.UngVien toUngVien(NhanSuYTe ns, boolean cungKhoa, boolean daDangKy, long soCaTrongNgay, int soCaToiDaMoiNgay, boolean daCoTrongCa) {
        if (ns == null) return null;
        return new PhanCongDtos.UngVien(
                ns.getId(),
                HoSoMapper.maNhanSu(ns),
                ns.getHoVaTen(),
                HoSoMapper.chucDanh(ns),
                HoSoMapper.hocVi(ns),
                ns.getKhoa() != null ? ns.getKhoa().getId() : null,
                ns.getKhoa() != null ? ns.getKhoa().getTen() : null,
                cungKhoa,
                daDangKy,
                soCaTrongNgay,
                soCaToiDaMoiNgay,
                daCoTrongCa
        );
    }
}
