package com.medcare.clinic.dto.thanhtoan;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KhieuNaiRequest {
    @NotNull(message = "ID phân công ca không được để trống")
    @JsonAlias({"caLamViecId", "phanCongCaId", "bangPhanCongId"})
    private Long bangPhanCongId;

    public Long getPhanCongCaId() {
        return bangPhanCongId;
    }

    public void setPhanCongCaId(Long phanCongCaId) {
        this.bangPhanCongId = phanCongCaId;
    }

    @NotBlank(message = "Nội dung khiếu nại không được để trống")
    @JsonAlias({"lyDo", "noiDung"})
    private String noiDung;

    @JsonAlias({"minhChung", "duongDanMinhChung"})
    private String duongDanMinhChung;
}
