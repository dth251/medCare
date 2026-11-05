package com.medcare.clinic.dto.dangky;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class DangKyDtos {
    private DangKyDtos() {}

    /** Một ca trực của khoa trong tuần (ô trong ma trận đăng ký / phân ca). */
    public record CaTruc(
            Long id,
            Long ngayId,
            LocalDate ngay,
            String thu,
            Long caLamViecId,
            String tenCa,
            String loaiCa,
            LocalTime gioBatDau,
            LocalTime gioKetThuc,
            BigDecimal heSo
    ) {}

    public record PhieuDangKy(
            Long id,
            String maPhieu,
            Long tuanId,
            String trangThai,
            LocalDateTime ngayDangKy,
            int tongSoCa,
            List<Long> caTrucKhoaIds
    ) {}
}
