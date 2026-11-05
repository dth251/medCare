package com.medcare.clinic.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Yêu cầu cập nhật thông tin cá nhân của người dùng hiện tại. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 150, message = "Họ và tên không được vượt quá 150 ký tự")
    private String fullName;

    @Size(max = 50, message = "Họ không được vượt quá 50 ký tự")
    private String ho;

    @Size(max = 50, message = "Tên đệm không được vượt quá 50 ký tự")
    private String tenDem;

    @Size(max = 50, message = "Tên không được vượt quá 50 ký tự")
    private String ten;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String soDienThoai;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    private LocalDate ngaySinh;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String diaChi;

    @Size(max = 50, message = "Số tài khoản không được vượt quá 50 ký tự")
    private String soTaiKhoan;

    @Size(max = 100, message = "Tên ngân hàng không được vượt quá 100 ký tự")
    private String tenNganHang;

    @Size(max = 100, message = "Chủ tài khoản không được vượt quá 100 ký tự")
    private String chuTaiKhoan;
}

