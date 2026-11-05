package com.medcare.clinic.service.impl;

import com.medcare.clinic.dto.phancong.PhanCongDtos;
import com.medcare.clinic.dto.phancong.PhanCongRequest;
import com.medcare.clinic.entity.*;
import com.medcare.clinic.enums.TrangThaiBangPhanCong;
import com.medcare.clinic.enums.TrangThaiTuan;
import com.medcare.clinic.enums.VaiTroPhanCong;
import com.medcare.clinic.exception.BusinessRuleException;
import com.medcare.clinic.exception.ResourceNotFoundException;
import com.medcare.clinic.mapper.BangPhanCongMapper;
import com.medcare.clinic.repository.*;
import com.medcare.clinic.service.HoSoMapper;
import com.medcare.clinic.service.PhanCongService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhanCongServiceImpl implements PhanCongService {

    private static final int SO_VI_TRI_MOI_CA = VaiTroPhanCong.values().length;

    private final BangPhanCongRepository bangPhanCongRepository;
    private final CaTrucKhoaRepository caTrucKhoaRepository;
    private final NhanSuYTeRepository nhanSuYTeRepository;
    private final NhanVienRepository nhanVienRepository;
    private final DangKyCaRepository dangKyCaRepository;
    private final TuanLamViecRepository tuanLamViecRepository;
    private final KhoaRepository khoaRepository;
    private final BangPhanCongMapper bangPhanCongMapper;

    @Override
    @Transactional
    public PhanCongDtos.Slot phanCong(String managerUsername, PhanCongRequest request) {
        NhanVien manager = nhanVienRepository.findByTenDangNhap(managerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người quản lý"));
        CaTrucKhoa ca = caTrucKhoaRepository.findById(request.getCaTrucKhoaId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca trực khoa"));
        NhanSuYTe nhanSu = nhanSuYTeRepository.findById(request.getNhanSuId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự"));

        TuanLamViec tuan = ca.getNgayTrongTuan().getTuanLamViec();
        if (tuan.getTrangThai() == TrangThaiTuan.DA_CONG_BO || tuan.getTrangThai() == TrangThaiTuan.KET_THUC) {
            throw new BusinessRuleException("Lịch làm việc đã được công bố hoặc tuần đã kết thúc, không thể phân công!");
        }

        if (request.getVaiTro() == VaiTroPhanCong.BAC_SI_CHINH && !HoSoMapper.laBacSi(nhanSu)) {
            throw new BusinessRuleException("Vị trí Bác sĩ chính phải do Bác sĩ đảm nhiệm!");
        }
        if ((request.getVaiTro() == VaiTroPhanCong.Y_TA_1 || request.getVaiTro() == VaiTroPhanCong.Y_TA_2) && !HoSoMapper.laYTa(nhanSu)) {
            throw new BusinessRuleException("Vị trí Y tá trực phải do Y tá đảm nhiệm!");
        }

        Optional<BangPhanCong> hienTai = bangPhanCongRepository.findByCaTrucKhoaIdAndVaiTro(ca.getId(), request.getVaiTro());
        if (hienTai.isPresent() && hienTai.get().getTrangThai() == TrangThaiBangPhanCong.DA_CONG_BO) {
            throw new BusinessRuleException("Lịch của ca đã được công bố, không thể chỉnh sửa!");
        }
        boolean cungNguoi = hienTai.map(pc -> pc.getNhanSuYTe() != null && Objects.equals(pc.getNhanSuYTe().getId(), nhanSu.getId())).orElse(false);

        if (!cungNguoi) {
            boolean daCoTrongCa = bangPhanCongRepository.findByCaTrucKhoaId(ca.getId()).stream()
                    .anyMatch(pc -> Objects.equals(pc.getNhanSuYTe().getId(), nhanSu.getId()));
            if (daCoTrongCa) {
                throw new BusinessRuleException(nhanSu.getHoVaTen() + " đã được phân công một vị trí khác trong ca này!");
            }
            long soCaTrongNgay = bangPhanCongRepository.countByNhanSuYTeIdAndCaTrucKhoa_NgayTrongTuan_Id(
                    nhanSu.getId(), ca.getNgayTrongTuan().getId());
            if (soCaTrongNgay >= tuan.getSoCaToiDaMoiNgay()) {
                throw new BusinessRuleException(nhanSu.getHoVaTen() + " đã đủ " + tuan.getSoCaToiDaMoiNgay()
                        + " ca trực trong ngày " + ca.getNgayTrongTuan().getNgay() + "!");
            }
        }

        boolean daDangKy = dangKyCaRepository.daDangKy(nhanSu.getId(), ca.getId());
        if (!daDangKy && !request.isDieuDongNgoaiDangKy()) {
            throw new BusinessRuleException(nhanSu.getHoVaTen() + " chưa đăng ký ca này! Cần xác nhận điều động ngoài đăng ký.");
        }
        boolean dieuDongNgoai = !daDangKy;

        BangPhanCong bpc = hienTai.orElseGet(() -> BangPhanCong.builder()
                .caTrucKhoa(ca)
                .vaiTro(request.getVaiTro())
                .khoa(ca.getKhoa())
                .tuanLamViec(tuan)
                .ngayLap(LocalDateTime.now())
                .trangThai(TrangThaiBangPhanCong.DANG_XEP)
                .build());

        bpc.setMaBang("BPC-" + ca.getKhoa().getMaKhoa() + "-T" + tuan.getId() + "-C" + ca.getId() + "-" + request.getVaiTro().name());
        bpc.setNhanSuYTe(nhanSu);
        bpc.setNguoiLap(manager);
        bpc.setDieuDongNgoaiDangKy(dieuDongNgoai);
        bpc.setGhiChu(request.getGhiChu());
        BangPhanCong saved = bangPhanCongRepository.save(bpc);

        return bangPhanCongMapper.toSlot(saved);
    }

    @Override
    @Transactional
    public void huyPhanCong(Long phanCongId) {
        BangPhanCong bpc = bangPhanCongRepository.findById(phanCongId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công"));
        if (bpc.getTrangThai() == TrangThaiBangPhanCong.DA_CONG_BO) {
            throw new BusinessRuleException("Lịch đã công bố, không thể hủy!");
        }
        bangPhanCongRepository.delete(bpc);
    }

    @Override
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

        long soKhoa = dsBang.stream().map(b -> b.getKhoa().getId()).distinct().count();
        return new PhanCongDtos.CongBoKetQua(tuanId, (int) soKhoa, dsBang.size(), now);
    }

    @Override
    @Transactional(readOnly = true)
    public PhanCongDtos.LichKhoa layLichKhoa(Long khoaId, Long tuanId) {
        Khoa khoa = khoaRepository.findById(khoaId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khoa"));
        TuanLamViec tuan = tuanLamViecRepository.findById(tuanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuần làm việc"));

        List<CaTrucKhoa> dsCa = caTrucKhoaRepository.timTheoKhoaTuan(khoaId, tuanId);
        List<BangPhanCong> dsPhanCong = bangPhanCongRepository.findByKhoaIdAndTuanLamViecId(khoaId, tuanId);

        Map<Long, Map<VaiTroPhanCong, BangPhanCong>> pcTheoCa = new HashMap<>();
        for (BangPhanCong pc : dsPhanCong) {
            if (pc.getCaTrucKhoa() != null && pc.getVaiTro() != null) {
                pcTheoCa.computeIfAbsent(pc.getCaTrucKhoa().getId(), k -> new EnumMap<>(VaiTroPhanCong.class))
                        .put(pc.getVaiTro(), pc);
            }
        }

        int soCaDaDu = 0;
        List<PhanCongDtos.CaPhanCong> danhSach = new ArrayList<>();
        for (CaTrucKhoa ca : dsCa) {
            Map<VaiTroPhanCong, BangPhanCong> theoVaiTro = pcTheoCa.getOrDefault(ca.getId(), Collections.emptyMap());
            if (theoVaiTro.size() == SO_VI_TRI_MOI_CA) soCaDaDu++;
            danhSach.add(new PhanCongDtos.CaPhanCong(
                    HoSoMapper.caTruc(ca),
                    bangPhanCongMapper.toSlot(theoVaiTro.get(VaiTroPhanCong.BAC_SI_CHINH)),
                    bangPhanCongMapper.toSlot(theoVaiTro.get(VaiTroPhanCong.Y_TA_1)),
                    bangPhanCongMapper.toSlot(theoVaiTro.get(VaiTroPhanCong.Y_TA_2))
            ));
        }

        boolean daCongBo = dsPhanCong.stream().anyMatch(b -> b.getTrangThai() == TrangThaiBangPhanCong.DA_CONG_BO);
        String trangThai = daCongBo ? "DA_CONG_BO" : (soCaDaDu == dsCa.size() && !dsCa.isEmpty() ? "DA_DU" : "DANG_XEP");

        return new PhanCongDtos.LichKhoa(
                khoa.getId(), khoa.getMaKhoa(), khoa.getTen(),
                tuan.getId(), tuan.getTen(),
                trangThai,
                dsCa.size(), soCaDaDu,
                danhSach
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhanCongDtos.KhoaTienDo> tongQuan(Long tuanId) {
        if (!tuanLamViecRepository.existsById(tuanId)) {
            throw new ResourceNotFoundException("Không tìm thấy tuần làm việc");
        }

        Map<Long, List<CaTrucKhoa>> caTheoKhoa = caTrucKhoaRepository.timTheoTuan(tuanId).stream()
                .collect(Collectors.groupingBy(c -> c.getKhoa().getId()));

        List<BangPhanCong> tatCaPhanCong = bangPhanCongRepository.findByTuanLamViecId(tuanId);
        Map<Long, Set<VaiTroPhanCong>> viTriTheoCa = new HashMap<>();
        for (BangPhanCong pc : tatCaPhanCong) {
            if (pc.getCaTrucKhoa() != null && pc.getVaiTro() != null) {
                viTriTheoCa.computeIfAbsent(pc.getCaTrucKhoa().getId(), k -> EnumSet.noneOf(VaiTroPhanCong.class)).add(pc.getVaiTro());
            }
        }

        Map<Long, List<NhanSuYTe>> nhanSuTheoKhoa = nhanSuYTeRepository.findAll().stream()
                .filter(n -> n.getKhoa() != null)
                .collect(Collectors.groupingBy(n -> n.getKhoa().getId()));

        Map<Long, List<BangPhanCong>> pcTheoKhoa = tatCaPhanCong.stream()
                .filter(b -> b.getKhoa() != null)
                .collect(Collectors.groupingBy(b -> b.getKhoa().getId()));

        List<PhanCongDtos.KhoaTienDo> result = new ArrayList<>();
        for (Khoa khoa : khoaRepository.findAll(Sort.by("maKhoa"))) {
            List<CaTrucKhoa> dsCa = caTheoKhoa.getOrDefault(khoa.getId(), List.of());
            int soCaDaDu = (int) dsCa.stream().filter(c -> {
                Set<VaiTroPhanCong> vt = viTriTheoCa.get(c.getId());
                return vt != null && vt.size() == SO_VI_TRI_MOI_CA;
            }).count();

            List<NhanSuYTe> ns = nhanSuTheoKhoa.getOrDefault(khoa.getId(), List.of());
            long soBacSi = ns.stream().filter(HoSoMapper::laBacSi).count();
            long soYTa = ns.stream().filter(HoSoMapper::laYTa).count();

            List<BangPhanCong> bangKhoa = pcTheoKhoa.getOrDefault(khoa.getId(), List.of());
            boolean daCongBo = bangKhoa.stream().anyMatch(b -> b.getTrangThai() == TrangThaiBangPhanCong.DA_CONG_BO);
            String trangThai = daCongBo ? "DA_CONG_BO" : (soCaDaDu == dsCa.size() && !dsCa.isEmpty() ? "DA_DU" : "DANG_XEP");
            LocalDateTime ngayCongBo = bangKhoa.stream()
                    .filter(b -> b.getTrangThai() == TrangThaiBangPhanCong.DA_CONG_BO)
                    .map(BangPhanCong::getNgayCongBo)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            result.add(new PhanCongDtos.KhoaTienDo(
                    khoa.getId(), khoa.getMaKhoa(), khoa.getTen(),
                    soBacSi, soYTa,
                    dsCa.size(), soCaDaDu,
                    trangThai,
                    ngayCongBo
            ));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhanCongDtos.UngVien> danhSachUngVien(Long caTrucKhoaId, VaiTroPhanCong vaiTro) {
        CaTrucKhoa ca = caTrucKhoaRepository.findById(caTrucKhoaId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca trực khoa"));
        TuanLamViec tuan = ca.getNgayTrongTuan().getTuanLamViec();
        boolean canBacSi = vaiTro == VaiTroPhanCong.BAC_SI_CHINH;

        Set<Long> daDangKy = dangKyCaRepository.findByCaTrucKhoaId(caTrucKhoaId).stream()
                .map(d -> d.getPhieuDangKy().getNhanSuYTe().getId())
                .collect(Collectors.toSet());
        Set<Long> trongCa = bangPhanCongRepository.findByCaTrucKhoaId(caTrucKhoaId).stream()
                .map(pc -> pc.getNhanSuYTe().getId())
                .collect(Collectors.toSet());

        return nhanSuYTeRepository.findAll().stream()
                .filter(n -> canBacSi ? HoSoMapper.laBacSi(n) : HoSoMapper.laYTa(n))
                .map(n -> {
                    boolean cungKhoa = n.getKhoa() != null && Objects.equals(n.getKhoa().getId(), ca.getKhoa().getId());
                    return bangPhanCongMapper.toUngVien(
                            n,
                            cungKhoa,
                            daDangKy.contains(n.getId()),
                            bangPhanCongRepository.countByNhanSuYTeIdAndCaTrucKhoa_NgayTrongTuan_Id(n.getId(), ca.getNgayTrongTuan().getId()),
                            tuan.getSoCaToiDaMoiNgay(),
                            trongCa.contains(n.getId()));
                })
                .sorted(Comparator.comparing(PhanCongDtos.UngVien::daDangKy).reversed()
                        .thenComparing(Comparator.comparing(PhanCongDtos.UngVien::cungKhoa).reversed())
                        .thenComparing(PhanCongDtos.UngVien::maNhanSu))
                .toList();
    }
}
