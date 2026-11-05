package com.medcare.clinic.service;

import com.medcare.clinic.dto.thanhtoan.ChamCongDTO;
import com.medcare.clinic.dto.thanhtoan.ThanhToanDtos;
import com.medcare.clinic.dto.thanhtoan.ThanhToanRequest;

import java.math.BigDecimal;
import java.util.List;

public interface ThanhToanService {
    BigDecimal HE_SO_NGOAI_GIO_MAC_DINH = BigDecimal.valueOf(2.0);

    ThanhToanDtos.PhieuLuong thanhToan(String accountantUsername, ThanhToanRequest request);
    List<ThanhToanDtos.PhieuLuong> layDanhSachPhieuTuan(Long tuanId);
    List<ThanhToanDtos.KhoaThanhToan> danhSachKhoa(Long tuanId);
    List<ThanhToanDtos.NhanSuLuong> danhSachNhanSu(Long tuanId, Long khoaId);
    ThanhToanDtos.PhieuLuong getPhieu(Long phieuId, String username, boolean laKeToan);
    List<ChamCongDTO> layBangChamCong(String username, boolean laKeToan, Long nhanSuId, Long tuanId);
    List<ThanhToanDtos.PhieuLuong> getTatCaPhieuCuaToi(String username);
    ThanhToanDtos.PhieuLuong getPhieuCuaToi(String username, Long tuanId);
}
