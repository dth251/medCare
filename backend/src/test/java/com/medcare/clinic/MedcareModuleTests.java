package com.medcare.clinic;

import com.medcare.clinic.dto.dangky.DangKyCaRequest;
import com.medcare.clinic.dto.dangky.DangKyDtos;
import com.medcare.clinic.dto.phancong.PhanCongDtos;
import com.medcare.clinic.dto.phancong.PhanCongRequest;
import com.medcare.clinic.dto.thanhtoan.DieuChinhRequest;
import com.medcare.clinic.dto.thanhtoan.KhieuNaiRequest;
import com.medcare.clinic.dto.thanhtoan.ThanhToanDtos;
import com.medcare.clinic.dto.thanhtoan.ThanhToanRequest;
import com.medcare.clinic.entity.*;
import com.medcare.clinic.enums.*;
import com.medcare.clinic.exception.BusinessRuleException;
import com.medcare.clinic.exception.ResourceNotFoundException;
import com.medcare.clinic.repository.*;
import com.medcare.clinic.service.DangKyCaService;
import com.medcare.clinic.service.KhieuNaiService;
import com.medcare.clinic.service.PhanCongService;
import com.medcare.clinic.service.ThanhToanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedcareModuleTests {

    @Mock PhieuDangKyRepository phieuDangKyRepository;
    @Mock DangKyCaRepository dangKyCaRepository;
    @Mock TuanLamViecRepository tuanLamViecRepository;
    @Mock NhanSuYTeRepository nhanSuYTeRepository;
    @Mock CaTrucKhoaRepository caTrucKhoaRepository;
    @Mock BangPhanCongRepository bangPhanCongRepository;
    @Mock PhanCongCaRepository phanCongCaRepository;
    @Mock NhanVienRepository nhanVienRepository;
    @Mock KhoaRepository khoaRepository;
    @Mock ChamCongRepository chamCongRepository;
    @Mock YeuCauKhieuNaiRepository yeuCauKhieuNaiRepository;
    @Mock PhieuThanhToanRepository phieuThanhToanRepository;

    @InjectMocks DangKyCaService dangKyCaService;
    @InjectMocks PhanCongService phanCongService;
    @InjectMocks KhieuNaiService khieuNaiService;
    @InjectMocks ThanhToanService thanhToanService;

    Khoa khoaRHM;
    Khoa khoaNoi;
    BacSi doctorA;
    YTa nurseB;
    NhanVien managerC;
    NhanVien accountantB;
    TuanLamViec week37;
    CaLamViec caSang;
    CaLamViec caToi;
    List<NgayTrongTuan> days;

    @BeforeEach
    void setUp() {
        khoaRHM = Khoa.builder().id(4L).maKhoa("K04").ten("Răng – Hàm – Mặt").build();
        khoaNoi = Khoa.builder().id(1L).maKhoa("K01").ten("Nội tổng quát").build();

        doctorA = new BacSi();
        doctorA.setId(101L);
        doctorA.setTenDangNhap("bs.nguyenvana");
        doctorA.setHo("Nguyễn"); doctorA.setTenDem("Văn"); doctorA.setTen("A");
        doctorA.setMaBacSi("BS001");
        doctorA.setHocVi(TrinhDoHocVi.THAC_SI);
        doctorA.setKhoa(khoaRHM);
        doctorA.setDonGiaTienCong(BigDecimal.valueOf(500000));
        doctorA.setSoTaiKhoan("0123456789");
        doctorA.setTenNganHang("Vietcombank");
        doctorA.setChuTaiKhoan("NGUYEN VAN A");

        nurseB = new YTa();
        nurseB.setId(102L);
        nurseB.setTenDangNhap("yt.tranthib");
        nurseB.setMaYTa("YT001");
        nurseB.setTrinhDo(TrinhDoHocVi.CU_NHAN);
        nurseB.setKhoa(khoaRHM);
        nurseB.setDonGiaTienCong(BigDecimal.valueOf(300000));

        managerC = new NhanVien();
        managerC.setId(201L);
        managerC.setTenDangNhap("ql.levanc");
        managerC.setViTriCongViec(ViTriCongViec.QUAN_LY);

        accountantB = new NhanVien();
        accountantB.setId(202L);
        accountantB.setTenDangNhap("kt.tranthib");
        accountantB.setViTriCongViec(ViTriCongViec.KE_TOAN);

        week37 = TuanLamViec.builder()
                .id(37L).ten("Tuần 37")
                .ngayBatDau(LocalDate.of(2026, 9, 7)).ngayKetThuc(LocalDate.of(2026, 9, 13))
                .thoiDiemMoDangKy(LocalDateTime.now().minusDays(1))
                .hanDangKy(LocalDateTime.now().plusDays(2))
                .trangThai(TrangThaiTuan.DANG_MO_DANG_KY)
                .soCaToiThieu(8).soCaToiDa(12).soCaToiDaMoiNgay(2)
                .build();

        caSang = CaLamViec.builder().id(1L).tenCa("Sáng").loaiCa(LoaiCa.CHINH)
                .gioBatDau(LocalTime.of(7, 0)).gioKetThuc(LocalTime.of(11, 0)).heSoLuong(BigDecimal.ONE).build();
        caToi = CaLamViec.builder().id(3L).tenCa("Tối").loaiCa(LoaiCa.NGOAI_GIO)
                .gioBatDau(LocalTime.of(18, 0)).gioKetThuc(LocalTime.of(22, 0)).heSoLuong(BigDecimal.valueOf(2.0)).build();

        days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            days.add(NgayTrongTuan.builder().id((long) i + 1).thu(DayOfWeek.of(i + 1))
                    .ngay(week37.getNgayBatDau().plusDays(i)).tuanLamViec(week37).build());
        }

        when(nhanSuYTeRepository.findByTenDangNhap("bs.nguyenvana")).thenReturn(Optional.of(doctorA));
        when(nhanSuYTeRepository.findById(doctorA.getId())).thenReturn(Optional.of(doctorA));
        when(nhanSuYTeRepository.findById(nurseB.getId())).thenReturn(Optional.of(nurseB));
        when(nhanVienRepository.findByTenDangNhap("ql.levanc")).thenReturn(Optional.of(managerC));
        when(nhanVienRepository.findByTenDangNhap("kt.tranthib")).thenReturn(Optional.of(accountantB));
        when(tuanLamViecRepository.findById(37L)).thenReturn(Optional.of(week37));
        when(tuanLamViecRepository.existsById(37L)).thenReturn(true);
    }

    private CaTrucKhoa ca(long id, Khoa khoa, int dayIdx, CaLamViec clv) {
        return CaTrucKhoa.builder().id(id).khoa(khoa).ngayTrongTuan(days.get(dayIdx)).caLamViec(clv).build();
    }

    /** 10 ca hợp lệ: 2 ca/ngày từ T2 đến T6, thuộc khoa RHM. */
    private List<CaTrucKhoa> muoiCaHopLe() {
        List<CaTrucKhoa> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) list.add(ca(i + 1, khoaRHM, i / 2, i % 2 == 0 ? caSang : caToi));
        return list;
    }

    private static List<Long> ids(List<CaTrucKhoa> cas) {
        return cas.stream().map(CaTrucKhoa::getId).toList();
    }

    // ==========================================================
    // MODULE 1: ĐĂNG KÝ CA
    // ==========================================================
    @Nested
    @DisplayName("Module 1 – Đăng ký ca trực")
    class DangKy {

        @Test
        @DisplayName("Thành công: đăng ký 10 ca hợp lệ, phiếu trả về đủ danh sách ca")
        void thanhCong() {
            List<CaTrucKhoa> cas = muoiCaHopLe();
            when(caTrucKhoaRepository.findAllById(any())).thenReturn(cas);
            when(phieuDangKyRepository.findByNhanSuYTeIdAndTuanLamViecId(doctorA.getId(), 37L)).thenReturn(Optional.empty());
            when(phieuDangKyRepository.save(any(PhieuDangKy.class))).thenAnswer(inv -> inv.getArgument(0));

            DangKyDtos.PhieuDangKy result = dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, ids(cas)));

            assertEquals("DA_LUU", result.trangThai());
            assertEquals(10, result.tongSoCa());
            assertEquals(10, result.caTrucKhoaIds().size());
        }

        @Test
        @DisplayName("Đăng ký lại: xóa chi tiết cũ và flush trước khi thêm mới (tránh trùng unique)")
        void dangKyLai() {
            List<CaTrucKhoa> cas = muoiCaHopLe();
            PhieuDangKy cu = PhieuDangKy.builder().id(9L).maPhieu("PDK-37-101").nhanSuYTe(doctorA).tuanLamViec(week37)
                    .trangThai(TrangThaiPhieuDangKy.DA_LUU).build();
            cu.getChiTietDangKy().add(DangKyCa.builder().id(1L).phieuDangKy(cu).caTrucKhoa(cas.get(0)).build());

            when(caTrucKhoaRepository.findAllById(any())).thenReturn(cas);
            when(phieuDangKyRepository.findByNhanSuYTeIdAndTuanLamViecId(doctorA.getId(), 37L)).thenReturn(Optional.of(cu));
            when(phieuDangKyRepository.saveAndFlush(any(PhieuDangKy.class))).thenAnswer(inv -> inv.getArgument(0));
            when(phieuDangKyRepository.save(any(PhieuDangKy.class))).thenAnswer(inv -> inv.getArgument(0));

            DangKyDtos.PhieuDangKy result = dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, ids(cas)));

            verify(phieuDangKyRepository).saveAndFlush(cu);
            assertEquals(10, result.caTrucKhoaIds().size());
        }

        @Test
        @DisplayName("Ngoại lệ: dưới định mức tối thiểu 8 ca")
        void duoiDinhMuc() {
            var ex = assertThrows(BusinessRuleException.class, () ->
                    dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, List.of(1L, 2L, 3L, 4L, 5L))));
            assertTrue(ex.getMessage().contains("Số ca đăng ký phải từ 8 đến 12 ca/tuần"));
        }

        @Test
        @DisplayName("Ngoại lệ: quá 2 ca trong cùng 1 ngày")
        void qua2CaMotNgay() {
            List<CaTrucKhoa> cas = new ArrayList<>(List.of(
                    ca(1, khoaRHM, 0, caSang), ca(2, khoaRHM, 0, caSang), ca(3, khoaRHM, 0, caToi)));
            for (int i = 4; i <= 8; i++) cas.add(ca(i, khoaRHM, i - 3, caSang));
            when(caTrucKhoaRepository.findAllById(any())).thenReturn(cas);

            var ex = assertThrows(BusinessRuleException.class, () ->
                    dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, ids(cas))));
            assertTrue(ex.getMessage().contains("Không được đăng ký quá 2 ca trong cùng một ngày"));
        }

        @Test
        @DisplayName("Ngoại lệ: cổng đăng ký đã đóng")
        void congDaDong() {
            week37.setTrangThai(TrangThaiTuan.DA_DONG_DANG_KY);
            var ex = assertThrows(BusinessRuleException.class, () ->
                    dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, ids(muoiCaHopLe()))));
            assertTrue(ex.getMessage().contains("không mở"));
        }

        @Test
        @DisplayName("Ngoại lệ: đã quá hạn đăng ký")
        void quaHan() {
            week37.setHanDangKy(LocalDateTime.now().minusMinutes(1));
            var ex = assertThrows(BusinessRuleException.class, () ->
                    dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, ids(muoiCaHopLe()))));
            assertTrue(ex.getMessage().contains("quá hạn"));
        }

        @Test
        @DisplayName("Ngoại lệ: có ID ca không tồn tại")
        void caKhongTonTai() {
            List<CaTrucKhoa> cas = muoiCaHopLe();
            when(caTrucKhoaRepository.findAllById(any())).thenReturn(cas.subList(0, 9));
            var ex = assertThrows(BusinessRuleException.class, () ->
                    dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, ids(cas))));
            assertTrue(ex.getMessage().contains("không tồn tại"));
        }

        @Test
        @DisplayName("Ngoại lệ: đăng ký ca của khoa khác")
        void caKhoaKhac() {
            List<CaTrucKhoa> cas = muoiCaHopLe();
            cas.set(0, ca(1, khoaNoi, 0, caSang));
            when(caTrucKhoaRepository.findAllById(any())).thenReturn(cas);
            var ex = assertThrows(BusinessRuleException.class, () ->
                    dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, ids(cas))));
            assertTrue(ex.getMessage().contains("khoa của bạn"));
        }
    }

    // ==========================================================
    // MODULE 2: PHÂN CÔNG
    // ==========================================================
    @Nested
    @DisplayName("Module 2 – Lên lịch & phân công")
    class PhanCong {

        CaTrucKhoa caMot;
        BangPhanCong bang;

        @BeforeEach
        void init() {
            caMot = ca(10, khoaRHM, 0, caSang);
            bang = BangPhanCong.builder().id(1L).khoa(khoaRHM).tuanLamViec(week37).trangThai(TrangThaiBangPhanCong.DANG_XEP).build();
            when(caTrucKhoaRepository.findById(10L)).thenReturn(Optional.of(caMot));
            when(bangPhanCongRepository.findByKhoaIdAndTuanLamViecId(khoaRHM.getId(), 37L)).thenReturn(Optional.of(bang));
            when(phanCongCaRepository.findByCaTrucKhoaIdAndVaiTro(anyLong(), any())).thenReturn(Optional.empty());
            when(phanCongCaRepository.findByCaTrucKhoaId(10L)).thenReturn(List.of());
            when(phanCongCaRepository.save(any(PhanCongCa.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("Thành công: phân công Bác sĩ đã đăng ký vào vị trí BS chính")
        void thanhCong() {
            when(dangKyCaRepository.daDangKy(doctorA.getId(), 10L)).thenReturn(true);

            PhanCongDtos.Slot slot = phanCongService.phanCong("ql.levanc",
                    new PhanCongRequest(10L, doctorA.getId(), VaiTroPhanCong.BAC_SI_CHINH, false, "Phân ca chuẩn"));

            assertEquals("BAC_SI_CHINH", slot.vaiTro());
            assertEquals(doctorA.getId(), slot.nhanSuId());
            assertFalse(slot.dieuDongNgoaiDangKy());
        }

        @Test
        @DisplayName("Ngoại lệ: gán Y tá vào vị trí Bác sĩ chính")
        void sai_vi_tri() {
            var ex = assertThrows(BusinessRuleException.class, () -> phanCongService.phanCong("ql.levanc",
                    new PhanCongRequest(10L, nurseB.getId(), VaiTroPhanCong.BAC_SI_CHINH, false, null)));
            assertTrue(ex.getMessage().contains("Vị trí Bác sĩ chính phải do Bác sĩ đảm nhiệm"));
        }

        @Test
        @DisplayName("Ngoại lệ: nhân sự chưa đăng ký ca và chưa xác nhận điều động")
        void chuaDangKy_canDieuDong() {
            when(dangKyCaRepository.daDangKy(doctorA.getId(), 10L)).thenReturn(false);
            var ex = assertThrows(BusinessRuleException.class, () -> phanCongService.phanCong("ql.levanc",
                    new PhanCongRequest(10L, doctorA.getId(), VaiTroPhanCong.BAC_SI_CHINH, false, null)));
            assertTrue(ex.getMessage().contains("điều động"));
        }

        @Test
        @DisplayName("Thành công: điều động ngoài đăng ký khi có xác nhận")
        void dieuDong() {
            when(dangKyCaRepository.daDangKy(doctorA.getId(), 10L)).thenReturn(false);
            var slot = phanCongService.phanCong("ql.levanc",
                    new PhanCongRequest(10L, doctorA.getId(), VaiTroPhanCong.BAC_SI_CHINH, true, "Thiếu BS"));
            assertTrue(slot.dieuDongNgoaiDangKy());
        }

        @Test
        @DisplayName("Ngoại lệ: nhân sự đã đủ 2 ca trong ngày")
        void du2CaTrongNgay() {
            when(dangKyCaRepository.daDangKy(doctorA.getId(), 10L)).thenReturn(true);
            when(phanCongCaRepository.countByNhanSuYTeIdAndCaTrucKhoa_NgayTrongTuan_Id(doctorA.getId(), 1L)).thenReturn(2L);
            var ex = assertThrows(BusinessRuleException.class, () -> phanCongService.phanCong("ql.levanc",
                    new PhanCongRequest(10L, doctorA.getId(), VaiTroPhanCong.BAC_SI_CHINH, false, null)));
            assertTrue(ex.getMessage().contains("đã đủ 2 ca"));
        }

        @Test
        @DisplayName("Ngoại lệ: lịch đã công bố thì không được sửa")
        void daCongBo() {
            bang.setTrangThai(TrangThaiBangPhanCong.DA_CONG_BO);
            var ex = assertThrows(BusinessRuleException.class, () -> phanCongService.phanCong("ql.levanc",
                    new PhanCongRequest(10L, doctorA.getId(), VaiTroPhanCong.BAC_SI_CHINH, true, null)));
            assertTrue(ex.getMessage().contains("đã được công bố"));
        }

        private void stubTongQuan(List<PhanCongCa> phanCong) {
            when(caTrucKhoaRepository.timTheoTuan(37L)).thenReturn(List.of(caMot));
            when(phanCongCaRepository.findByBangPhanCong_TuanLamViec_Id(37L)).thenReturn(phanCong);
            when(bangPhanCongRepository.findByTuanLamViecId(37L)).thenReturn(List.of(bang));
            when(nhanSuYTeRepository.findAll()).thenReturn(List.of(doctorA, nurseB));
            when(khoaRepository.findAll(any(Sort.class))).thenReturn(List.of(khoaRHM));
        }

        @Test
        @DisplayName("Ngoại lệ: không công bố được khi còn ca chưa đủ 1 BS + 2 YT")
        void congBo_chuaDu() {
            stubTongQuan(List.of(PhanCongCa.builder().caTrucKhoa(caMot).vaiTro(VaiTroPhanCong.BAC_SI_CHINH).nhanSuYTe(doctorA).build()));
            var ex = assertThrows(BusinessRuleException.class, () -> phanCongService.congBoLichTuan(37L));
            assertTrue(ex.getMessage().contains("K04 (0/1)"));
        }

        @Test
        @DisplayName("Thành công: công bố khi tất cả các ca đã đủ người")
        void congBo_thanhCong() {
            stubTongQuan(List.of(
                    PhanCongCa.builder().caTrucKhoa(caMot).vaiTro(VaiTroPhanCong.BAC_SI_CHINH).nhanSuYTe(doctorA).build(),
                    PhanCongCa.builder().caTrucKhoa(caMot).vaiTro(VaiTroPhanCong.Y_TA_1).nhanSuYTe(nurseB).build(),
                    PhanCongCa.builder().caTrucKhoa(caMot).vaiTro(VaiTroPhanCong.Y_TA_2).nhanSuYTe(nurseB).build()));

            var kq = phanCongService.congBoLichTuan(37L);

            assertEquals(1, kq.tongSoCa());
            assertEquals(TrangThaiBangPhanCong.DA_CONG_BO, bang.getTrangThai());
            assertEquals(TrangThaiTuan.DA_CONG_BO, week37.getTrangThai());
        }
    }

    // ==========================================================
    // MODULE 3: KHIẾU NẠI & THANH TOÁN
    // ==========================================================
    @Nested
    @DisplayName("Module 3 – Khiếu nại & thanh toán")
    class ThanhToan {

        BangPhanCong bang;
        PhanCongCa pcSang;
        PhanCongCa pcToiLoi;

        @BeforeEach
        void init() {
            bang = BangPhanCong.builder().id(1L).khoa(khoaRHM).tuanLamViec(week37).trangThai(TrangThaiBangPhanCong.DA_CONG_BO).build();
            pcSang = PhanCongCa.builder().id(50L).bangPhanCong(bang).nhanSuYTe(doctorA)
                    .caTrucKhoa(ca(1, khoaRHM, 0, caSang)).vaiTro(VaiTroPhanCong.BAC_SI_CHINH).build();
            pcToiLoi = PhanCongCa.builder().id(51L).bangPhanCong(bang).nhanSuYTe(doctorA)
                    .caTrucKhoa(ca(3, khoaRHM, 0, caToi)).vaiTro(VaiTroPhanCong.BAC_SI_CHINH).build();
            when(phanCongCaRepository.findById(51L)).thenReturn(Optional.of(pcToiLoi));
            when(phieuThanhToanRepository.findByNhanSuYTeIdAndTuanLamViecId(doctorA.getId(), 37L)).thenReturn(Optional.empty());
            when(yeuCauKhieuNaiRepository.save(any(YeuCauKhieuNai.class))).thenAnswer(inv -> inv.getArgument(0));
            when(phieuThanhToanRepository.save(any(PhieuThanhToan.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("Gửi khiếu nại thành công cho ca của chính mình")
        void guiKhieuNai() {
            var don = khieuNaiService.taoKhieuNai("bs.nguyenvana", new KhieuNaiRequest(51L, "Thiếu check-in ca tối", "camera.jpg"));
            assertEquals("CHO_XU_LY", don.trangThai());
            assertEquals(51L, don.phanCongCaId());
            assertEquals(37L, don.tuanId());
        }

        @Test
        @DisplayName("Không được khiếu nại ca của người khác")
        void khieuNaiCaNguoiKhac() {
            assertThrows(AccessDeniedException.class, () ->
                    khieuNaiService.taoKhieuNai("yt.tranthib", new KhieuNaiRequest(51L, "...", null)));
        }

        @Test
        @DisplayName("Lập biên bản điều chỉnh bổ sung 1 ca ngoài giờ = 500.000 × 2.0")
        void dieuChinh() {
            YeuCauKhieuNai kn = YeuCauKhieuNai.builder().id(88L).maYeuCau("YCKN-1").nhanSu(doctorA)
                    .phanCongCa(pcToiLoi).bangPhanCong(bang).trangThai("CHO_XU_LY").ngayGui(LocalDateTime.now()).build();
            when(yeuCauKhieuNaiRepository.findById(88L)).thenReturn(Optional.of(kn));

            var kq = khieuNaiService.lapBienBanDieuChinh("kt.tranthib",
                    new DieuChinhRequest(88L, 0, 1, BigDecimal.ZERO, BigDecimal.ZERO, "Xác nhận có trực"));

            assertEquals("DA_XU_LY", kq.trangThai());
            assertEquals(0, BigDecimal.valueOf(1000000).compareTo(kq.dieuChinh().soTien()));
        }

        @Test
        @DisplayName("Không xử lý lại khiếu nại đã xử lý")
        void dieuChinhLai() {
            YeuCauKhieuNai kn = YeuCauKhieuNai.builder().id(88L).maYeuCau("YCKN-1").nhanSu(doctorA).trangThai("DA_XU_LY").build();
            when(yeuCauKhieuNaiRepository.findById(88L)).thenReturn(Optional.of(kn));
            assertThrows(BusinessRuleException.class, () -> khieuNaiService.lapBienBanDieuChinh("kt.tranthib",
                    new DieuChinhRequest(88L, 0, 1, BigDecimal.ZERO, BigDecimal.ZERO, "x")));
        }

        @Test
        @DisplayName("ID khiếu nại không tồn tại -> báo lỗi, không lấy nhầm đơn khác")
        void khieuNaiKhongTonTai() {
            when(yeuCauKhieuNaiRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> khieuNaiService.lapBienBanDieuChinh("kt.tranthib",
                    new DieuChinhRequest(999L, 0, 1, BigDecimal.ZERO, BigDecimal.ZERO, "x")));
            verify(yeuCauKhieuNaiRepository, never()).findAll();
        }

        private void stubLuong(String trangThaiKhieuNai) {
            List<PhanCongCa> ds = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                ds.add(PhanCongCa.builder().id(60L + i).bangPhanCong(bang).nhanSuYTe(doctorA)
                        .caTrucKhoa(ca(20 + i, khoaRHM, i, caSang)).vaiTro(VaiTroPhanCong.BAC_SI_CHINH).build());
            }
            ds.add(PhanCongCa.builder().id(70L).bangPhanCong(bang).nhanSuYTe(doctorA)
                    .caTrucKhoa(ca(30, khoaRHM, 4, caToi)).vaiTro(VaiTroPhanCong.BAC_SI_CHINH).build());
            ds.add(pcToiLoi);
            when(phanCongCaRepository.findByNhanSuYTeIdAndBangPhanCong_TuanLamViec_Id(doctorA.getId(), 37L)).thenReturn(ds);
            when(chamCongRepository.findByPhanCongCaId(anyLong())).thenReturn(Optional.empty());
            when(chamCongRepository.findByPhanCongCaId(51L)).thenReturn(Optional.of(
                    ChamCong.builder().id(1L).phanCongCa(pcToiLoi).trangThai(TrangThaiChamCong.LOI_THIET_BI).build()));

            YeuCauKhieuNai kn = YeuCauKhieuNai.builder().id(88L).maYeuCau("YCKN-1").nhanSu(doctorA).bangPhanCong(bang)
                    .trangThai(trangThaiKhieuNai).soTienDieuChinh(BigDecimal.valueOf(1000000)).build();
            when(yeuCauKhieuNaiRepository.findTheoNhanSuTuan(doctorA.getId(), 37L)).thenReturn(List.of(kn));
        }

        @Test
        @DisplayName("Tính lương: 3 ca chính + 1 ca ngoài giờ, bỏ ca lỗi thiết bị, cộng 1.000.000 điều chỉnh")
        void tinhLuong() {
            stubLuong("DA_XU_LY");
            ThanhToanDtos.BangTinhLuong l = thanhToanService.tinhLuong(doctorA, 37L);

            assertEquals(3, l.soCaChinh());
            assertEquals(1, l.soCaNgoaiGio());
            assertEquals(1, l.soCaKhongTinh());
            // 3 × 500k + 1 × 500k × 2.0 + 1.000k = 3.500.000
            assertEquals(0, BigDecimal.valueOf(3500000).compareTo(l.tongThucLinh()));
        }

        @Test
        @DisplayName("Thanh toán trực tuyến thành công, ghi đúng tài khoản nhận và tổng thực lĩnh")
        void thanhToanThanhCong() {
            stubLuong("DA_XU_LY");
            var phieu = thanhToanService.thanhToan("kt.tranthib",
                    new ThanhToanRequest(doctorA.getId(), 37L, HinhThucThanhToan.TRUC_TUYEN, "TXN-998877", null));

            assertEquals("DA_THANH_TOAN", phieu.trangThai());
            assertEquals("TRUC_TUYEN", phieu.hinhThucThanhToan());
            assertEquals("0123456789", phieu.soTaiKhoanNhan());
            assertEquals(0, BigDecimal.valueOf(3500000).compareTo(phieu.tongThucLinh()));
            assertEquals(1, phieu.dieuChinh().size());
        }

        @Test
        @DisplayName("Không thanh toán khi còn khiếu nại chờ xử lý")
        void conKhieuNai() {
            stubLuong("CHO_XU_LY");
            var ex = assertThrows(BusinessRuleException.class, () -> thanhToanService.thanhToan("kt.tranthib",
                    new ThanhToanRequest(doctorA.getId(), 37L, HinhThucThanhToan.TRUC_TIEP, null, null)));
            assertTrue(ex.getMessage().contains("khiếu nại chưa xử lý"));
        }

        @Test
        @DisplayName("Không thanh toán hai lần cho cùng một tuần")
        void thanhToanHaiLan() {
            when(phieuThanhToanRepository.findByNhanSuYTeIdAndTuanLamViecId(doctorA.getId(), 37L)).thenReturn(Optional.of(
                    PhieuThanhToan.builder().id(5L).trangThai(TrangThaiThanhToan.DA_THANH_TOAN).build()));
            var ex = assertThrows(BusinessRuleException.class, () -> thanhToanService.thanhToan("kt.tranthib",
                    new ThanhToanRequest(doctorA.getId(), 37L, HinhThucThanhToan.TRUC_TIEP, null, null)));
            assertTrue(ex.getMessage().contains("đã được thanh toán"));
        }

        @Test
        @DisplayName("Nhân sự không xem được bảng chấm công của người khác")
        void chamCongNguoiKhac() {
            assertThrows(AccessDeniedException.class, () ->
                    thanhToanService.layBangChamCong("yt.tranthib", false, doctorA.getId(), 37L));
        }
    }
}
