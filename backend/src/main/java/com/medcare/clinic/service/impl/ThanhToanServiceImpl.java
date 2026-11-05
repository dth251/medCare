package com.medcare.clinic.service.impl;

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
import com.medcare.clinic.mapper.ChamCongMapper;
import com.medcare.clinic.mapper.ThanhToanMapper;
import com.medcare.clinic.repository.*;
import com.medcare.clinic.service.HoSoMapper;
import com.medcare.clinic.service.ThanhToanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ThanhToanServiceImpl implements ThanhToanService {

    private static final Set<TrangThaiChamCong> KHONG_TINH_CONG = Set.of(
            TrangThaiChamCong.VANG_MAT,
            TrangThaiChamCong.LOI_THIET_BI
    );

    private final PhieuThanhToanRepository phieuThanhToanRepository;
    private final NhanSuYTeRepository nhanSuYTeRepository;
    private final TuanLamViecRepository tuanLamViecRepository;
    private final NhanVienRepository nhanVienRepository;
    private final BangPhanCongRepository bangPhanCongRepository;
    private final ChamCongRepository chamCongRepository;
    private final YeuCauKhieuNaiRepository yeuCauKhieuNaiRepository;
    private final KhoaRepository khoaRepository;
    private final ChamCongMapper chamCongMapper;
    private final ThanhToanMapper thanhToanMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ThanhToanDtos.KhoaThanhToan> danhSachKhoa(Long tuanId) {
        tuanLamViecRepository.findById(tuanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuần làm việc"));

        List<Khoa> dsKhoa = khoaRepository.findAll(Sort.by("id"));
        List<ThanhToanDtos.KhoaThanhToan> ketQua = new ArrayList<>();
        for (Khoa k : dsKhoa) {
            List<NhanSuYTe> dsNhanSu = nhanSuYTeRepository.findByKhoaId(k.getId());
            int soNhanSu = dsNhanSu.size();
            int soDaThanhToan = 0;
            BigDecimal tongTien = BigDecimal.ZERO;

            for (NhanSuYTe ns : dsNhanSu) {
                Optional<PhieuThanhToan> ptt = phieuThanhToanRepository.findByNhanSuYTeIdAndTuanLamViecId(ns.getId(), tuanId);
                if (ptt.isPresent() && ptt.get().getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
                    soDaThanhToan++;
                    tongTien = tongTien.add(ptt.get().getTongThucLinh());
                } else {
                    ThanhToanDtos.BangTinhLuong luong = tinhLuong(ns, tuanId);
                    tongTien = tongTien.add(luong.tongThucLinh());
                }
            }
            ketQua.add(new ThanhToanDtos.KhoaThanhToan(k.getId(), k.getMaKhoa(), k.getTen(), soNhanSu, soDaThanhToan, tongTien));
        }
        return ketQua;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThanhToanDtos.NhanSuLuong> danhSachNhanSu(Long tuanId, Long khoaId) {
        tuanLamViecRepository.findById(tuanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuần làm việc"));
        khoaRepository.findById(khoaId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khoa"));

        List<NhanSuYTe> dsNhanSu = nhanSuYTeRepository.findByKhoaId(khoaId);
        List<ThanhToanDtos.NhanSuLuong> ketQua = new ArrayList<>();
        for (NhanSuYTe ns : dsNhanSu) {
            Optional<PhieuThanhToan> ptt = phieuThanhToanRepository.findByNhanSuYTeIdAndTuanLamViecId(ns.getId(), tuanId);
            ThanhToanDtos.BangTinhLuong luong = tinhLuong(ns, tuanId);

            String trangThai = ptt.isPresent() && ptt.get().getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN
                    ? TrangThaiThanhToan.DA_THANH_TOAN.name()
                    : TrangThaiThanhToan.CHUA_THANH_TOAN.name();
            Long phieuId = ptt.map(PhieuThanhToan::getId).orElse(null);

            ketQua.add(new ThanhToanDtos.NhanSuLuong(
                    ns.getId(),
                    HoSoMapper.maNhanSu(ns),
                    ns.getHoVaTen(),
                    HoSoMapper.chucDanh(ns),
                    HoSoMapper.hocVi(ns),
                    luong,
                    trangThai,
                    phieuId));
        }
        return ketQua;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChamCongDTO> layBangChamCong(String username, boolean laKeToan, Long nhanSuId, Long tuanId) {
        NhanSuYTe target = nhanSuYTeRepository.findById(nhanSuId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự"));
        if (!laKeToan && !username.equals(target.getTenDangNhap())) {
            throw new AccessDeniedException("Bạn không có quyền xem chấm công của nhân sự khác");
        }
        return bangChamCongChiTiet(nhanSuId, tuanId);
    }

    @Transactional(readOnly = true)
    public List<ChamCongDTO> bangChamCongChiTiet(Long nhanSuId, Long tuanId) {
        nhanSuYTeRepository.findById(nhanSuId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự"));
        tuanLamViecRepository.findById(tuanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuần làm việc"));

        List<ChamCongDTO> list = new ArrayList<>();
        List<BangPhanCong> dsPhanCong = bangPhanCongRepository.findByNhanSuYTeIdAndTuanLamViecId(nhanSuId, tuanId);

        for (BangPhanCong pc : dsPhanCong) {
            Optional<ChamCong> ccOpt = chamCongRepository.findByBangPhanCongId(pc.getId());
            Optional<YeuCauKhieuNai> knOpt = yeuCauKhieuNaiRepository.findByBangPhanCongId(pc.getId()).stream()
                    .max(Comparator.comparing(YeuCauKhieuNai::getNgayGui));

            list.add(chamCongMapper.toDto(pc, ccOpt.orElse(null), knOpt.orElse(null)));
        }

        list.sort(Comparator.comparing(ChamCongDTO::getNgay).thenComparing(ChamCongDTO::getTenCa));
        return list;
    }

    @Transactional(readOnly = true)
    public ThanhToanDtos.BangTinhLuong tinhLuong(NhanSuYTe nhanSu, Long tuanId) {
        BigDecimal donGia = nhanSu.getDonGiaTienCong() != null ? nhanSu.getDonGiaTienCong() : BigDecimal.ZERO;
        BigDecimal heSoNgoaiGio = HE_SO_NGOAI_GIO_MAC_DINH;

        int soCaChinh = 0;
        int soCaNgoaiGio = 0;
        int soCaKhongTinh = 0;

        List<BangPhanCong> dsPhanCong = bangPhanCongRepository.findByNhanSuYTeIdAndTuanLamViecId(nhanSu.getId(), tuanId);
        for (BangPhanCong pc : dsPhanCong) {
            Optional<ChamCong> cc = chamCongRepository.findByBangPhanCongId(pc.getId());
            if (cc.isPresent() && KHONG_TINH_CONG.contains(cc.get().getTrangThai())) {
                soCaKhongTinh++;
                continue;
            }
            if (pc.getCaTrucKhoa() != null && pc.getCaTrucKhoa().getCaLamViec() != null) {
                CaLamViec clv = pc.getCaTrucKhoa().getCaLamViec();
                if (clv.getLoaiCa() == LoaiCa.NGOAI_GIO) {
                    soCaNgoaiGio++;
                } else {
                    soCaChinh++;
                }
            } else {
                soCaChinh++;
            }
        }

        BigDecimal tienChinh = donGia.multiply(BigDecimal.valueOf(soCaChinh));
        BigDecimal tienOT = donGia.multiply(heSoNgoaiGio).multiply(BigDecimal.valueOf(soCaNgoaiGio));

        List<YeuCauKhieuNai> dsKhieuNai = yeuCauKhieuNaiRepository.findTheoNhanSuTuan(nhanSu.getId(), tuanId);
        int choXuLy = (int) dsKhieuNai.stream().filter(k -> "CHO_XU_LY".equals(k.getTrangThai())).count();

        BigDecimal tienDieuChinh = dsKhieuNai.stream()
                .filter(k -> "DA_XU_LY".equals(k.getTrangThai()))
                .map(YeuCauKhieuNai::getSoTienDieuChinh)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tong = tienChinh.add(tienOT).add(tienDieuChinh);
        if (tong.compareTo(BigDecimal.ZERO) < 0) tong = BigDecimal.ZERO;

        return new ThanhToanDtos.BangTinhLuong(
                soCaChinh,
                soCaNgoaiGio,
                soCaKhongTinh,
                donGia,
                heSoNgoaiGio,
                tienChinh,
                tienOT,
                tienDieuChinh,
                tong,
                choXuLy);
    }

    @Override
    @Transactional
    public ThanhToanDtos.PhieuLuong thanhToan(String accountantUsername, ThanhToanRequest request) {
        NhanVien keToan = nhanVienRepository.findByTenDangNhap(accountantUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kế toán"));
        NhanSuYTe nhanSu = nhanSuYTeRepository.findById(request.getNhanSuId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự y tế"));
        TuanLamViec tuan = tuanLamViecRepository.findById(request.getTuanId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuần làm việc"));

        Optional<PhieuThanhToan> daCo = phieuThanhToanRepository.findByNhanSuYTeIdAndTuanLamViecId(nhanSu.getId(), tuan.getId());
        if (daCo.isPresent() && daCo.get().getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
            throw new BusinessRuleException("Nhân sự " + nhanSu.getHoVaTen() + " đã được thanh toán " + tuan.getTen() + "!");
        }
        if (nhanSu.getDonGiaTienCong() == null) {
            throw new BusinessRuleException("Nhân sự " + nhanSu.getHoVaTen() + " chưa được cấu hình đơn giá ca!");
        }

        ThanhToanDtos.BangTinhLuong luong = tinhLuong(nhanSu, tuan.getId());
        if (luong.soKhieuNaiChoXuLy() > 0) {
            throw new BusinessRuleException(String.format("Còn %d khiếu nại chưa xử lý của %s trong %s! Phải xử lý trước khi chi trả.",
                    luong.soKhieuNaiChoXuLy(), nhanSu.getHoVaTen(), tuan.getTen()));
        }

        LocalDateTime now = LocalDateTime.now();
        HinhThucThanhToan hinhThuc = request.getHinhThucThanhToan() != null
                ? request.getHinhThucThanhToan()
                : (nhanSu.getSoTaiKhoan() != null && !nhanSu.getSoTaiKhoan().isBlank() ? HinhThucThanhToan.TRUC_TUYEN : HinhThucThanhToan.TRUC_TIEP);

        String maGD = hinhThuc == HinhThucThanhToan.TRUC_TUYEN
                ? (request.getMaGiaoDich() != null && !request.getMaGiaoDich().isBlank()
                ? request.getMaGiaoDich()
                : "TX-" + System.currentTimeMillis())
                : null;

        List<YeuCauKhieuNai> dsKhieuNai = yeuCauKhieuNaiRepository.findTheoNhanSuTuan(nhanSu.getId(), tuan.getId());
        YeuCauKhieuNai knDaXuLy = dsKhieuNai.stream()
                .filter(k -> "DA_XU_LY".equals(k.getTrangThai()))
                .findFirst()
                .orElse(null);

        PhieuThanhToan phieu = daCo.orElseGet(() -> PhieuThanhToan.builder()
                .maPhieu("PL-" + tuan.getId() + "-" + nhanSu.getId())
                .nhanSuYTe(nhanSu)
                .tuanLamViec(tuan)
                .ngayLap(now)
                .build());

        phieu.setKeToan(keToan);
        phieu.setNgayThanhToan(now);
        phieu.setTrangThai(TrangThaiThanhToan.DA_THANH_TOAN);
        phieu.setHinhThucThanhToan(hinhThuc);
        phieu.setMaGiaoDich(maGD);
        phieu.setSoTaiKhoanNhan(nhanSu.getSoTaiKhoan());
        phieu.setNganHangNhan(nhanSu.getTenNganHang());
        phieu.setChuTaiKhoanNhan(nhanSu.getChuTaiKhoan());
        phieu.setSoCaChinhGoc(luong.soCaChinh());
        phieu.setSoCaNgoaiGioGoc(luong.soCaNgoaiGio());
        phieu.setDonGiaCaApDung(luong.donGiaCa());
        phieu.setHeSoNgoaiGioApDung(luong.heSoNgoaiGio());
        phieu.setPhuCapTrachNhiem(BigDecimal.ZERO);
        phieu.setThuong(BigDecimal.ZERO);
        phieu.setPhat(BigDecimal.ZERO);
        phieu.setTongThucLinh(luong.tongThucLinh());
        phieu.setYeuCauKhieuNai(knDaXuLy);

        PhieuThanhToan saved = phieuThanhToanRepository.save(phieu);
        return thanhToanMapper.toPhieuLuong(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThanhToanDtos.PhieuLuong> layDanhSachPhieuTuan(Long tuanId) {
        return phieuThanhToanRepository.findByTuanLamViecId(tuanId).stream()
                .map(thanhToanMapper::toPhieuLuong)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ThanhToanDtos.PhieuLuong getPhieu(Long phieuId, String username, boolean laKeToan) {
        PhieuThanhToan p = phieuThanhToanRepository.findById(phieuId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu thanh toán"));
        if (!laKeToan && (p.getNhanSuYTe() == null || !username.equals(p.getNhanSuYTe().getTenDangNhap()))) {
            throw new AccessDeniedException("Bạn không có quyền xem phiếu lương này");
        }
        return thanhToanMapper.toPhieuLuong(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThanhToanDtos.PhieuLuong> getTatCaPhieuCuaToi(String username) {
        NhanSuYTe nhanSu = nhanSuYTeRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự"));
        return phieuThanhToanRepository.findByNhanSuYTeIdOrderByNgayLapDesc(nhanSu.getId()).stream()
                .map(thanhToanMapper::toPhieuLuong)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ThanhToanDtos.PhieuLuong getPhieuCuaToi(String username, Long tuanId) {
        NhanSuYTe nhanSu = nhanSuYTeRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự"));
        PhieuThanhToan p = phieuThanhToanRepository.findByNhanSuYTeIdAndTuanLamViecId(nhanSu.getId(), tuanId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có phiếu lương cho tuần này"));
        return thanhToanMapper.toPhieuLuong(p);
    }
}
