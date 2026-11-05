package com.medcare.clinic.controller;

import com.medcare.clinic.dto.common.ApiResponse;
import com.medcare.clinic.dto.thanhtoan.DieuChinhRequest;
import com.medcare.clinic.dto.thanhtoan.KhieuNaiRequest;
import com.medcare.clinic.dto.thanhtoan.ThanhToanDtos;
import com.medcare.clinic.dto.thanhtoan.TuChoiRequest;
import com.medcare.clinic.service.KhieuNaiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/khieu-nai")
@RequiredArgsConstructor
public class KhieuNaiController {

    private final KhieuNaiService khieuNaiService;

    // ----- Nhân sự -----

    @PostMapping("/gui")
    public ResponseEntity<ApiResponse<ThanhToanDtos.KhieuNai>> guiKhieuNai(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody KhieuNaiRequest request) {
        var don = khieuNaiService.taoKhieuNai(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Gửi yêu cầu khiếu nại " + don.maYeuCau() + " thành công", don));
    }

    @GetMapping("/cua-toi")
    public ResponseEntity<ApiResponse<List<ThanhToanDtos.KhieuNai>>> cuaToi(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(khieuNaiService.danhSachCuaToi(userDetails.getUsername())));
    }

    // ----- Kế toán -----

    @GetMapping
    public ResponseEntity<ApiResponse<List<ThanhToanDtos.KhieuNai>>> danhSach(@RequestParam(required = false) String trangThai) {
        return ResponseEntity.ok(ApiResponse.ok(khieuNaiService.danhSach(trangThai)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ThanhToanDtos.KhieuNai>> chiTiet(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(khieuNaiService.chiTiet(id)));
    }

    @PostMapping("/dieu-chinh")
    public ResponseEntity<ApiResponse<ThanhToanDtos.KhieuNai>> lapBienBanDieuChinh(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DieuChinhRequest request) {
        var kn = khieuNaiService.lapBienBanDieuChinh(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Lập biên bản điều chỉnh thù lao thành công", kn));
    }

    @PostMapping("/{id}/tu-choi")
    public ResponseEntity<ApiResponse<ThanhToanDtos.KhieuNai>> tuChoi(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody TuChoiRequest request) {
        var kn = khieuNaiService.tuChoi(userDetails.getUsername(), id, request.lyDo());
        return ResponseEntity.ok(ApiResponse.ok("Đã từ chối khiếu nại " + kn.maYeuCau(), kn));
    }
}
