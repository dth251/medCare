package com.medcare.clinic.dto.phancong;

import com.medcare.clinic.dto.dangky.DangKyDtos;
import java.time.LocalDateTime;
import java.util.List;

public final class PhanCongDtos {
    private PhanCongDtos() {}

    /** Tiến độ xếp lịch của một khoa trong tuần. */
    public record KhoaTienDo(
            Long khoaId,
            String maKhoa,
            String tenKhoa,
            long soBacSi,
            long soYTa,
            int tongSoCa,
            int soCaDaDu,
            String trangThaiBang,
            LocalDateTime ngayCongBo
    ) {}

    /** Một vị trí đã được gán trong ca. */
    public record Slot(
            Long phanCongId,
            Long nhanSuId,
            String maNhanSu,
            String hoTen,
            String vaiTro,
            boolean dieuDongNgoaiDangKy,
            String ghiChu
    ) {}

    public record CaPhanCong(
            DangKyDtos.CaTruc ca,
            Slot bacSiChinh,
            Slot yTa1,
            Slot yTa2
    ) {}

    public record LichKhoa(
            Long khoaId,
            String maKhoa,
            String tenKhoa,
            Long tuanId,
            String tenTuan,
            String trangThaiBang,
            int tongSoCa,
            int soCaDaDu,
            List<CaPhanCong> danhSachCa
    ) {}

    /** Ứng viên có thể gán cho một vị trí trong ca. */
    public record UngVien(
            Long nhanSuId,
            String maNhanSu,
            String hoTen,
            String chucDanh,
            String hocVi,
            Long khoaId,
            String tenKhoa,
            boolean cungKhoa,
            boolean daDangKy,
            long soCaTrongNgay,
            int soCaToiDaMoiNgay,
            boolean daCoTrongCa
    ) {}

    public record CongBoKetQua(
            Long tuanId,
            int soKhoa,
            int tongSoCa,
            LocalDateTime ngayCongBo
    ) {}
}
