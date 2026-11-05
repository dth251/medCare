package com.medcare.clinic.service.impl;

import com.medcare.clinic.dto.tuan.TuanDTO;
import com.medcare.clinic.entity.ChamCong;
import com.medcare.clinic.entity.NgayTrongTuan;
import com.medcare.clinic.mapper.TuanMapper;
import com.medcare.clinic.repository.ChamCongRepository;
import com.medcare.clinic.repository.NgayTrongTuanRepository;
import com.medcare.clinic.repository.TuanLamViecRepository;
import com.medcare.clinic.service.TuanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TuanServiceImpl implements TuanService {

    private final TuanLamViecRepository tuanLamViecRepository;
    private final NgayTrongTuanRepository ngayTrongTuanRepository;
    private final ChamCongRepository chamCongRepository;
    private final TuanMapper tuanMapper;

    @Override
    @Transactional
    public List<TuanDTO> danhSach() {
        dongBoTuanTheoThoiGianThuc();
        return tuanLamViecRepository.findAllByOrderByNgayBatDauDesc().stream()
                .map(tuanMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TuanDTO layTuanHienTai() {
        dongBoTuanTheoThoiGianThuc();
        LocalDate today = LocalDate.now();
        return tuanLamViecRepository.timTheoNgay(today)
                .map(tuanMapper::toResponse)
                .orElseGet(() -> danhSach().stream().findFirst().orElse(null));
    }

    @Override
    @Transactional
    public TuanDTO layTuanKeTiep() {
        dongBoTuanTheoThoiGianThuc();
        LocalDate nextWeekDay = LocalDate.now().plusWeeks(1);
        return tuanLamViecRepository.timTheoNgay(nextWeekDay)
                .map(tuanMapper::toResponse)
                .orElseGet(() -> danhSach().stream().findFirst().orElse(null));
    }

    @Override
    public void dongBoTuanTheoThoiGianThuc() {
        LocalDate today = LocalDate.now();
        LocalDate currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate currentSunday = currentMonday.plusDays(6);
        int currentWeekNum = currentMonday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        LocalDate nextMonday = currentMonday.plusWeeks(1);
        LocalDate nextSunday = nextMonday.plusDays(6);
        int nextWeekNum = nextMonday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        LocalDate prevMonday = currentMonday.minusWeeks(1);
        LocalDate prevSunday = prevMonday.plusDays(6);
        int prevWeekNum = prevMonday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        // 1. Đồng bộ Tuần kế tiếp (id = 1)
        tuanLamViecRepository.findById(1L).ifPresent(tuanKeTiep -> {
            if (!Objects.equals(tuanKeTiep.getNgayBatDau(), nextMonday)) {
                tuanKeTiep.setTen("Tuần " + nextWeekNum);
                tuanKeTiep.setNgayBatDau(nextMonday);
                tuanKeTiep.setNgayKetThuc(nextSunday);
                tuanKeTiep.setThoiDiemMoDangKy(currentMonday.atStartOfDay());
                tuanKeTiep.setHanDangKy(currentSunday.atTime(23, 59, 59));
                tuanLamViecRepository.save(tuanKeTiep);
                capNhatNgayTrongTuan(tuanKeTiep.getId(), nextMonday);
            }
        });

        // 2. Đồng bộ Tuần hiện tại (id = 2)
        tuanLamViecRepository.findById(2L).ifPresent(tuanHienTai -> {
            if (!Objects.equals(tuanHienTai.getNgayBatDau(), currentMonday)) {
                tuanHienTai.setTen("Tuần " + currentWeekNum);
                tuanHienTai.setNgayBatDau(currentMonday);
                tuanHienTai.setNgayKetThuc(currentSunday);
                tuanHienTai.setThoiDiemMoDangKy(prevMonday.atStartOfDay());
                tuanHienTai.setHanDangKy(prevSunday.atTime(23, 59, 59));
                tuanLamViecRepository.save(tuanHienTai);
                capNhatNgayTrongTuan(tuanHienTai.getId(), currentMonday);
                capNhatNgayChamCong();
            }
        });

        // 3. Đồng bộ Tuần trước (id = 3)
        tuanLamViecRepository.findById(3L).ifPresent(tuanTruoc -> {
            if (!Objects.equals(tuanTruoc.getNgayBatDau(), prevMonday)) {
                tuanTruoc.setTen("Tuần " + prevWeekNum);
                tuanTruoc.setNgayBatDau(prevMonday);
                tuanTruoc.setNgayKetThuc(prevSunday);
                tuanTruoc.setThoiDiemMoDangKy(prevMonday.minusWeeks(1).atStartOfDay());
                tuanTruoc.setHanDangKy(prevMonday.minusDays(1).atTime(23, 59, 59));
                tuanLamViecRepository.save(tuanTruoc);
            }
        });
    }

    private void capNhatNgayTrongTuan(Long tuanId, LocalDate monday) {
        List<NgayTrongTuan> dsNgay = ngayTrongTuanRepository.findByTuanLamViecId(tuanId);
        for (NgayTrongTuan n : dsNgay) {
            if (n.getThu() != null) {
                n.setNgay(monday.plusDays(n.getThu().ordinal()));
            }
        }
        ngayTrongTuanRepository.saveAll(dsNgay);
    }

    private void capNhatNgayChamCong() {
        List<ChamCong> dsChamCong = chamCongRepository.findAll();
        for (ChamCong cc : dsChamCong) {
            if (cc.getBangPhanCong() != null
                    && cc.getBangPhanCong().getCaTrucKhoa() != null
                    && cc.getBangPhanCong().getCaTrucKhoa().getNgayTrongTuan() != null) {
                LocalDate ngayCa = cc.getBangPhanCong().getCaTrucKhoa().getNgayTrongTuan().getNgay();
                if (ngayCa != null) {
                    if (cc.getGioCheckIn() != null) {
                        cc.setGioCheckIn(LocalDateTime.of(ngayCa, cc.getGioCheckIn().toLocalTime()));
                    }
                    if (cc.getGioCheckOut() != null) {
                        cc.setGioCheckOut(LocalDateTime.of(ngayCa, cc.getGioCheckOut().toLocalTime()));
                    }
                }
            }
        }
        chamCongRepository.saveAll(dsChamCong);
    }
}

