package com.medcare.clinic.dto.thanhtoan;

import jakarta.validation.constraints.NotBlank;

public record TuChoiRequest(
        @NotBlank(message = "Lý do từ chối không được để trống") String lyDo
) {}
