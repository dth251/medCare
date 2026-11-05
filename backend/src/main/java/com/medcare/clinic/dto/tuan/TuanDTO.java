package com.medcare.clinic.dto.tuan;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TuanDTO(
        Long id,
        String ten,
        LocalDate ngayBatDau,
        LocalDate ngayKetThuc,
        LocalDateTime thoiDiemMoDangKy,
        LocalDateTime hanDangKy,
        String trangThai,
        int soCaToiThieu,
        int soCaToiDa,
        int soCaToiDaMoiNgay
) {}
