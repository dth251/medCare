package com.medcare.clinic.mapper;

import com.medcare.clinic.dto.dangky.DangKyDtos;
import com.medcare.clinic.dto.tuan.TuanDTO;
import com.medcare.clinic.entity.CaLamViec;
import com.medcare.clinic.entity.CaTrucKhoa;
import com.medcare.clinic.entity.NgayTrongTuan;
import com.medcare.clinic.entity.TuanLamViec;
import org.springframework.stereotype.Component;

@Component
public class TuanMapper {

    public TuanDTO toResponse(TuanLamViec t) {
        if (t == null) return null;
        return new TuanDTO(
                t.getId(),
                t.getTen(),
                t.getNgayBatDau(),
                t.getNgayKetThuc(),
                t.getThoiDiemMoDangKy(),
                t.getHanDangKy(),
                t.getTrangThai() != null ? t.getTrangThai().name() : null,
                t.getSoCaToiThieu(),
                t.getSoCaToiDa(),
                t.getSoCaToiDaMoiNgay()
        );
    }

    public DangKyDtos.CaTruc toCaTruc(CaTrucKhoa ca) {
        if (ca == null) return null;
        NgayTrongTuan ngay = ca.getNgayTrongTuan();
        CaLamViec clv = ca.getCaLamViec();
        return new DangKyDtos.CaTruc(
                ca.getId(),
                ngay != null ? ngay.getId() : null,
                ngay != null ? ngay.getNgay() : null,
                (ngay != null && ngay.getThu() != null) ? ngay.getThu().name() : null,
                clv != null ? clv.getId() : null,
                clv != null ? clv.getTenCa() : null,
                (clv != null && clv.getLoaiCa() != null) ? clv.getLoaiCa().name() : null,
                clv != null ? clv.getGioBatDau() : null,
                clv != null ? clv.getGioKetThuc() : null,
                clv != null ? clv.getHeSoLuong() : null
        );
    }
}

