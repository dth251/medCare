package com.medcare.clinic.mapper;

import com.medcare.clinic.dto.dangky.DangKyDtos;
import com.medcare.clinic.entity.CaLamViec;
import com.medcare.clinic.entity.CaTrucKhoa;
import com.medcare.clinic.entity.DangKyCa;
import com.medcare.clinic.entity.NgayTrongTuan;
import com.medcare.clinic.entity.PhieuDangKy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DangKyMapper {

    public DangKyDtos.PhieuDangKy toResponse(PhieuDangKy phieu, List<DangKyCa> caList) {
        if (phieu == null) return null;
        List<Long> ids = caList != null
                ? caList.stream().map(dk -> dk.getCaTrucKhoa().getId()).toList()
                : List.of();
        return new DangKyDtos.PhieuDangKy(
                phieu.getId(),
                phieu.getMaPhieu(),
                phieu.getTuanLamViec().getId(),
                phieu.getTrangThai() != null ? phieu.getTrangThai().name() : null,
                phieu.getNgayDangKy(),
                phieu.getTongSoCa(),
                ids
        );
    }

    public DangKyDtos.CaTruc toCaTruc(CaTrucKhoa ca) {
        if (ca == null) return null;
        NgayTrongTuan n = ca.getNgayTrongTuan();
        CaLamViec c = ca.getCaLamViec();
        return new DangKyDtos.CaTruc(
                ca.getId(),
                n != null ? n.getId() : null,
                n != null ? n.getNgay() : null,
                n != null && n.getThu() != null ? n.getThu().name() : null,
                c != null ? c.getId() : null,
                c != null ? c.getTenCa() : null,
                c != null && c.getLoaiCa() != null ? c.getLoaiCa().name() : null,
                c != null ? c.getGioBatDau() : null,
                c != null ? c.getGioKetThuc() : null,
                c != null ? c.getHeSoLuong() : null
        );
    }
}
