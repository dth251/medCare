package com.medcare.clinic.service;

import com.medcare.clinic.dto.phancong.PhanCongDtos;
import com.medcare.clinic.dto.phancong.PhanCongRequest;
import com.medcare.clinic.entity.*;
import com.medcare.clinic.enums.TrangThaiBangPhanCong;
import com.medcare.clinic.enums.TrangThaiTuan;
import com.medcare.clinic.enums.VaiTroPhanCong;
import com.medcare.clinic.exception.BusinessRuleException;
import com.medcare.clinic.exception.ResourceNotFoundException;
import com.medcare.clinic.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhanCongService {

    private static final int SO_VI_TRI_MOI_CA = VaiTroPhanCong.values().length;

    private final BangPhanCongRepository bangPhanCongRepository;
    private final PhanCongCaRepository phanCongCaRepository;
    private final CaTrucKhoaRepository caTrucKhoaRepository;
    private final NhanSuYTeRepository nhanSuYTeRepository;
    private final NhanVienRepository nhanVienRepository;
    private final DangKyCaRepository dangKyCaRepository;
    private final TuanLamViecRepository tuanLamViecRepository;
    private final KhoaRepository khoaRepository;

    // =====================================================================
    // GHI: phân công / hủy phân công / công bố
    // =====================================================================

    @Transactional
    public PhanCongDtos.Slot phanCong(String managerUsername, PhanCongRequest request) {
        NhanVien manager = nhanVienRepository.findByTenDangNhap(managerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người quản lý"));
        CaTrucKhoa ca = caTrucKhoaRepository.findById(request.getCaTrucKhoaId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca trực khoa"));
        NhanSuYTe nhanSu = nhanSuYTeRepository.findById(request.getNhanSuId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự"));

        TuanLamViec tuan = ca.getNgayTrongTuan().getTuanLamViec();
        if (tuan.getTrangThai() == TrangThaiTuan.KET_THUC) {
            throw new BusinessRuleException("Tuần làm việc đã kết thúc, không thể phân công!");
        }

        // Vị trí phải đúng chức danh
        if (request.getVaiTro() == VaiTroPhanCong.BAC_SI_CHINH && !HoSoMapper.laBacSi(nhanSu)) {
            throw new BusinessRuleException("Vị trí Bác sĩ chính phải do Bác sĩ đảm nhiệm!");
        }
        if ((request.getVaiTro() == VaiTroPhanCong.Y_TA_1 || request.getVaiTro() == VaiTroPhanCong.Y_TA_2) && !HoSoMapper.laYTa(nhanSu)) {
            throw new BusinessRuleException("Vị trí Y tá trực phải do Y tá đảm nhiệm!");
        }

        BangPhanCong bang = layHoacTaoBang(ca.getKhoa(), tuan, manager);
        if (bang.getTrangThai() == TrangThaiBangPhanCong.DA_CONG_BO) {
            throw new BusinessRuleException("Lịch của khoa đã được công bố, không thể chỉnh sửa!");
        }

        Optional<PhanCongCa> hienTai = phanCongCaRepository.findByCaTrucKhoaIdAndVaiTro(ca.getId(), request.getVaiTro());
        boolean cungNguoi = hienTai.map(pc -> Objects.equals(pc.getNhanSuYTe().getId(), nhanSu.getId())).orElse(false);

        if (!cungNguoi) {
            boolean daCoTrongCa = phanCongCaRepository.findByCaTrucKhoaId(ca.getId()).stream()
                    .anyMatch(pc -> Objects.equals(pc.getNhanSuYTe().getId(), nhanSu.getId()));
            if (daCoTrongCa) {
                throw new BusinessRuleException(nhanSu.getHoVaTen() + " đã được phân công một vị trí khác trong ca này!");
            }
            long soCaTrongNgay = phanCongCaRepository.countByNhanSuYTeIdAndCaTrucKhoa_NgayTrongTuan_Id(
                    nhanSu.getId(), ca.getNgayTrongTuan().getId());
            if (soCaTrongNgay >= tuan.getSoCaToiDaMoiNgay()) {
                throw new BusinessRuleException(nhanSu.getHoVaTen() + " đã đủ " + tuan.getSoCaToiDaMoiNgay() + " ca trong ngày này!");
            }
        }

        boolean daDangKy = dangKyCaRepository.daDangKy(nhanSu.getId(), ca.getId());
        boolean laDieuDong = !daDangKy;
        if (laDieuDong && !request.isDieuDongNgoaiDangKy()) {
            throw new BusinessRuleException(nhanSu.getHoVaTen()
                    + " không đăng ký ca này. Cần xác nhận điều động ngoài đăng ký!");
        }

        PhanCongCa phanCong = hienTai.orElseGet(() -> {
            PhanCongCa moi = PhanCongCa.builder()
                    .bangPhanCong(bang)
                    .caTrucKhoa(ca)
                    .vaiTro(request.getVaiTro())
                    .build();
            bang.getChiTietPhanCong().add(moi);
            return moi;
        });
        phanCong.setNhanSuYTe(nhanSu);
        phanCong.setDieuDongNgoaiDangKy(laDieuDong);
        phanCong.setGhiChu(request.getGhiChu());

        PhanCongCa saved = phanCongCaRepository.save(phanCong);
        capNhatTrangThaiBang(bang);
        return toSlot(saved);
    }

    @Transactional
    public void huyPhanCong(Long phanCongId) {
        PhanCongCa pc = phanCongCaRepository.findById(phanCongId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công"));
        BangPhanCong bang = pc.getBangPhanCong();
        if (bang.getTrangThai() == TrangThaiBangPhanCong.DA_CONG_BO) {
            throw new BusinessRuleException("Lịch của khoa đã được công bố, không thể chỉnh sửa!");
        }
        bang.getChiTietPhanCong().remove(pc);
        phanCongCaRepository.delete(pc);
        bang.setTrangThai(TrangThaiBangPhanCong.DANG_XEP);
        bangPhanCongRepository.save(bang);
    }

    @Transactional
    public PhanCongDtos.CongBoKetQua congBoLichTuan(Long tuanId) {
        TuanLamViec tuan = tuanLamViecRepository.findById(tuanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuần làm việc"));

        List<PhanCongDtos.KhoaTienDo> tienDo = tongQuan(tuanId).stream()
                .filter(k -> k.tongSoCa() > 0)
                .toList();
        if (tienDo.isEmpty()) {
            throw new BusinessRuleException("Tuần này chưa có ca trực nào để công bố!");
        }
        List<String> chuaDu = tienDo.stream()
                .filter(k -> k.soCaDaDu() < k.tongSoCa())
                .map(k -> k.maKhoa() + " (" + k.soCaDaDu() + "/" + k.tongSoCa() + ")")
                .toList();
        if (!chuaDu.isEmpty()) {
            throw new BusinessRuleException("Chưa thể công bố: các khoa chưa xếp đủ ca – " + String.join(", ", chuaDu));
        }

        LocalDateTime now = LocalDateTime.now();
        List<BangPhanCong> dsBang = bangPhanCongRepository.findByTuanLamViecId(tuanId);
        for (BangPhanCong b : dsBang) {
            b.setTrangThai(TrangThaiBangPhanCong.DA_CONG_BO);
            b.setNgayCongBo(now);
        }
        bangPhanCongRepository.saveAll(dsBang);
        tuan.setTrangThai(TrangThaiTuan.DA_CONG_BO);
        tuanLamViecRepository.save(tuan);

        int tongSoCa = tienDo.stream().mapToInt(PhanCongDtos.KhoaTienDo::tongSoCa).sum();
        return new PhanCongDtos.CongBoKetQua(tuanId, tienDo.size(), tongSoCa, now);
    }

    // =====================================================================
    // ĐỌC: tổng quan, lịch khoa, ứng viên
    // =====================================================================

    @Transactional(readOnly = true)
    public List<PhanCongDtos.KhoaTienDo> tongQuan(Long tuanId) {
        if (!tuanLamViecRepository.existsById(tuanId)) {
            throw new ResourceNotFoundException("Không tìm thấy tuần làm việc");
        }
        Map<Long, List<CaTrucKhoa>> caTheoKhoa = caTrucKhoaRepository.timTheoTuan(tuanId).stream()
                .collect(Collectors.groupingBy(c -> c.getKhoa().getId()));
        Map<Long, Set<VaiTroPhanCong>> viTriTheoCa = viTriTheoCa(phanCongCaRepository.findByBangPhanCong_TuanLamViec_Id(tuanId));
        Map<Long, BangPhanCong> bangTheoKhoa = bangPhanCongRepository.findByTuanLamViecId(tuanId).stream()
                .collect(Collectors.toMap(b -> b.getKhoa().getId(), b -> b, (a, b) -> a));
        Map<Long, List<NhanSuYTe>> nhanSuTheoKhoa = nhanSuYTeRepository.findAll().stream()
                .filter(n -> n.getKhoa() != null)
                .collect(Collectors.groupingBy(n -> n.getKhoa().getId()));

        List<PhanCongDtos.KhoaTienDo> result = new ArrayList<>();
        for (Khoa khoa : khoaRepository.findAll(Sort.by("maKhoa"))) {
            List<CaTrucKhoa> dsCa = caTheoKhoa.getOrDefault(khoa.getId(), List.of());
            int soCaDaDu = (int) dsCa.stream().filter(c -> laCaDu(viTriTheoCa.get(c.getId()))).count();
            List<NhanSuYTe> ns = nhanSuTheoKhoa.getOrDefault(khoa.getId(), List.of());
            BangPhanCong bang = bangTheoKhoa.get(khoa.getId());
            result.add(new PhanCongDtos.KhoaTienDo(
                    khoa.getId(), khoa.getMaKhoa(), khoa.getTen(),
                    ns.stream().filter(HoSoMapper::laBacSi).count(),
                    ns.stream().filter(HoSoMapper::laYTa).count(),
                    dsCa.size(), soCaDaDu,
                    bang != null ? bang.getTrangThai().name() : null,
                    bang != null ? bang.getNgayCongBo() : null));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PhanCongDtos.LichKhoa layLichKhoa(Long khoaId, Long tuanId) {
        Khoa khoa = khoaRepository.findById(khoaId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khoa"));
        TuanLamViec tuan = tuanLamViecRepository.findById(tuanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuần làm việc"));

        List<CaTrucKhoa> dsCa = caTrucKhoaRepository.timTheoKhoaTuan(khoaId, tuanId);
        Optional<BangPhanCong> bang = bangPhanCongRepository.findByKhoaIdAndTuanLamViecId(khoaId, tuanId);
        Map<Long, List<PhanCongCa>> pcTheoCa = bang
                .map(b -> phanCongCaRepository.findByBangPhanCongId(b.getId()))
                .orElse(List.of()).stream()
                .collect(Collectors.groupingBy(pc -> pc.getCaTrucKhoa().getId()));

        int soCaDaDu = 0;
        List<PhanCongDtos.CaPhanCong> danhSach = new ArrayList<>();
        for (CaTrucKhoa ca : dsCa) {
            Map<VaiTroPhanCong, PhanCongCa> theoVaiTro = pcTheoCa.getOrDefault(ca.getId(), List.of()).stream()
                    .collect(Collectors.toMap(PhanCongCa::getVaiTro, pc -> pc, (a, b) -> a));
            if (theoVaiTro.size() == SO_VI_TRI_MOI_CA) soCaDaDu++;
            danhSach.add(new PhanCongDtos.CaPhanCong(
                    HoSoMapper.caTruc(ca),
                    toSlotOrNull(theoVaiTro.get(VaiTroPhanCong.BAC_SI_CHINH)),
                    toSlotOrNull(theoVaiTro.get(VaiTroPhanCong.Y_TA_1)),
                    toSlotOrNull(theoVaiTro.get(VaiTroPhanCong.Y_TA_2))));
        }

        return new PhanCongDtos.LichKhoa(khoa.getId(), khoa.getMaKhoa(), khoa.getTen(),
                tuan.getId(), tuan.getTen(),
                bang.map(b -> b.getTrangThai().name()).orElse(null),
                dsCa.size(), soCaDaDu, danhSach);
    }

    /** Danh sách ứng viên phù hợp chức danh cho một vị trí; cùng khoa và đã đăng ký được xếp trước. */
    @Transactional(readOnly = true)
    public List<PhanCongDtos.UngVien> danhSachUngVien(Long caTrucKhoaId, VaiTroPhanCong vaiTro) {
        CaTrucKhoa ca = caTrucKhoaRepository.findById(caTrucKhoaId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca trực khoa"));
        TuanLamViec tuan = ca.getNgayTrongTuan().getTuanLamViec();
        boolean canBacSi = vaiTro == VaiTroPhanCong.BAC_SI_CHINH;

        Set<Long> daDangKy = dangKyCaRepository.findByCaTrucKhoaId(caTrucKhoaId).stream()
                .map(d -> d.getPhieuDangKy().getNhanSuYTe().getId())
                .collect(Collectors.toSet());
        Set<Long> trongCa = phanCongCaRepository.findByCaTrucKhoaId(caTrucKhoaId).stream()
                .map(pc -> pc.getNhanSuYTe().getId())
                .collect(Collectors.toSet());

        return nhanSuYTeRepository.findAll().stream()
                .filter(n -> canBacSi ? HoSoMapper.laBacSi(n) : HoSoMapper.laYTa(n))
                .map(n -> {
                    boolean cungKhoa = n.getKhoa() != null && Objects.equals(n.getKhoa().getId(), ca.getKhoa().getId());
                    return new PhanCongDtos.UngVien(
                            n.getId(), HoSoMapper.maNhanSu(n), n.getHoVaTen(),
                            HoSoMapper.chucDanh(n), HoSoMapper.hocVi(n),
                            n.getKhoa() != null ? n.getKhoa().getId() : null,
                            n.getKhoa() != null ? n.getKhoa().getTen() : null,
                            cungKhoa,
                            daDangKy.contains(n.getId()),
                            phanCongCaRepository.countByNhanSuYTeIdAndCaTrucKhoa_NgayTrongTuan_Id(n.getId(), ca.getNgayTrongTuan().getId()),
                            tuan.getSoCaToiDaMoiNgay(),
                            trongCa.contains(n.getId()));
                })
                .sorted(Comparator.comparing(PhanCongDtos.UngVien::daDangKy).reversed()
                        .thenComparing(Comparator.comparing(PhanCongDtos.UngVien::cungKhoa).reversed())
                        .thenComparing(PhanCongDtos.UngVien::maNhanSu))
                .toList();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private BangPhanCong layHoacTaoBang(Khoa khoa, TuanLamViec tuan, NhanVien manager) {
        return bangPhanCongRepository.findByKhoaIdAndTuanLamViecId(khoa.getId(), tuan.getId())
                .orElseGet(() -> bangPhanCongRepository.save(BangPhanCong.builder()
                        .maBang("BPC-" + khoa.getMaKhoa() + "-T" + tuan.getId())
                        .khoa(khoa)
                        .tuanLamViec(tuan)
                        .nguoiLap(manager)
                        .ngayLap(LocalDateTime.now())
                        .trangThai(TrangThaiBangPhanCong.DANG_XEP)
                        .build()));
    }

    private void capNhatTrangThaiBang(BangPhanCong bang) {
        if (bang.getKhoa() == null || bang.getTuanLamViec() == null) return;
        List<CaTrucKhoa> dsCa = caTrucKhoaRepository.timTheoKhoaTuan(bang.getKhoa().getId(), bang.getTuanLamViec().getId());
        Map<Long, Set<VaiTroPhanCong>> viTri = viTriTheoCa(bang.getChiTietPhanCong());
        boolean du = !dsCa.isEmpty() && dsCa.stream().allMatch(c -> laCaDu(viTri.get(c.getId())));
        bang.setTrangThai(du ? TrangThaiBangPhanCong.DA_DU : TrangThaiBangPhanCong.DANG_XEP);
        bangPhanCongRepository.save(bang);
    }

    private static Map<Long, Set<VaiTroPhanCong>> viTriTheoCa(Collection<PhanCongCa> ds) {
        Map<Long, Set<VaiTroPhanCong>> map = new HashMap<>();
        for (PhanCongCa pc : ds) {
            map.computeIfAbsent(pc.getCaTrucKhoa().getId(), k -> EnumSet.noneOf(VaiTroPhanCong.class)).add(pc.getVaiTro());
        }
        return map;
    }

    private static boolean laCaDu(Set<VaiTroPhanCong> viTri) {
        return viTri != null && viTri.size() == SO_VI_TRI_MOI_CA;
    }

    private static PhanCongDtos.Slot toSlotOrNull(PhanCongCa pc) {
        return pc == null ? null : toSlot(pc);
    }

    private static PhanCongDtos.Slot toSlot(PhanCongCa pc) {
        NhanSuYTe ns = pc.getNhanSuYTe();
        return new PhanCongDtos.Slot(pc.getId(), ns.getId(), HoSoMapper.maNhanSu(ns), ns.getHoVaTen(),
                pc.getVaiTro().name(), pc.isDieuDongNgoaiDangKy(), pc.getGhiChu());
    }
}
