package com.medcare.clinic.controller;

import com.medcare.clinic.dto.common.ApiResponse;
import com.medcare.clinic.dto.dangky.DangKyCaRequest;
import com.medcare.clinic.dto.dangky.DangKyDtos;
import com.medcare.clinic.service.DangKyCaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dang-ky-ca")
@RequiredArgsConstructor
public class DangKyCaController {

    private final DangKyCaService dangKyCaService;

    @PostMapping("/luu")
    public ResponseEntity<ApiResponse<DangKyDtos.PhieuDangKy>> luuDangKy(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DangKyCaRequest request) {
        var phieu = dangKyCaService.luuDangKy(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Lưu đăng ký " + phieu.tongSoCa() + " ca trực thành công", phieu));
    }

    /** Phiếu đăng ký của tôi trong tuần (data = null nếu chưa đăng ký). */
    @GetMapping("/lich-tuan/{tuanId}")
    public ResponseEntity<ApiResponse<DangKyDtos.PhieuDangKy>> getMyRegistration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tuanId) {
        var phieu = dangKyCaService.getMyRegistration(userDetails.getUsername(), tuanId).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin đăng ký ca trực thành công", phieu));
    }

    /** Các ca trực của khoa tôi trong tuần – dùng để dựng ma trận đăng ký. */
    @GetMapping("/ca-truc/{tuanId}")
    public ResponseEntity<ApiResponse<List<DangKyDtos.CaTruc>>> danhSachCaTruc(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tuanId) {
        return ResponseEntity.ok(ApiResponse.ok(dangKyCaService.danhSachCaTruc(userDetails.getUsername(), tuanId)));
    }
}
