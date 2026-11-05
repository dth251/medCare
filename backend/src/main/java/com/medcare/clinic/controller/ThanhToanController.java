package com.medcare.clinic.controller;

import com.medcare.clinic.dto.common.ApiResponse;
import com.medcare.clinic.dto.thanhtoan.ChamCongDTO;
import com.medcare.clinic.dto.thanhtoan.ThanhToanDtos;
import com.medcare.clinic.dto.thanhtoan.ThanhToanRequest;
import com.medcare.clinic.service.ThanhToanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/thanh-toan")
@RequiredArgsConstructor
public class ThanhToanController {

    private final ThanhToanService thanhToanService;

    // ----- Kế toán -----

    @PostMapping("/chi-tra")
    public ResponseEntity<ApiResponse<ThanhToanDtos.PhieuLuong>> chiTra(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ThanhToanRequest request) {
        var phieu = thanhToanService.thanhToan(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Quyết toán và phát hành phiếu lương thành công", phieu));
    }

    @GetMapping("/tuan/{tuanId}")
    public ResponseEntity<ApiResponse<List<ThanhToanDtos.PhieuLuong>>> layDanhSachPhieuTuan(@PathVariable Long tuanId) {
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách phiếu thanh toán tuần thành công",
                thanhToanService.layDanhSachPhieuTuan(tuanId)));
    }

    @GetMapping("/tuan/{tuanId}/khoa")
    public ResponseEntity<ApiResponse<List<ThanhToanDtos.KhoaThanhToan>>> danhSachKhoa(@PathVariable Long tuanId) {
        return ResponseEntity.ok(ApiResponse.ok(thanhToanService.danhSachKhoa(tuanId)));
    }

    @GetMapping("/tuan/{tuanId}/khoa/{khoaId}/nhan-su")
    public ResponseEntity<ApiResponse<List<ThanhToanDtos.NhanSuLuong>>> danhSachNhanSu(
            @PathVariable Long tuanId, @PathVariable Long khoaId) {
        return ResponseEntity.ok(ApiResponse.ok(thanhToanService.danhSachNhanSu(tuanId, khoaId)));
    }

    // ----- Nhân sự & Kế toán (kiểm tra quyền sở hữu trong service) -----

    @GetMapping("/phieu/{phieuId}")
    public ResponseEntity<ApiResponse<ThanhToanDtos.PhieuLuong>> chiTietPhieu(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long phieuId) {
        return ResponseEntity.ok(ApiResponse.ok(
                thanhToanService.getPhieu(phieuId, userDetails.getUsername(), Roles.laKeToan(userDetails))));
    }

    @GetMapping("/cham-cong/{nhanSuId}/tuan/{tuanId}")
    public ResponseEntity<ApiResponse<List<ChamCongDTO>>> layBangChamCong(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long nhanSuId,
            @PathVariable Long tuanId) {
        var list = thanhToanService.layBangChamCong(userDetails.getUsername(), Roles.laKeToan(userDetails), nhanSuId, tuanId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy bảng chấm công chi tiết thành công", list));
    }

    // ----- Nhân sự -----

    @GetMapping("/phieu-cua-toi/tat-ca")
    public ResponseEntity<ApiResponse<List<ThanhToanDtos.PhieuLuong>>> getTatCaPhieuCuaToi(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok("Lấy lịch sử phiếu lương cá nhân thành công",
                thanhToanService.getTatCaPhieuCuaToi(userDetails.getUsername())));
    }

    @GetMapping("/phieu-cua-toi/{tuanId}")
    public ResponseEntity<ApiResponse<ThanhToanDtos.PhieuLuong>> getPhieuCuaToi(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tuanId) {
        return ResponseEntity.ok(ApiResponse.ok("Lấy phiếu lương cá nhân thành công",
                thanhToanService.getPhieuCuaToi(userDetails.getUsername(), tuanId)));
    }
}
