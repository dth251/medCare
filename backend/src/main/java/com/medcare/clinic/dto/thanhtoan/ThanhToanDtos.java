package com.medcare.clinic.dto.thanhtoan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ThanhToanDtos {
    private ThanhToanDtos() {}

    /** Kết quả tính thù lao (dùng chung cho bảng lương tạm tính và phiếu lương). */
    public record BangTinhLuong(
            int soCaChinh,
            int soCaNgoaiGio,
            int soCaKhongTinh,
            BigDecimal donGiaCa,
            BigDecimal heSoNgoaiGio,
            BigDecimal tienCaChinh,
            BigDecimal tienCaNgoaiGio,
            BigDecimal tienDieuChinh,
            BigDecimal tongThucLinh,
            int soKhieuNaiChoXuLy
    ) {}

    public record KhoaThanhToan(
            Long khoaId,
            String maKhoa,
            String tenKhoa,
            int soNhanSu,
            int soDaThanhToan,
            BigDecimal tongTien
    ) {}

    public record NhanSuLuong(
            Long nhanSuId,
            String maNhanSu,
            String hoTen,
            String chucDanh,
            String hocVi,
            BangTinhLuong luong,
            String trangThai,           // DA_THANH_TOAN | CHUA_THANH_TOAN
            Long phieuId
    ) {}

    public record DieuChinh(
            Long khieuNaiId,
            String maYeuCau,
            int soCaChinh,
            int soCaNgoaiGio,
            BigDecimal phuCap,
            BigDecimal khauTru,
            BigDecimal soTien,
            String lyDo
    ) {}

    public record PhieuLuong(
            Long id,
            String maPhieu,
            String trangThai,
            LocalDateTime ngayLap,
            LocalDateTime ngayThanhToan,
            String hinhThucThanhToan,
            String maGiaoDich,
            Long tuanId,
            String tenTuan,
            LocalDate tuNgay,
            LocalDate denNgay,
            Long nhanSuId,
            String maNhanSu,
            String hoTen,
            String chucDanh,
            String hocVi,
            String tenKhoa,
            String soTaiKhoanNhan,
            String nganHangNhan,
            String chuTaiKhoanNhan,
            int soCaChinh,
            int soCaNgoaiGio,
            BigDecimal donGiaCa,
            BigDecimal heSoNgoaiGio,
            BigDecimal tienCaChinh,
            BigDecimal tienCaNgoaiGio,
            BigDecimal phuCapTrachNhiem,
            BigDecimal thuong,
            BigDecimal phat,
            List<DieuChinh> dieuChinh,
            BigDecimal tongThucLinh,
            String tenKeToan
    ) {}

    public record KhieuNai(
            Long id,
            String maYeuCau,
            String trangThai,
            LocalDateTime ngayGui,
            LocalDateTime ngayXuLy,
            String noiDung,
            String minhChung,
            String lyDoPhanHoi,
            Long nhanSuId,
            String maNhanSu,
            String hoTen,
            String tenKhoa,
            Long tuanId,
            String tenTuan,
            Long phanCongCaId,
            String vaiTro,
            boolean dieuDongNgoaiDangKy,
            LocalDate ngay,
            String thu,
            String tenCa,
            String thoiGian,
            String loaiCa,
            BigDecimal heSo,
            String trangThaiChamCong,
            String gioCheckIn,
            String gioCheckOut,
            String ghiChuChamCong,
            DieuChinh dieuChinh,
            String nguoiXuLy
    ) {}
}
