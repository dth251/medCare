package com.medcare.clinic.service;

import com.medcare.clinic.dto.tuan.TuanDTO;

import java.util.List;

public interface TuanService {
    List<TuanDTO> danhSach();
    TuanDTO layTuanHienTai();
    TuanDTO layTuanKeTiep();
    void dongBoTuanTheoThoiGianThuc();
}
