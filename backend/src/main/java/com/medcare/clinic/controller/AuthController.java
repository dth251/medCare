package com.medcare.clinic.controller;

import com.medcare.clinic.dto.auth.JwtResponse;
import com.medcare.clinic.dto.auth.LoginRequest;
import com.medcare.clinic.dto.auth.MeResponse;
import com.medcare.clinic.dto.auth.UpdateProfileRequest;
import com.medcare.clinic.dto.common.ApiResponse;
import com.medcare.clinic.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping({"/login", "/dang-nhap"})
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", response));
    }

    /** Hồ sơ người dùng hiện tại – frontend dùng để khôi phục phiên và phân quyền màn hình. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal UserDetails userDetails) {
        var roles = layDanhSachRoles(userDetails);
        return ResponseEntity.ok(ApiResponse.ok(authService.me(userDetails.getUsername(), roles)));
    }

    /** Cập nhật thông tin cá nhân của người dùng và lưu lại vào CSDL. */
    @PutMapping({"/profile", "/me"})
    public ResponseEntity<ApiResponse<MeResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        var roles = layDanhSachRoles(userDetails);
        MeResponse updated = authService.updateProfile(userDetails.getUsername(), roles, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin cá nhân thành công", updated));
    }

    private List<String> layDanhSachRoles(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
