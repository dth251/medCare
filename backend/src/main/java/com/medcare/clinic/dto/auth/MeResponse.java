package com.medcare.clinic.dto.auth;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Hồ sơ người dùng đang đăng nhập (không bao giờ chứa mật khẩu). */
public record MeResponse(
        Long id,
        String username,
        String fullName,
        List<String> roles,
        String vaiTro,          // NHAN_SU | QUAN_LY | KE_TOAN
        String maNhanSu,
        String chucDanh,        // Bác sĩ | Y tá | Quản lý | Kế toán
        String hocVi,
        Long khoaId,
        String maKhoa,
        String tenKhoa,
        BigDecimal donGiaCa,
        String soTaiKhoan,
        String tenNganHang,
        String chuTaiKhoan,
        String email,
        String soDienThoai,
        LocalDate ngaySinh,
        String diaChi
) {}
