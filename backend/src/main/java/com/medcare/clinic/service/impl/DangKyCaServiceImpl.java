package com.medcare.clinic.service.impl;

import com.medcare.clinic.dto.dangky.DangKyCaRequest;
import com.medcare.clinic.dto.dangky.DangKyDtos;
import com.medcare.clinic.entity.*;
import com.medcare.clinic.enums.TrangThaiDangKyCa;
import com.medcare.clinic.enums.TrangThaiPhieuDangKy;
import com.medcare.clinic.enums.TrangThaiTuan;
import com.medcare.clinic.exception.BusinessRuleException;
import com.medcare.clinic.exception.ResourceNotFoundException;
import com.medcare.clinic.mapper.DangKyMapper;
import com.medcare.clinic.mapper.TuanMapper;
import com.medcare.clinic.repository.CaTrucKhoaRepository;
import com.medcare.clinic.repository.NhanSuYTeRepository;
import com.medcare.clinic.repository.PhieuDangKyRepository;
import com.medcare.clinic.repository.TuanLamViecRepository;
import com.medcare.clinic.service.DangKyCaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DangKyCaServiceImpl implements DangKyCaService {

    private final PhieuDangKyRepository phieuDangKyRepository;
    private final TuanLamViecRepository tuanLamViecRepository;
    private final NhanSuYTeRepository nhanSuYTeRepository;
    private final CaTrucKhoaRepository caTrucKhoaRepository;
    private final DangKyMapper dangKyMapper;
    private final TuanMapper tuanMapper;

    @Override
    @Transactional
    public DangKyDtos.PhieuDangKy luuDangKy(String username, DangKyCaRequest request) {
        NhanSuYTe nhanSu = timNhanSu(username);
        TuanLamViec tuan = tuanLamViecRepository.findById(request.getTuanLamViecId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuần làm việc"));

        LocalDateTime now = LocalDateTime.now();
        kiemTraCongDangKy(tuan, now);

        List<Long> shiftIds = request.getCaTrucKhoaIds();
        Set<Long> uniqueIds = new LinkedHashSet<>(shiftIds);
        if (uniqueIds.size() != shiftIds.size()) {
            throw new BusinessRuleException("Danh sách ca đăng ký bị trùng lặp!");
        }

        int totalShifts = uniqueIds.size();
        if (totalShifts < tuan.getSoCaToiThieu()) {
            throw new BusinessRuleException(String.format("Số ca đăng ký phải tối thiểu %d ca/tuần (Hiện tại: %d ca)",
                    tuan.getSoCaToiThieu(), totalShifts));
        }
        if (totalShifts > tuan.getSoCaToiDa()) {
            throw new BusinessRuleException(String.format("Số ca đăng ký tối đa %d ca/tuần (Hiện tại: %d ca)",
                    tuan.getSoCaToiDa(), totalShifts));
        }

        List<CaTrucKhoa> caList = caTrucKhoaRepository.findAllById(uniqueIds);
        if (caList.size() != totalShifts) {
            throw new BusinessRuleException("Có ca trực không tồn tại trong hệ thống!");
        }

        Long khoaId = nhanSu.getKhoa() != null ? nhanSu.getKhoa().getId() : null;
        Map<Long, Integer> shiftsPerDay = new HashMap<>();
        for (CaTrucKhoa ca : caList) {
            NgayTrongTuan ngay = ca.getNgayTrongTuan();
            if (ngay.getTuanLamViec() != null && !Objects.equals(ngay.getTuanLamViec().getId(), tuan.getId())) {
                throw new BusinessRuleException("Ca trực #" + ca.getId() + " không thuộc tuần đang đăng ký!");
            }
            if (ca.getKhoa() != null && khoaId != null && !Objects.equals(ca.getKhoa().getId(), khoaId)) {
                throw new BusinessRuleException("Ca trực #" + ca.getId() + " không thuộc chuyên khoa của bạn!");
            }
            if (!ca.isDangMoCua()) {
                throw new BusinessRuleException("Ca trực #" + ca.getId() + " đã đóng, không nhận đăng ký!");
            }
            int count = shiftsPerDay.merge(ngay.getId(), 1, Integer::sum);
            if (count > tuan.getSoCaToiDaMoiNgay()) {
                throw new BusinessRuleException("Không được đăng ký vượt quá tối đa " + tuan.getSoCaToiDaMoiNgay() + " ca trong cùng một ngày!");
            }
        }

        PhieuDangKy phieu = phieuDangKyRepository.findByNhanSuYTeIdAndTuanLamViecId(nhanSu.getId(), tuan.getId())
                .orElseGet(() -> PhieuDangKy.builder()
                        .maPhieu("PDK-" + tuan.getId() + "-" + nhanSu.getId())
                        .nhanSuYTe(nhanSu)
                        .tuanLamViec(tuan)
                        .build());

        if (phieu.getTrangThai() == TrangThaiPhieuDangKy.DA_KHOA) {
            throw new BusinessRuleException("Phiếu đăng ký tuần này đã bị khóa, không thể chỉnh sửa!");
        }

        phieu.setNgayDangKy(now);
        phieu.setTrangThai(TrangThaiPhieuDangKy.DA_LUU);
        phieu.setTongSoCa(totalShifts);

        if (phieu.getId() != null && !phieu.getChiTietDangKy().isEmpty()) {
            phieu.getChiTietDangKy().clear();
            phieu = phieuDangKyRepository.saveAndFlush(phieu);
        }

        for (CaTrucKhoa ca : caList) {
            phieu.getChiTietDangKy().add(DangKyCa.builder()
                    .phieuDangKy(phieu)
                    .caTrucKhoa(ca)
                    .ngayDangKy(now)
                    .trangThai(TrangThaiDangKyCa.HOP_LE)
                    .build());
        }

        PhieuDangKy saved = phieuDangKyRepository.save(phieu);
        return dangKyMapper.toResponse(saved, saved.getChiTietDangKy());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DangKyDtos.PhieuDangKy> getMyRegistration(String username, Long tuanId) {
        NhanSuYTe nhanSu = timNhanSu(username);
        return phieuDangKyRepository.findByNhanSuYTeIdAndTuanLamViecId(nhanSu.getId(), tuanId)
                .map(p -> dangKyMapper.toResponse(p, p.getChiTietDangKy()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DangKyDtos.CaTruc> danhSachCaTruc(String username, Long tuanId) {
        NhanSuYTe nhanSu = timNhanSu(username);
        if (nhanSu.getKhoa() == null) {
            throw new BusinessRuleException("Nhân sự chưa được gán khoa!");
        }
        return caTrucKhoaRepository.timTheoKhoaTuan(nhanSu.getKhoa().getId(), tuanId).stream()
                .map(tuanMapper::toCaTruc)
                .toList();
    }

    private void kiemTraCongDangKy(TuanLamViec tuan, LocalDateTime now) {
        if (tuan.getTrangThai() != TrangThaiTuan.DANG_MO_DANG_KY) {
            throw new BusinessRuleException("Cổng đăng ký ca của " + tuan.getTen() + " không trong giai đoạn mở!");
        }
        if (tuan.getThoiDiemMoDangKy() != null && now.isBefore(tuan.getThoiDiemMoDangKy())) {
            throw new BusinessRuleException("Cổng đăng ký ca của " + tuan.getTen() + " chưa đến thời điểm mở!");
        }
        if (tuan.getHanDangKy() != null && now.isAfter(tuan.getHanDangKy())) {
            throw new BusinessRuleException("Đã quá hạn đăng ký ca của " + tuan.getTen() + "!");
        }
    }

    private NhanSuYTe timNhanSu(String username) {
        return nhanSuYTeRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự y tế"));
    }
}
