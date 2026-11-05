package com.medcare.clinic.service;

import com.medcare.clinic.dto.dangky.DangKyDtos;
import com.medcare.clinic.dto.tuan.TuanDTO;
import com.medcare.clinic.entity.*;
import com.medcare.clinic.enums.TrinhDoHocVi;
import org.hibernate.Hibernate;

import java.time.LocalTime;

/** Các hàm ánh xạ entity -> thông tin hiển thị dùng chung giữa các service. */
public final class HoSoMapper {

    private HoSoMapper() {}

    /**
     * Bỏ lớp Hibernate proxy để kiểm tra kiểu con (BacSi / YTa / NhanVien).
     * Entity nạp lazy là proxy của lớp cha nên "instanceof BacSi" sẽ sai nếu không unproxy.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ThanhVien> T thuc(T tv) {
        return tv == null ? null : (T) Hibernate.unproxy(tv);
    }

    public static boolean laBacSi(ThanhVien tv) {
        return thuc(tv) instanceof BacSi;
    }

    public static boolean laYTa(ThanhVien tv) {
        return thuc(tv) instanceof YTa;
    }

    public static String maNhanSu(ThanhVien tv) {
        tv = thuc(tv);
        if (tv instanceof BacSi bs && bs.getMaBacSi() != null) return bs.getMaBacSi();
        if (tv instanceof YTa yt && yt.getMaYTa() != null) return yt.getMaYTa();
        if (tv instanceof NhanVien nv && nv.getViTriCongViec() != null) {
            return (nv.getViTriCongViec().name().equals("QUAN_LY") ? "QL" : "KT") + String.format("%03d", nv.getId());
        }
        return "NS" + String.format("%03d", tv.getId());
    }

    public static String chucDanh(ThanhVien tv) {
        tv = thuc(tv);
        if (tv instanceof BacSi) return "Bác sĩ";
        if (tv instanceof YTa) return "Y tá";
        if (tv instanceof NhanVien nv && nv.getViTriCongViec() != null) {
            return switch (nv.getViTriCongViec()) {
                case QUAN_LY -> "Quản lý";
                case KE_TOAN -> "Kế toán";
            };
        }
        return "Nhân sự y tế";
    }

    public static String hocVi(ThanhVien tv) {
        tv = thuc(tv);
        if (tv instanceof BacSi bs) return hocVi(bs.getHocVi());
        if (tv instanceof YTa yt) return hocVi(yt.getTrinhDo());
        return null;
    }

    public static String hocVi(TrinhDoHocVi t) {
        if (t == null) return null;
        return switch (t) {
            case TIEN_SI -> "Tiến sĩ";
            case THAC_SI -> "Thạc sĩ";
            case CHUYEN_KHOA_I -> "Chuyên khoa I";
            case CHUYEN_KHOA_II -> "Chuyên khoa II";
            case DAI_HOC -> "Đại học";
            case CU_NHAN -> "Cử nhân";
            case CAO_DANG -> "Cao đẳng";
        };
    }

    public static DangKyDtos.CaTruc caTruc(CaTrucKhoa ca) {
        NgayTrongTuan ngay = ca.getNgayTrongTuan();
        CaLamViec clv = ca.getCaLamViec();
        return new DangKyDtos.CaTruc(
                ca.getId(),
                ngay.getId(),
                ngay.getNgay(),
                ngay.getThu() != null ? ngay.getThu().name() : null,
                clv.getId(),
                clv.getTenCa(),
                clv.getLoaiCa() != null ? clv.getLoaiCa().name() : null,
                clv.getGioBatDau(),
                clv.getGioKetThuc(),
                clv.getHeSoLuong());
    }

    public static String khungGio(CaLamViec clv) {
        return hhmm(clv.getGioBatDau()) + " - " + hhmm(clv.getGioKetThuc());
    }

    public static String hhmm(LocalTime t) {
        return t == null ? "--:--" : String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    public static TuanDTO tuan(TuanLamViec t) {
        return new TuanDTO(t.getId(), t.getTen(), t.getNgayBatDau(), t.getNgayKetThuc(),
                t.getThoiDiemMoDangKy(), t.getHanDangKy(),
                t.getTrangThai() != null ? t.getTrangThai().name() : null,
                t.getSoCaToiThieu(), t.getSoCaToiDa(), t.getSoCaToiDaMoiNgay());
    }
}
