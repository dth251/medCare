package com.medcare.clinic.service;

import com.medcare.clinic.dto.thanhtoan.DieuChinhRequest;
import com.medcare.clinic.dto.thanhtoan.KhieuNaiRequest;
import com.medcare.clinic.dto.thanhtoan.ThanhToanDtos;
import com.medcare.clinic.entity.*;
import com.medcare.clinic.enums.LoaiCa;
import com.medcare.clinic.enums.TrangThaiThanhToan;
import com.medcare.clinic.exception.BusinessRuleException;
import com.medcare.clinic.exception.ResourceNotFoundException;
import com.medcare.clinic.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KhieuNaiService {

    public static final String CHO_XU_LY = "CHO_XU_LY";
    public static final String DA_XU_LY = "DA_XU_LY";
    public static final String TU_CHOI = "TU_CHOI";

    private final YeuCauKhieuNaiRepository yeuCauKhieuNaiRepository;
    private final PhanCongCaRepository phanCongCaRepository;
    private final NhanVienRepository nhanVienRepository;
    private final ChamCongRepository chamCongRepository;
    private final PhieuThanhToanRepository phieuThanhToanRepository;
    private final NhanSuYTeRepository nhanSuYTeRepository;

    // =====================================================================
    // Nhân sự gửi khiếu nại
    // =====================================================================

    @Transactional
    public ThanhToanDtos.KhieuNai taoKhieuNai(String username, KhieuNaiRequest request) {
        PhanCongCa pc = phanCongCaRepository.findById(request.getPhanCongCaId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca được phân công"));
        NhanSuYTe nhanSu = pc.getNhanSuYTe();
        if (!nhanSu.getTenDangNhap().equals(username)) {
            throw new AccessDeniedException("Chỉ được khiếu nại ca trực của chính mình");
        }
        if (yeuCauKhieuNaiRepository.existsByPhanCongCaIdAndTrangThai(pc.getId(), CHO_XU_LY)) {
            throw new BusinessRuleException("Ca trực này đã có khiếu nại đang chờ xử lý!");
        }
        kiemTraChuaThanhToan(nhanSu, pc.getBangPhanCong().getTuanLamViec());

        YeuCauKhieuNai yeuCau = YeuCauKhieuNai.builder()
                .maYeuCau("YCKN-" + System.currentTimeMillis())
                .nhanSu(nhanSu)
                .phanCongCa(pc)
                .chamCong(chamCongRepository.findByPhanCongCaId(pc.getId()).orElse(null))
                .bangPhanCong(pc.getBangPhanCong())
                .noiDung(request.getNoiDung())
                .dsMinhChung(request.getDuongDanMinhChung())
                .ngayGui(LocalDateTime.now())
                .trangThai(CHO_XU_LY)
                .donGiaCaApDung(nhanSu.getDonGiaTienCong() != null ? nhanSu.getDonGiaTienCong() : BigDecimal.ZERO)
                .heSoNgoaiGioApDung(heSoNgoaiGio(pc))
                .build();

        return toDto(yeuCauKhieuNaiRepository.save(yeuCau));
    }

    @Transactional(readOnly = true)
    public List<ThanhToanDtos.KhieuNai> danhSachCuaToi(String username) {
        NhanSuYTe nhanSu = nhanSuYTeRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự"));
        return yeuCauKhieuNaiRepository.findByNhanSuIdOrderByNgayGuiDesc(nhanSu.getId()).stream()
                .map(KhieuNaiService::toDto)
                .toList();
    }

    // =====================================================================
    // Kế toán đối soát
    // =====================================================================

    @Transactional(readOnly = true)
    public List<ThanhToanDtos.KhieuNai> danhSach(String trangThai) {
        List<YeuCauKhieuNai> ds = StringUtils.hasText(trangThai)
                ? yeuCauKhieuNaiRepository.findByTrangThaiInOrderByNgayGuiDesc(List.of(trangThai))
                : yeuCauKhieuNaiRepository.findAllByOrderByNgayGuiDesc();
        return ds.stream().map(KhieuNaiService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ThanhToanDtos.KhieuNai chiTiet(Long id) {
        return toDto(timKhieuNai(id));
    }

    @Transactional
    public ThanhToanDtos.KhieuNai lapBienBanDieuChinh(String accountantUsername, DieuChinhRequest request) {
        YeuCauKhieuNai yeuCau = timKhieuNai(request.getDonKhieuNaiId());
        if (!CHO_XU_LY.equals(yeuCau.getTrangThai())) {
            throw new BusinessRuleException("Khiếu nại " + yeuCau.getMaYeuCau() + " đã được xử lý trước đó!");
        }
        if (yeuCau.getNhanSu() != null && yeuCau.getBangPhanCong() != null) {
            kiemTraChuaThanhToan(yeuCau.getNhanSu(), yeuCau.getBangPhanCong().getTuanLamViec());
        }
        NhanVien ketoan = nhanVienRepository.findByTenDangNhap(accountantUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên kế toán"));

        BigDecimal donGia = yeuCau.getNhanSu() != null && yeuCau.getNhanSu().getDonGiaTienCong() != null
                ? yeuCau.getNhanSu().getDonGiaTienCong()
                : Optional.ofNullable(yeuCau.getDonGiaCaApDung()).orElse(BigDecimal.ZERO);
        if (donGia.signum() == 0 && (request.getSoCaChinhDieuChinh() > 0 || request.getSoCaNgoaiGioDieuChinh() > 0)) {
            throw new BusinessRuleException("Nhân sự chưa được cấu hình đơn giá ca, không thể quy đổi số ca!");
        }
        BigDecimal heSoOT = heSoNgoaiGio(yeuCau.getPhanCongCa());

        BigDecimal tienChinh = donGia.multiply(BigDecimal.valueOf(request.getSoCaChinhDieuChinh()));
        BigDecimal tienOT = donGia.multiply(heSoOT).multiply(BigDecimal.valueOf(request.getSoCaNgoaiGioDieuChinh()));
        BigDecimal tongDieuChinh = tienChinh.add(tienOT).add(request.getPhuCapBoSung()).subtract(request.getKhauTru());

        yeuCau.setSoCaChinhDieuChinh(request.getSoCaChinhDieuChinh());
        yeuCau.setSoCaNgoaiGioDieuChinh(request.getSoCaNgoaiGioDieuChinh());
        yeuCau.setDonGiaCaApDung(donGia);
        yeuCau.setHeSoNgoaiGioApDung(heSoOT);
        yeuCau.setPhuCapBoSung(request.getPhuCapBoSung());
        yeuCau.setKhauTru(request.getKhauTru());
        yeuCau.setSoTienDieuChinh(tongDieuChinh);
        yeuCau.setLyDo(request.getLyDo());
        yeuCau.setLyDoPhanHoi(request.getLyDo());
        yeuCau.setNguoiXuLy(ketoan);
        yeuCau.setNgayXuLy(LocalDateTime.now());
        yeuCau.setTrangThai(DA_XU_LY);

        return toDto(yeuCauKhieuNaiRepository.save(yeuCau));
    }

    @Transactional
    public ThanhToanDtos.KhieuNai tuChoi(String accountantUsername, Long id, String lyDo) {
        YeuCauKhieuNai yeuCau = timKhieuNai(id);
        if (!CHO_XU_LY.equals(yeuCau.getTrangThai())) {
            throw new BusinessRuleException("Khiếu nại " + yeuCau.getMaYeuCau() + " đã được xử lý trước đó!");
        }
        NhanVien ketoan = nhanVienRepository.findByTenDangNhap(accountantUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên kế toán"));
        yeuCau.setTrangThai(TU_CHOI);
        yeuCau.setLyDoPhanHoi(lyDo);
        yeuCau.setSoTienDieuChinh(BigDecimal.ZERO);
        yeuCau.setNguoiXuLy(ketoan);
        yeuCau.setNgayXuLy(LocalDateTime.now());
        return toDto(yeuCauKhieuNaiRepository.save(yeuCau));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private YeuCauKhieuNai timKhieuNai(Long id) {
        return yeuCauKhieuNaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu khiếu nại #" + id));
    }

    private void kiemTraChuaThanhToan(NhanSuYTe nhanSu, TuanLamViec tuan) {
        if (tuan == null) return;
        phieuThanhToanRepository.findByNhanSuYTeIdAndTuanLamViecId(nhanSu.getId(), tuan.getId())
                .filter(p -> p.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN)
                .ifPresent(p -> {
                    throw new BusinessRuleException("Thù lao " + tuan.getTen() + " của " + nhanSu.getHoVaTen()
                            + " đã được chi trả (phiếu " + p.getMaPhieu() + "), không thể điều chỉnh thêm!");
                });
    }

    private static BigDecimal heSoNgoaiGio(PhanCongCa pc) {
        if (pc != null) {
            CaLamViec clv = pc.getCaTrucKhoa().getCaLamViec();
            if (clv.getLoaiCa() == LoaiCa.NGOAI_GIO && clv.getHeSoLuong() != null) return clv.getHeSoLuong();
        }
        return ThanhToanService.HE_SO_NGOAI_GIO_MAC_DINH;
    }

    static ThanhToanDtos.DieuChinh toDieuChinh(YeuCauKhieuNai k) {
        return new ThanhToanDtos.DieuChinh(k.getId(), k.getMaYeuCau(),
                k.getSoCaChinhDieuChinh(), k.getSoCaNgoaiGioDieuChinh(),
                k.getPhuCapBoSung(), k.getKhauTru(), k.getSoTienDieuChinh(), k.getLyDo());
    }

    static ThanhToanDtos.KhieuNai toDto(YeuCauKhieuNai k) {
        NhanSuYTe ns = k.getNhanSu();
        PhanCongCa pc = k.getPhanCongCa();
        ChamCong cc = k.getChamCong();
        CaTrucKhoa ca = pc != null ? pc.getCaTrucKhoa() : null;
        CaLamViec clv = ca != null ? ca.getCaLamViec() : null;
        TuanLamViec tuan = k.getBangPhanCong() != null ? k.getBangPhanCong().getTuanLamViec() : null;

        return new ThanhToanDtos.KhieuNai(
                k.getId(), k.getMaYeuCau(), k.getTrangThai(), k.getNgayGui(), k.getNgayXuLy(),
                k.getNoiDung(), k.getDsMinhChung(), k.getLyDoPhanHoi(),
                ns != null ? ns.getId() : null,
                ns != null ? HoSoMapper.maNhanSu(ns) : null,
                ns != null ? ns.getHoVaTen() : null,
                ns != null && ns.getKhoa() != null ? ns.getKhoa().getTen() : null,
                tuan != null ? tuan.getId() : null,
                tuan != null ? tuan.getTen() : null,
                pc != null ? pc.getId() : null,
                pc != null ? pc.getVaiTro().name() : null,
                pc != null && pc.isDieuDongNgoaiDangKy(),
                ca != null ? ca.getNgayTrongTuan().getNgay() : null,
                ca != null && ca.getNgayTrongTuan().getThu() != null ? ca.getNgayTrongTuan().getThu().name() : null,
                clv != null ? clv.getTenCa() : null,
                clv != null ? HoSoMapper.khungGio(clv) : null,
                clv != null ? clv.getLoaiCa().name() : null,
                clv != null ? clv.getHeSoLuong() : null,
                cc != null ? cc.getTrangThai().name() : null,
                cc != null && cc.getGioCheckIn() != null ? HoSoMapper.hhmm(cc.getGioCheckIn().toLocalTime()) : null,
                cc != null && cc.getGioCheckOut() != null ? HoSoMapper.hhmm(cc.getGioCheckOut().toLocalTime()) : null,
                cc != null ? cc.getGhiChu() : null,
                DA_XU_LY.equals(k.getTrangThai()) ? toDieuChinh(k) : null,
                k.getNguoiXuLy() != null ? k.getNguoiXuLy().getHoVaTen() : null);
    }
}
