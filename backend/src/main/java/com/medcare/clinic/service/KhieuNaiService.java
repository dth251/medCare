package com.medcare.clinic.service;

import com.medcare.clinic.dto.thanhtoan.DieuChinhRequest;
import com.medcare.clinic.dto.thanhtoan.KhieuNaiRequest;
import com.medcare.clinic.dto.thanhtoan.ThanhToanDtos;

import java.util.List;

public interface KhieuNaiService {
    String CHO_XU_LY = "CHO_XU_LY";
    String DA_XU_LY = "DA_XU_LY";
    String TU_CHOI = "TU_CHOI";

    ThanhToanDtos.KhieuNai taoKhieuNai(String username, KhieuNaiRequest request);
    List<ThanhToanDtos.KhieuNai> danhSachCuaToi(String username);
    List<ThanhToanDtos.KhieuNai> danhSach(String trangThai);
    ThanhToanDtos.KhieuNai chiTiet(Long id);
    ThanhToanDtos.KhieuNai lapBienBanDieuChinh(String accountantUsername, DieuChinhRequest request);
    ThanhToanDtos.KhieuNai tuChoi(String accountantUsername, Long id, String lyDo);
}
