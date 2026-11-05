package com.medcare.clinic.service;

import com.medcare.clinic.dto.thanhtoan.ChamCongDTO;
import com.medcare.clinic.dto.thanhtoan.ThanhToanDtos;
import com.medcare.clinic.dto.thanhtoan.ThanhToanRequest;
import com.medcare.clinic.entity.*;
import com.medcare.clinic.enums.HinhThucThanhToan;
import com.medcare.clinic.enums.LoaiCa;
import com.medcare.clinic.enums.TrangThaiChamCong;
import com.medcare.clinic.enums.TrangThaiThanhToan;
import com.medcare.clinic.exception.BusinessRuleException;
import com.medcare.clinic.exception.ResourceNotFoundException;
import com.medcare.clinic.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Quyết toán thù lao theo tuần.
 *
 * Quy tắc tính:
 *  - Mỗi ca được phân công trong tuần được tính công, trừ khi bản ghi chấm công là VANG_MAT hoặc
 *    LOI_THIET_BI (thiếu dữ liệu quẹt thẻ – cần khiếu nại để được bổ sung).
 *    Ca chưa có bản ghi chấm công được tính theo lịch phân công.
 *  - Ca chính = đơn giá × 1.0; ca ngoài giờ = đơn giá × hệ số ngoài giờ (hệ số lương của ca, mặc định 2.0).
 *  - Cộng thêm số tiền của các biên bản điều chỉnh (khiếu nại DA_XU_LY) trong tuần.
 */
@Service
@RequiredArgsConstructor
public class ThanhToanService {

    static final BigDecimal HE_SO_NGOAI_GIO_MAC_DINH = BigDecimal.valueOf(2.0);
    private static final Set<TrangThaiChamCong> KHONG_TINH_CONG = EnumSet.of(TrangThaiChamCong.VANG_MAT, TrangThaiChamCong.LOI_THIET_BI);

    private final PhieuThanhToanRepository phieuThanhToanRepository;
    private final NhanSuYTeRepository nhanSuYTeRepository;
    private final TuanLamViecRepository tuanLamViecRepository;
    private final NhanVienRepository nhanVienRepository;
    private final PhanCongCaRepository phanCongCaRepository;
    private final ChamCongRepository chamCongRepository;
    private final YeuCauKhieuNaiRepository yeuCauKhieuNaiRepository;
    private final KhoaRepository khoaRepository;

    // =====================================================================
    // Tính lương
    // =====================================================================

    @Transactional(readOnly = true)
    public ThanhToanDtos.BangTinhLuong tinhLuong(NhanSuYTe nhanSu, Long tuanId) {
        BigDecimal donGia = nhanSu.getDonGiaTienCong() != null ? nhanSu.getDonGiaTienCong() : BigDecimal.ZERO;
        BigDecimal heSoNgoaiGio = HE_SO_NGOAI_GIO_MAC_DINH;

        int soCaChinh = 0;
        int soCaNgoaiGio = 0;
        int soCaKhongTinh = 0;
        for (PhanCongCa pc : phanCongCaRepository.findByNhanSuYTeIdAndBangPhanCong_TuanLamViec_Id(nhanSu.getId(), tuanId)) {
            Optional<ChamCong> cc = chamCongRepository.findByPhanCongCaId(pc.getId());
            if (cc.isPresent() && KHONG_TINH_CONG.contains(cc.get().getTrangThai())) {
                soCaKhongTinh++;
                continue;
            }
            CaLamViec clv = pc.getCaTrucKhoa().getCaLamViec();
            if (clv.getLoaiCa() == LoaiCa.NGOAI_GIO) {
                soCaNgoaiGio++;
                if (clv.getHeSoLuong() != null) heSoNgoaiGio = clv.getHeSoLuong();
            } else {
                soCaChinh++;
            }
        }

        BigDecimal tienDieuChinh = BigDecimal.ZERO;
        int choXuLy = 0;
        for (YeuCauKhieuNai kn : yeuCauKhieuNaiRepository.findTheoNhanSuTuan(nhanSu.getId(), tuanId)) {
            if (KhieuNaiService.DA_XU_LY.equals(kn.getTrangThai()) && kn.getSoTienDieuChinh() != null) {
                tienDieuChinh = tienDieuChinh.add(kn.getSoTienDieuChinh());
            } else if (KhieuNaiService.CHO_XU_LY.equals(kn.getTrangThai())) {
                choXuLy++;
            }
        }

        BigDecimal tienCaChinh = donGia.multiply(BigDecimal.valueOf(soCaChinh));
        BigDecimal tienCaNgoaiGio = donGia.multiply(heSoNgoaiGio).multiply(BigDecimal.valueOf(soCaNgoaiGio));
        BigDecimal tong = tienCaChinh.add(tienCaNgoaiGio).add(tienDieuChinh);

        return new ThanhToanDtos.BangTinhLuong(soCaChinh, soCaNgoaiGio, soCaKhongTinh, donGia, heSoNgoaiGio,
                tienCaChinh, tienCaNgoaiGio, tienDieuChinh, tong, choXuLy);
    }

