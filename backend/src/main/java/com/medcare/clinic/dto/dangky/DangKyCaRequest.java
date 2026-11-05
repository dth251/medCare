package com.medcare.clinic.dto.dangky;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DangKyCaRequest {
    @NotNull(message = "ID tuần làm việc không được để trống")
    @JsonAlias({"tuanId", "tuanLamViecId"})
    private Long tuanLamViecId;

    @NotEmpty(message = "Danh sách ca trực không được để trống")
    @JsonAlias({"caIds", "caTrucKhoaIds"})
    private List<Long> caTrucKhoaIds;
}
