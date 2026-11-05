package com.medcare.clinic.dto.phancong;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.medcare.clinic.enums.VaiTroPhanCong;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhanCongRequest {
    @NotNull(message = "ID ca trực khoa không được để trống")
    @JsonAlias({"caLamViecId", "caTrucKhoaId"})
    private Long caTrucKhoaId;

    @NotNull(message = "ID nhân sự không được để trống")
    private Long nhanSuId;

    @NotNull(message = "Vai trò phân công không được để trống")
    @JsonAlias({"viTri", "vaiTro"})
    private VaiTroPhanCong vaiTro;

    @JsonAlias({"laDieuDong", "dieuDongNgoaiDangKy"})
    private boolean dieuDongNgoaiDangKy;

    @JsonAlias({"lyDoDieuDong", "ghiChu"})
    private String ghiChu;
}