    // =====================================================================
    // Chi trả
    // =====================================================================

    @Transactional
    public ThanhToanDtos.PhieuLuong thanhToan(String accountantUsername, ThanhToanRequest request) {
        NhanSuYTe nhanSu = nhanSuYTeRepository.findById(request.getNhanSuId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự y tế"));
        TuanLamViec tuan = tuanLamViecRepository.findById(request.getTuanId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuần làm việc"));
        NhanVien ketoan = nhanVienRepository.findByTenDangNhap(accountantUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên kế toán"));

        Optional<PhieuThanhToan> daCo = phieuThanhToanRepository.findByNhanSuYTeIdAndTuanLamViecId(nhanSu.getId(), tuan.getId());
        if (daCo.isPresent() && daCo.get().getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
            throw new BusinessRuleException("Nhân sự " + nhanSu.getHoVaTen() + " đã được thanh toán " + tuan.getTen() + "!");
        }
        if (nhanSu.getDonGiaTienCong() == null) {
            throw new BusinessRuleException("Nhân sự " + nhanSu.getHoVaTen() + " chưa được cấu hình đơn giá ca!");
        }

        ThanhToanDtos.BangTinhLuong luong = tinhLuong(nhanSu, tuan.getId());
        if (luong.soKhieuNaiChoXuLy() > 0) {
            throw new BusinessRuleException("Còn " + luong.soKhieuNaiChoXuLy() + " khiếu nại chưa xử lý, cần đối soát trước khi thanh toán!");
        }
        if (luong.soCaChinh() + luong.soCaNgoaiGio() == 0 && luong.tienDieuChinh().signum() == 0) {
            throw new BusinessRuleException("Nhân sự không có ca trực nào được tính công trong " + tuan.getTen() + "!");
        }

        String maGiaoDich = request.getMaGiaoDich();
        if (request.getHinhThucThanhToan() == HinhThucThanhToan.TRUC_TUYEN) {
            if (!StringUtils.hasText(nhanSu.getSoTaiKhoan())) {
                throw new BusinessRuleException("Nhân sự chưa khai báo tài khoản ngân hàng, hãy chọn thanh toán trực tiếp!");
            }
            if (!StringUtils.hasText(maGiaoDich)) maGiaoDich = "CK-" + System.currentTimeMillis();
        } else if (!StringUtils.hasText(maGiaoDich)) {
            maGiaoDich = "TM-" + System.currentTimeMillis();
        }

        LocalDateTime now = LocalDateTime.now();
        PhieuThanhToan phieu = daCo.orElseGet(() -> PhieuThanhToan.builder()
                .maPhieu("PL-" + tuan.getId() + "-" + nhanSu.getId())
                .nhanSuYTe(nhanSu)
                .tuanLamViec(tuan)
                .ngayLap(now)
                .build());

        phieu.setKeToan(ketoan);
        phieu.setSoCaChinhGoc(luong.soCaChinh());
        phieu.setSoCaNgoaiGioGoc(luong.soCaNgoaiGio());
        phieu.setDonGiaCaApDung(luong.donGiaCa());
        phieu.setHeSoNgoaiGioApDung(luong.heSoNgoaiGio());
        if (phieu.getPhuCapTrachNhiem() == null) phieu.setPhuCapTrachNhiem(BigDecimal.ZERO);
        if (phieu.getPhuCapThuThuat() == null) phieu.setPhuCapThuThuat(BigDecimal.ZERO);
        if (phieu.getThuong() == null) phieu.setThuong(BigDecimal.ZERO);
        if (phieu.getPhat() == null) phieu.setPhat(BigDecimal.ZERO);
        phieu.setTongThucLinh(luong.tongThucLinh()
                .add(phieu.getPhuCapTrachNhiem()).add(phieu.getPhuCapThuThuat())
                .add(phieu.getThuong()).subtract(phieu.getPhat()));
        phieu.setHinhThucThanhToan(request.getHinhThucThanhToan());
        phieu.setTrangThai(TrangThaiThanhToan.DA_THANH_TOAN);
        phieu.setNgayThanhToan(now);
        phieu.setSoTaiKhoanNhan(nhanSu.getSoTaiKhoan());
        phieu.setNganHangNhan(nhanSu.getTenNganHang());
        phieu.setChuTaiKhoanNhan(nhanSu.getChuTaiKhoan());
        phieu.setMaGiaoDich(maGiaoDich);
        phieu.setMinhChungNhanTien(request.getMinhChungNhanTien());

        PhieuThanhToan saved = phieuThanhToanRepository.save(phieu);

        // Gắn các biên bản điều chỉnh đã áp dụng vào phiếu lương
        List<YeuCauKhieuNai> dieuChinh = yeuCauKhieuNaiRepository.findTheoNhanSuTuan(nhanSu.getId(), tuan.getId()).stream()
                .filter(k -> KhieuNaiService.DA_XU_LY.equals(k.getTrangThai()))
                .toList();
        dieuChinh.forEach(k -> k.setPhieuThanhToan(saved));
        yeuCauKhieuNaiRepository.saveAll(dieuChinh);

        return toPhieuDto(saved, dieuChinh);
    }

    // =====================================================================
    // Tra cứu – kế toán
    // =====================================================================

    @Transactional(readOnly = true)
    public List<ThanhToanDtos.KhoaThanhToan> danhSachKhoa(Long tuanId) {
        kiemTraTuan(tuanId);
        Map<Long, List<NhanSuYTe>> theoKhoa = nhanSuYTeRepository.findAll().stream()
                .filter(n -> n.getKhoa() != null)
                .collect(Collectors.groupingBy(n -> n.getKhoa().getId()));
        Map<Long, PhieuThanhToan> phieuTheoNs = phieuTheoNhanSu(tuanId);

        List<ThanhToanDtos.KhoaThanhToan> result = new ArrayList<>();
        for (Khoa khoa : khoaRepository.findAll(Sort.by("maKhoa"))) {
            List<NhanSuYTe> ds = theoKhoa.getOrDefault(khoa.getId(), List.of());
            BigDecimal tong = BigDecimal.ZERO;
            int daTra = 0;
            for (NhanSuYTe ns : ds) {
                PhieuThanhToan p = phieuTheoNs.get(ns.getId());
                if (p != null && p.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
                    daTra++;
                    tong = tong.add(p.getTongThucLinh());
                } else {
                    tong = tong.add(tinhLuong(ns, tuanId).tongThucLinh());
                }
            }
            result.add(new ThanhToanDtos.KhoaThanhToan(khoa.getId(), khoa.getMaKhoa(), khoa.getTen(), ds.size(), daTra, tong));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ThanhToanDtos.NhanSuLuong> danhSachNhanSu(Long tuanId, Long khoaId) {
        kiemTraTuan(tuanId);
        Map<Long, PhieuThanhToan> phieuTheoNs = phieuTheoNhanSu(tuanId);
        return nhanSuYTeRepository.findByKhoaId(khoaId).stream()
                .sorted(Comparator.comparing(HoSoMapper::maNhanSu))
                .map(ns -> {
                    PhieuThanhToan p = phieuTheoNs.get(ns.getId());
                    boolean daTra = p != null && p.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN;
                    ThanhToanDtos.BangTinhLuong luong = daTra ? luongTuPhieu(p, tinhLuong(ns, tuanId)) : tinhLuong(ns, tuanId);
                    return new ThanhToanDtos.NhanSuLuong(ns.getId(), HoSoMapper.maNhanSu(ns), ns.getHoVaTen(),
                            HoSoMapper.chucDanh(ns), HoSoMapper.hocVi(ns), luong,
                            daTra ? TrangThaiThanhToan.DA_THANH_TOAN.name() : TrangThaiThanhToan.CHUA_THANH_TOAN.name(),
                            p != null ? p.getId() : null);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ThanhToanDtos.PhieuLuong> layDanhSachPhieuTuan(Long tuanId) {
        return phieuThanhToanRepository.findByTuanLamViecId(tuanId).stream().map(this::toPhieuDto).toList();
    }

    // =====================================================================
    // Tra cứu – nhân sự (có kiểm tra quyền sở hữu)
    // =====================================================================

    @Transactional(readOnly = true)
    public ThanhToanDtos.PhieuLuong getPhieu(Long phieuId, String username, boolean laKeToan) {
        PhieuThanhToan p = phieuThanhToanRepository.findById(phieuId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu thanh toán"));
        if (!laKeToan && !p.getNhanSuYTe().getTenDangNhap().equals(username)) {
            throw new AccessDeniedException("Không được xem phiếu lương của người khác");
        }
        return toPhieuDto(p);
    }

    @Transactional(readOnly = true)
    public ThanhToanDtos.PhieuLuong getPhieuCuaToi(String username, Long tuanId) {
        NhanSuYTe nhanSu = timNhanSu(username);
        return phieuThanhToanRepository.findByNhanSuYTeIdAndTuanLamViecId(nhanSu.getId(), tuanId)
                .map(this::toPhieuDto)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có phiếu thanh toán cho tuần này"));
    }

    @Transactional(readOnly = true)
    public List<ThanhToanDtos.PhieuLuong> getTatCaPhieuCuaToi(String username) {
        NhanSuYTe nhanSu = timNhanSu(username);
        return phieuThanhToanRepository.findByNhanSuYTeId(nhanSu.getId()).stream()
                .sorted(Comparator.comparing((PhieuThanhToan p) -> p.getTuanLamViec().getNgayBatDau()).reversed())
                .map(this::toPhieuDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChamCongDTO> layBangChamCong(String username, boolean laKeToan, Long nhanSuId, Long tuanId) {
        NhanSuYTe nhanSu = nhanSuYTeRepository.findById(nhanSuId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự"));
        if (!laKeToan && !nhanSu.getTenDangNhap().equals(username)) {
            throw new AccessDeniedException("Không được xem bảng chấm công của người khác");
        }

        List<ChamCongDTO> list = new ArrayList<>();
        for (PhanCongCa pc : phanCongCaRepository.findByNhanSuYTeIdAndBangPhanCong_TuanLamViec_Id(nhanSuId, tuanId)) {
            Optional<ChamCong> ccOpt = chamCongRepository.findByPhanCongCaId(pc.getId());
            Optional<YeuCauKhieuNai> knOpt = yeuCauKhieuNaiRepository.findByPhanCongCaId(pc.getId()).stream()
                    .max(Comparator.comparing(YeuCauKhieuNai::getNgayGui));

            CaTrucKhoa ca = pc.getCaTrucKhoa();
            CaLamViec clv = ca.getCaLamViec();
            boolean khongTinh = ccOpt.map(c -> KHONG_TINH_CONG.contains(c.getTrangThai())).orElse(false);

            list.add(ChamCongDTO.builder()
                    .id(ccOpt.map(ChamCong::getId).orElse(null))
                    .phanCongCaId(pc.getId())
                    .nhanSuId(nhanSuId)
                    .tenNhanSu(nhanSu.getHoVaTen())
                    .ngay(ca.getNgayTrongTuan().getNgay() != null ? ca.getNgayTrongTuan().getNgay().toString() : "")
                    .thu(ca.getNgayTrongTuan().getThu() != null ? ca.getNgayTrongTuan().getThu().name() : "")
                    .tenCa(clv.getTenCa())
                    .thoiGian(HoSoMapper.khungGio(clv))
                    .loaiCa(clv.getLoaiCa().name())
                    .heSo(clv.getHeSoLuong() != null ? clv.getHeSoLuong().doubleValue() : 1.0)
                    .gioCheckIn(ccOpt.map(c -> c.getGioCheckIn() != null ? HoSoMapper.hhmm(c.getGioCheckIn().toLocalTime()) : "--:--").orElse("--:--"))
                    .gioCheckOut(ccOpt.map(c -> c.getGioCheckOut() != null ? HoSoMapper.hhmm(c.getGioCheckOut().toLocalTime()) : "--:--").orElse("--:--"))
                    .trangThai(ccOpt.map(c -> c.getTrangThai().name()).orElse("CHUA_CO_DU_LIEU"))
                    .ghiChu(ccOpt.map(ChamCong::getGhiChu).orElse(""))
                    .duocTinhCong(!khongTinh)
                    .thieuQuetThe(khongTinh)
                    .khieuNaiId(knOpt.map(YeuCauKhieuNai::getId).orElse(null))
                    .maDonKhieuNai(knOpt.map(YeuCauKhieuNai::getMaYeuCau).orElse(null))
                    .trangThaiKhieuNai(knOpt.map(YeuCauKhieuNai::getTrangThai).orElse(null))
                    .build());
        }
        list.sort(Comparator.comparing(ChamCongDTO::getNgay).thenComparing(ChamCongDTO::getThoiGian));
        return list;
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private void kiemTraTuan(Long tuanId) {
        if (!tuanLamViecRepository.existsById(tuanId)) {
            throw new ResourceNotFoundException("Không tìm thấy tuần làm việc");
        }
    }

    private NhanSuYTe timNhanSu(String username) {
        return nhanSuYTeRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự"));
    }

    private Map<Long, PhieuThanhToan> phieuTheoNhanSu(Long tuanId) {
        return phieuThanhToanRepository.findByTuanLamViecId(tuanId).stream()
                .collect(Collectors.toMap(p -> p.getNhanSuYTe().getId(), p -> p, (a, b) -> a));
    }

    /** Với nhân sự đã thanh toán: hiển thị đúng số liệu đã chốt trên phiếu, không tính lại. */
    private static ThanhToanDtos.BangTinhLuong luongTuPhieu(PhieuThanhToan p, ThanhToanDtos.BangTinhLuong hienTai) {
        BigDecimal tienChinh = p.getDonGiaCaApDung().multiply(BigDecimal.valueOf(p.getSoCaChinhGoc()));
        BigDecimal tienNgoaiGio = p.getDonGiaCaApDung().multiply(p.getHeSoNgoaiGioApDung()).multiply(BigDecimal.valueOf(p.getSoCaNgoaiGioGoc()));
        BigDecimal dieuChinh = p.getTongThucLinh().subtract(tienChinh).subtract(tienNgoaiGio);
        return new ThanhToanDtos.BangTinhLuong(p.getSoCaChinhGoc(), p.getSoCaNgoaiGioGoc(), hienTai.soCaKhongTinh(),
                p.getDonGiaCaApDung(), p.getHeSoNgoaiGioApDung(), tienChinh, tienNgoaiGio, dieuChinh,
                p.getTongThucLinh(), hienTai.soKhieuNaiChoXuLy());
    }

    private ThanhToanDtos.PhieuLuong toPhieuDto(PhieuThanhToan p) {
        List<YeuCauKhieuNai> dieuChinh = yeuCauKhieuNaiRepository.findTheoNhanSuTuan(p.getNhanSuYTe().getId(), p.getTuanLamViec().getId())
                .stream()
                .filter(k -> KhieuNaiService.DA_XU_LY.equals(k.getTrangThai()))
                .filter(k -> k.getPhieuThanhToan() == null || Objects.equals(k.getPhieuThanhToan().getId(), p.getId()))
                .toList();
        return toPhieuDto(p, dieuChinh);
    }

    private ThanhToanDtos.PhieuLuong toPhieuDto(PhieuThanhToan p, List<YeuCauKhieuNai> dieuChinh) {
        NhanSuYTe ns = p.getNhanSuYTe();
        TuanLamViec t = p.getTuanLamViec();
        BigDecimal donGia = p.getDonGiaCaApDung();
        BigDecimal heSo = p.getHeSoNgoaiGioApDung();
        return new ThanhToanDtos.PhieuLuong(
                p.getId(), p.getMaPhieu(),
                p.getTrangThai() != null ? p.getTrangThai().name() : null,
                p.getNgayLap(), p.getNgayThanhToan(),
                p.getHinhThucThanhToan() != null ? p.getHinhThucThanhToan().name() : null,
                p.getMaGiaoDich(),
                t.getId(), t.getTen(), t.getNgayBatDau(), t.getNgayKetThuc(),
                ns.getId(), HoSoMapper.maNhanSu(ns), ns.getHoVaTen(), HoSoMapper.chucDanh(ns), HoSoMapper.hocVi(ns),
                ns.getKhoa() != null ? ns.getKhoa().getTen() : null,
                p.getSoTaiKhoanNhan(), p.getNganHangNhan(), p.getChuTaiKhoanNhan(),
                p.getSoCaChinhGoc(), p.getSoCaNgoaiGioGoc(),
                donGia, heSo,
                donGia.multiply(BigDecimal.valueOf(p.getSoCaChinhGoc())),
                donGia.multiply(heSo).multiply(BigDecimal.valueOf(p.getSoCaNgoaiGioGoc())),
                p.getPhuCapTrachNhiem(), p.getThuong(), p.getPhat(),
                dieuChinh.stream().map(KhieuNaiService::toDieuChinh).toList(),
                p.getTongThucLinh(),
                p.getKeToan() != null ? p.getKeToan().getHoVaTen() : null);
    }
}
