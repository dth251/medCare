package com.medcare.clinic.service;

import com.medcare.clinic.dto.phancong.PhanCongDtos;
import com.medcare.clinic.dto.phancong.PhanCongRequest;
import com.medcare.clinic.enums.VaiTroPhanCong;

import java.util.List;

public interface PhanCongService {
    PhanCongDtos.Slot phanCong(String managerUsername, PhanCongRequest request);
    void huyPhanCong(Long phanCongId);
    PhanCongDtos.CongBoKetQua congBoLichTuan(Long tuanId);
    PhanCongDtos.LichKhoa layLichKhoa(Long khoaId, Long tuanId);
    List<PhanCongDtos.KhoaTienDo> tongQuan(Long tuanId);
    List<PhanCongDtos.UngVien> danhSachUngVien(Long caTrucKhoaId, VaiTroPhanCong vaiTro);
}
