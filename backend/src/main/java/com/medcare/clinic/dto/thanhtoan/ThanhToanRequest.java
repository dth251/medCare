package com.medcare.clinic.dto.thanhtoan;

import com.medcare.clinic.enums.HinhThucThanhToan;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThanhToanRequest {
    @NotNull(message = "ID nhân sự không được để trống")
    private Long nhanSuId;

    @NotNull(message = "ID tuần không được để trống")
    private Long tuanId;

    @NotNull(message = "Hình thức thanh toán không được để trống")
    private HinhThucThanhToan hinhThucThanhToan;

    private String maGiaoDich;
    private String minhChungNhanTien;
}
