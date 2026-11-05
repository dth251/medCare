package com.medcare.clinic.service.impl;

import com.medcare.clinic.dto.thanhtoan.DieuChinhRequest;
import com.medcare.clinic.dto.thanhtoan.KhieuNaiRequest;
import com.medcare.clinic.dto.thanhtoan.ThanhToanDtos;
import com.medcare.clinic.dto.thanhtoan.TuChoiRequest;
import com.medcare.clinic.entity.*;
import com.medcare.clinic.enums.LoaiCa;
import com.medcare.clinic.enums.TrangThaiThanhToan;
import com.medcare.clinic.exception.BusinessRuleException;
import com.medcare.clinic.exception.ResourceNotFoundException;
import com.medcare.clinic.mapper.KhieuNaiMapper;
import com.medcare.clinic.repository.*;
import com.medcare.clinic.service.KhieuNaiService;
import com.medcare.clinic.service.ThanhToanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class KhieuNaiServiceImpl implements KhieuNaiService {

    private final YeuCauKhieuNaiRepository yeuCauKhieuNaiRepository;
    private final BangPhanCongRepository bangPhanCongRepository;
    private final NhanVienRepository nhanVienRepository;
    private final ChamCongRepository chamCongRepository;
    private final PhieuThanhToanRepository phieuThanhToanRepository;
    private final NhanSuYTeRepository nhanSuYTeRepository;
    private final KhieuNaiMapper khieuNaiMapper;

    @Override
    @Transactional
    public ThanhToanDtos.KhieuNai taoKhieuNai(String username, KhieuNaiRequest request) {
        Long bpcId = request.getBangPhanCongId();
        BangPhanCong pc = bangPhanCongRepository.findById(bpcId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca được phân công"));
        NhanSuYTe nhanSu = pc.getNhanSuYTe();
        if (!nhanSu.getTenDangNhap().equals(username)) {
            throw new AccessDeniedException("Chỉ được khiếu nại ca trực của chính mình");
        }
        if (yeuCauKhieuNaiRepository.existsByBangPhanCongIdAndTrangThai(pc.getId(), CHO_XU_LY)) {
            throw new BusinessRuleException("Ca trực này đã có khiếu nại đang chờ xử lý!");
        }
        kiemTraChuaThanhToan(nhanSu, pc.getTuanLamViec());

        YeuCauKhieuNai yeuCau = YeuCauKhieuNai.builder()
                .maYeuCau("YCKN-" + System.currentTimeMillis())
                .nhanSu(nhanSu)
                .bangPhanCong(pc)
                .chamCong(chamCongRepository.findByBangPhanCongId(pc.getId()).orElse(null))
                .noiDung(request.getNoiDung())
                .dsMinhChung(request.getDuongDanMinhChung())
                .ngayGui(LocalDateTime.now())
                .trangThai(CHO_XU_LY)
                .build();

        return khieuNaiMapper.toDto(yeuCauKhieuNaiRepository.save(yeuCau));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThanhToanDtos.KhieuNai> danhSachCuaToi(String username) {
        NhanSuYTe nhanSu = nhanSuYTeRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự"));
        return yeuCauKhieuNaiRepository.findByNhanSuIdOrderByNgayGuiDesc(nhanSu.getId()).stream()
                .map(khieuNaiMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThanhToanDtos.KhieuNai> danhSach(String trangThai) {
        List<YeuCauKhieuNai> list;
        if (trangThai == null || trangThai.isBlank() || "TAT_CA".equalsIgnoreCase(trangThai)) {
            list = yeuCauKhieuNaiRepository.findAllByOrderByNgayGuiDesc();
        } else {
            list = yeuCauKhieuNaiRepository.findByTrangThaiInOrderByNgayGuiDesc(List.of(trangThai.toUpperCase()));
        }
        return list.stream().map(khieuNaiMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ThanhToanDtos.KhieuNai chiTiet(Long id) {
        YeuCauKhieuNai k = yeuCauKhieuNaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại #" + id));
        return khieuNaiMapper.toDto(k);
    }

    @Override
    @Transactional
    public ThanhToanDtos.KhieuNai lapBienBanDieuChinh(String accountantUsername, DieuChinhRequest request) {
        NhanVien keToan = nhanVienRepository.findByTenDangNhap(accountantUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kế toán"));
        YeuCauKhieuNai yeuCau = yeuCauKhieuNaiRepository.findById(request.getDonKhieuNaiId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại"));

        if (!CHO_XU_LY.equals(yeuCau.getTrangThai())) {
            throw new BusinessRuleException("Khiếu nại này đã được xử lý trước đó!");
        }

        NhanSuYTe nhanSu = yeuCau.getNhanSu();
        TuanLamViec tuan = yeuCau.getBangPhanCong() != null ? yeuCau.getBangPhanCong().getTuanLamViec() : null;
        kiemTraChuaThanhToan(nhanSu, tuan);

        BigDecimal donGia = nhanSu.getDonGiaTienCong();
        if (donGia == null && (request.getSoCaChinhDieuChinh() != 0 || request.getSoCaNgoaiGioDieuChinh() != 0)) {
            throw new BusinessRuleException("Nhân sự chưa được cấu hình đơn giá ca, không thể quy đổi số ca!");
        }
        BigDecimal heSoOT = heSoNgoaiGio(yeuCau.getBangPhanCong());

        BigDecimal tienChinh = donGia != null ? donGia.multiply(BigDecimal.valueOf(request.getSoCaChinhDieuChinh())) : BigDecimal.ZERO;
        BigDecimal tienOT = donGia != null ? donGia.multiply(heSoOT).multiply(BigDecimal.valueOf(request.getSoCaNgoaiGioDieuChinh())) : BigDecimal.ZERO;
        BigDecimal tongDieuChinh = tienChinh.add(tienOT).add(request.getPhuCapBoSung()).subtract(request.getKhauTru());

        yeuCau.setTrangThai(DA_XU_LY);
        yeuCau.setNgayXuLy(LocalDateTime.now());
        yeuCau.setNguoiXuLy(keToan);
        yeuCau.setLyDoPhanHoi(request.getLyDo());
        yeuCau.setSoCaChinhDieuChinh(request.getSoCaChinhDieuChinh());
        yeuCau.setSoCaNgoaiGioDieuChinh(request.getSoCaNgoaiGioDieuChinh());
        yeuCau.setDonGiaCaApDung(donGia != null ? donGia : BigDecimal.ZERO);
        yeuCau.setHeSoNgoaiGioApDung(heSoOT);
        yeuCau.setPhuCapBoSung(request.getPhuCapBoSung());
        yeuCau.setKhauTru(request.getKhauTru());
        yeuCau.setSoTienDieuChinh(tongDieuChinh);
        yeuCau.setLyDo(request.getLyDo());

        return khieuNaiMapper.toDto(yeuCauKhieuNaiRepository.save(yeuCau));
    }

    @Override
    @Transactional
    public ThanhToanDtos.KhieuNai tuChoi(String accountantUsername, Long id, String lyDo) {
        NhanVien keToan = nhanVienRepository.findByTenDangNhap(accountantUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kế toán"));
        YeuCauKhieuNai yeuCau = yeuCauKhieuNaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại"));

        if (!CHO_XU_LY.equals(yeuCau.getTrangThai())) {
            throw new BusinessRuleException("Khiếu nại này đã được xử lý trước đó!");
        }

        yeuCau.setTrangThai(TU_CHOI);
        yeuCau.setNgayXuLy(LocalDateTime.now());
        yeuCau.setNguoiXuLy(keToan);
        yeuCau.setLyDoPhanHoi(lyDo);

        return khieuNaiMapper.toDto(yeuCauKhieuNaiRepository.save(yeuCau));
    }

    private void kiemTraChuaThanhToan(NhanSuYTe nhanSu, TuanLamViec tuan) {
        if (nhanSu == null || tuan == null) return;
        Optional<PhieuThanhToan> daCo = phieuThanhToanRepository.findByNhanSuYTeIdAndTuanLamViecId(nhanSu.getId(), tuan.getId());
        if (daCo.isPresent() && daCo.get().getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
            throw new BusinessRuleException("Tuần này đã được thanh toán, không thể gửi hay xử lý khiếu nại!");
        }
    }

    private static BigDecimal heSoNgoaiGio(BangPhanCong pc) {
        if (pc != null && pc.getCaTrucKhoa() != null) {
            CaLamViec clv = pc.getCaTrucKhoa().getCaLamViec();
            if (clv != null && clv.getLoaiCa() == LoaiCa.NGOAI_GIO && clv.getHeSoLuong() != null) {
                return clv.getHeSoLuong();
            }
        }
        return ThanhToanService.HE_SO_NGOAI_GIO_MAC_DINH;
    }
}
