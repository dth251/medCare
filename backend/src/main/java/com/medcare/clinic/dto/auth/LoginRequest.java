package com.medcare.clinic.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @JsonAlias({"tenDangNhap", "username"})
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @JsonAlias({"matKhau", "password"})
    private String password;
}
