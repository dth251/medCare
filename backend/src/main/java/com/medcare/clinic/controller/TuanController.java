package com.medcare.clinic.controller;

import com.medcare.clinic.dto.common.ApiResponse;
import com.medcare.clinic.dto.tuan.TuanDTO;
import com.medcare.clinic.service.TuanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/tuan")
@RequiredArgsConstructor
public class TuanController {

    private final TuanService tuanService;

    @GetMapping({"", "/danh-sach"})
    public ResponseEntity<ApiResponse<List<TuanDTO>>> danhSach() {
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách tuần làm việc thành công", tuanService.danhSach()));
    }

    @GetMapping("/hien-tai")
    public ResponseEntity<ApiResponse<TuanDTO>> tuanHienTai() {
        return ResponseEntity.ok(ApiResponse.ok("Lấy tuần làm việc hiện tại thành công", tuanService.layTuanHienTai()));
    }

    @GetMapping("/ke-tiep")
    public ResponseEntity<ApiResponse<TuanDTO>> tuanKeTiep() {
        return ResponseEntity.ok(ApiResponse.ok("Lấy tuần làm việc kế tiếp thành công", tuanService.layTuanKeTiep()));
    }
}
