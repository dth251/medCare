package com.medcare.clinic.service;

import com.medcare.clinic.dto.dangky.DangKyCaRequest;
import com.medcare.clinic.dto.dangky.DangKyDtos;

import java.util.List;
import java.util.Optional;

public interface DangKyCaService {
    DangKyDtos.PhieuDangKy luuDangKy(String username, DangKyCaRequest request);
    Optional<DangKyDtos.PhieuDangKy> getMyRegistration(String username, Long tuanId);
    List<DangKyDtos.CaTruc> danhSachCaTruc(String username, Long tuanId);
}
