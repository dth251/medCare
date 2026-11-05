package com.medcare.clinic.dto.thanhtoan;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DieuChinhRequest {

    @NotNull(message = "ID đơn khiếu nại không được để trống")
    @JsonAlias({"khieuNaiId", "donKhieuNaiId"})
    private Long donKhieuNaiId;

    @Min(value = 0, message = "Số ca chính điều chỉnh không được âm")
    @JsonAlias({"soCaChinhBoSung", "soCaChinhDieuChinh"})
    private int soCaChinhDieuChinh;

    @Min(value = 0, message = "Số ca ngoài giờ điều chỉnh không được âm")
    @JsonAlias({"soCaNgoaiGioBoSung", "soCaNgoaiGioDieuChinh"})
    private int soCaNgoaiGioDieuChinh;

    @NotNull(message = "Phụ cấp bổ sung không được để trống")
    @PositiveOrZero(message = "Phụ cấp bổ sung không được âm")
    private BigDecimal phuCapBoSung = BigDecimal.ZERO;

    @NotNull(message = "Khấu trừ không được để trống")
    @PositiveOrZero(message = "Khấu trừ không được âm")
    private BigDecimal khauTru = BigDecimal.ZERO;

    @NotBlank(message = "Lý do điều chỉnh không được để trống")
    @JsonAlias({"noiDungDieuChinh", "lyDo"})
    private String lyDo;
}
