package com.medcare.clinic.controller;

import com.medcare.clinic.dto.common.ApiResponse;
import com.medcare.clinic.dto.phancong.PhanCongDtos;
import com.medcare.clinic.dto.phancong.PhanCongRequest;
import com.medcare.clinic.enums.VaiTroPhanCong;
import com.medcare.clinic.service.PhanCongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/phan-cong")
@RequiredArgsConstructor
public class PhanCongController {

    private final PhanCongService phanCongService;

    @PostMapping("/ca")
    public ResponseEntity<ApiResponse<PhanCongDtos.Slot>> phanCong(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PhanCongRequest request) {
        var slot = phanCongService.phanCong(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Đã phân công " + slot.hoTen() + " vào ca trực", slot));
    }

    @DeleteMapping("/ca/{phanCongId}")
    public ResponseEntity<ApiResponse<Void>> huyPhanCong(@PathVariable Long phanCongId) {
        phanCongService.huyPhanCong(phanCongId);
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy phân công khỏi vị trí ca trực", null));
    }

    @PostMapping("/cong-bo/{tuanId}")
    public ResponseEntity<ApiResponse<PhanCongDtos.CongBoKetQua>> congBoLich(@PathVariable Long tuanId) {
        var kq = phanCongService.congBoLichTuan(tuanId);
        return ResponseEntity.ok(ApiResponse.ok("Công bố lịch làm việc toàn viện thành công", kq));
    }

    /** Lưới phân ca chi tiết của một khoa (mọi người dùng đã đăng nhập được xem). */
    @GetMapping("/khoa/{khoaId}/tuan/{tuanId}")
    public ResponseEntity<ApiResponse<PhanCongDtos.LichKhoa>> layLichKhoa(
            @PathVariable Long khoaId, @PathVariable Long tuanId) {
        return ResponseEntity.ok(ApiResponse.ok("Lấy lịch phân công khoa thành công", phanCongService.layLichKhoa(khoaId, tuanId)));
    }

    /** Tiến độ xếp lịch của tất cả các khoa trong tuần. */
    @GetMapping("/tuan/{tuanId}")
    public ResponseEntity<ApiResponse<List<PhanCongDtos.KhoaTienDo>>> tongQuan(@PathVariable Long tuanId) {
        return ResponseEntity.ok(ApiResponse.ok("Lấy tiến độ xếp lịch tuần thành công", phanCongService.tongQuan(tuanId)));
    }

    @GetMapping("/ung-vien")
    public ResponseEntity<ApiResponse<List<PhanCongDtos.UngVien>>> ungVien(
            @RequestParam Long caTrucKhoaId, @RequestParam VaiTroPhanCong vaiTro) {
        return ResponseEntity.ok(ApiResponse.ok(phanCongService.danhSachUngVien(caTrucKhoaId, vaiTro)));
    }
}
