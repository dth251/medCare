package com.medcare.clinic;

import com.medcare.clinic.dto.auth.UpdateProfileRequest;
import com.medcare.clinic.dto.dangky.DangKyCaRequest;
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
import com.medcare.clinic.mapper.*;
import com.medcare.clinic.repository.*;
import com.medcare.clinic.service.impl.AuthServiceImpl;
import com.medcare.clinic.service.impl.DangKyCaServiceImpl;
import com.medcare.clinic.service.impl.KhieuNaiServiceImpl;
import com.medcare.clinic.service.impl.PhanCongServiceImpl;
import com.medcare.clinic.service.impl.ThanhToanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
    @Mock NhanVienRepository nhanVienRepository;
    @Mock KhoaRepository khoaRepository;
    @Mock ChamCongRepository chamCongRepository;
    @Mock YeuCauKhieuNaiRepository yeuCauKhieuNaiRepository;
    @Mock PhieuThanhToanRepository phieuThanhToanRepository;
    @Mock ThanhVienRepository thanhVienRepository;
    @Mock DiaChiRepository diaChiRepository;
    @Mock org.springframework.security.authentication.AuthenticationManager authenticationManager;
    @Mock com.medcare.clinic.config.JwtTokenProvider jwtTokenProvider;
    @Spy UserMapper userMapper = new UserMapper();
    @InjectMocks AuthServiceImpl authService;

    @Spy DangKyMapper dangKyMapper = new DangKyMapper();
    @Spy BangPhanCongMapper bangPhanCongMapper = new BangPhanCongMapper();
    @Spy KhieuNaiMapper khieuNaiMapper = new KhieuNaiMapper();
    @Spy ChamCongMapper chamCongMapper = new ChamCongMapper();
    @Spy ThanhToanMapper thanhToanMapper = new ThanhToanMapper(new KhieuNaiMapper());

    @InjectMocks DangKyCaServiceImpl dangKyCaService;
    @InjectMocks PhanCongServiceImpl phanCongService;
    @InjectMocks KhieuNaiServiceImpl khieuNaiService;
    @InjectMocks ThanhToanServiceImpl thanhToanService;

    Khoa khoaRHM;
    Khoa khoaNoi;
    BacSi doctorA;
    YTa nurseB;
    NhanVien managerC;
    NhanVien accountantB;
    TuanLamViec week37;
    CaLamViec caSang;
    CaLamViec caToi;

    @BeforeEach
    void setUpCommon() {
        khoaRHM = Khoa.builder().id(1L).maKhoa("K04").ten("Răng Hàm Mặt").build();
        khoaNoi = Khoa.builder().id(2L).maKhoa("K01").ten("Nội tổng quát").build();

        doctorA = new BacSi();
        doctorA.setId(101L);
        doctorA.setTenDangNhap("bs.nguyenvana");
        doctorA.setHo("Nguyễn");
        doctorA.setTenDem("Văn");
        doctorA.setTen("A");
        doctorA.setKhoa(khoaRHM);
        doctorA.setMaBacSi("BS001");
        doctorA.setHocVi(TrinhDoHocVi.THAC_SI);
        doctorA.setDonGiaTienCong(BigDecimal.valueOf(500000));
        doctorA.setSoTaiKhoan("0123456789");
        doctorA.setTenNganHang("Vietcombank");
        doctorA.setChuTaiKhoan("NGUYEN VAN A");

        nurseB = new YTa();
        nurseB.setId(201L);
        nurseB.setTenDangNhap("yt.tranthib");
        nurseB.setHo("Trần");
        nurseB.setTenDem("Thị");
        nurseB.setTen("B");
        nurseB.setKhoa(khoaRHM);
        nurseB.setMaYTa("YT001");
        nurseB.setTrinhDo(TrinhDoHocVi.CU_NHAN);
        nurseB.setDonGiaTienCong(BigDecimal.valueOf(300000));

        managerC = new NhanVien();
        managerC.setId(301L);
        managerC.setTenDangNhap("ql.levanc");
        managerC.setHo("Lê");
        managerC.setTenDem("Văn");
        managerC.setTen("C");
        managerC.setViTriCongViec(ViTriCongViec.QUAN_LY);

        accountantB = new NhanVien();
        accountantB.setId(302L);
        accountantB.setTenDangNhap("kt.tranthib");
        accountantB.setHo("Trần");
        accountantB.setTenDem("Thị");
        accountantB.setTen("B");
        accountantB.setViTriCongViec(ViTriCongViec.KE_TOAN);

        week37 = TuanLamViec.builder()
                .id(37L)
                .ten("Tuần 37 (07/09/2026 - 13/09/2026)")
                .ngayBatDau(LocalDate.of(2026, 9, 7))
                .ngayKetThuc(LocalDate.of(2026, 9, 13))
                .trangThai(TrangThaiTuan.DANG_MO_DANG_KY)
                .thoiDiemMoDangKy(LocalDateTime.now().minusDays(3))
                .hanDangKy(LocalDateTime.now().plusDays(2))
                .soCaToiThieu(8)
                .soCaToiDa(12)
                .soCaToiDaMoiNgay(2)
                .build();

        caSang = CaLamViec.builder().id(1L).tenCa("Ca Sáng").loaiCa(LoaiCa.CHINH)
                .gioBatDau(LocalTime.of(7, 0)).gioKetThuc(LocalTime.of(12, 0))
                .heSoLuong(BigDecimal.valueOf(1.0)).build();
        caToi = CaLamViec.builder().id(3L).tenCa("Ca Tối").loaiCa(LoaiCa.NGOAI_GIO)
                .gioBatDau(LocalTime.of(17, 30)).gioKetThuc(LocalTime.of(21, 30))
                .heSoLuong(BigDecimal.valueOf(2.0)).build();

        when(nhanSuYTeRepository.findByTenDangNhap("bs.nguyenvana")).thenReturn(Optional.of(doctorA));
        when(nhanSuYTeRepository.findByTenDangNhap("yt.tranthib")).thenReturn(Optional.of(nurseB));
        when(nhanSuYTeRepository.findById(101L)).thenReturn(Optional.of(doctorA));
        when(nhanSuYTeRepository.findById(201L)).thenReturn(Optional.of(nurseB));
        when(nhanVienRepository.findByTenDangNhap("ql.levanc")).thenReturn(Optional.of(managerC));
        when(nhanVienRepository.findByTenDangNhap("kt.tranthib")).thenReturn(Optional.of(accountantB));
        when(tuanLamViecRepository.findById(37L)).thenReturn(Optional.of(week37));
        when(tuanLamViecRepository.existsById(37L)).thenReturn(true);
        when(khoaRepository.findById(1L)).thenReturn(Optional.of(khoaRHM));
        when(khoaRepository.findById(2L)).thenReturn(Optional.of(khoaNoi));
    }

    CaTrucKhoa ca(long id, Khoa k, int dayOffset, CaLamViec clv) {
        LocalDate d = week37.getNgayBatDau().plusDays(dayOffset);
        NgayTrongTuan n = NgayTrongTuan.builder()
                .id((long) (dayOffset + 1))
                .ngay(d)
                .thu(DayOfWeek.from(d))
                .tuanLamViec(week37)
                .build();
        return CaTrucKhoa.builder().id(id).khoa(k).ngayTrongTuan(n).caLamViec(clv).dangMoCua(true).build();
    }

    // ==========================================================
    // MODULE 1: ĐĂNG KÝ CA TRỰC
    // ==========================================================
    @Nested
    @DisplayName("Module 1 – Đăng ký ca trực")
    class DangKyCa {

        List<CaTrucKhoa> mockCaKhoa;

        @BeforeEach
        void initCa() {
            mockCaKhoa = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                mockCaKhoa.add(ca(10 + i, khoaRHM, i, caSang));
                mockCaKhoa.add(ca(20 + i, khoaRHM, i, caToi));
            }
            when(caTrucKhoaRepository.timTheoKhoaTuan(khoaRHM.getId(), 37L)).thenReturn(mockCaKhoa);
            for (CaTrucKhoa c : mockCaKhoa) {
                when(caTrucKhoaRepository.findById(c.getId())).thenReturn(Optional.of(c));
            }
            when(caTrucKhoaRepository.findAllById(any())).thenAnswer(inv -> {
                Iterable<Long> ids = inv.getArgument(0);
                List<CaTrucKhoa> res = new ArrayList<>();
                for (Long id : ids) {
                    mockCaKhoa.stream().filter(c -> c.getId().equals(id)).findFirst().ifPresent(res::add);
                }
                return res;
            });
            when(phieuDangKyRepository.findByNhanSuYTeIdAndTuanLamViecId(doctorA.getId(), 37L))
                    .thenReturn(Optional.empty());
            when(phieuDangKyRepository.save(any(PhieuDangKy.class))).thenAnswer(inv -> {
                PhieuDangKy p = inv.getArgument(0);
                p.setId(99L);
                return p;
            });
        }

        @Test
        @DisplayName("Thành công: đăng ký hợp lệ 8 ca (đủ min 8, max 12, không quá 2 ca/ngày)")
        void dangKyHopLe() {
            List<Long> caIds = List.of(10L, 20L, 11L, 21L, 12L, 22L, 13L, 23L);

            var res = dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, caIds));

            assertNotNull(res);
            assertEquals(8, res.tongSoCa());
            verify(phieuDangKyRepository).save(any(PhieuDangKy.class));
        }

        @Test
        @DisplayName("Ngoại lệ: đăng ký dưới 8 ca -> BusinessRuleException")
        void thieuSoCaToiThieu() {
            List<Long> caIds = List.of(10L, 11L, 12L);
            var ex = assertThrows(BusinessRuleException.class, () ->
                    dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, caIds)));
            assertTrue(ex.getMessage().contains("tối thiểu 8 ca"));
        }

        @Test
        @DisplayName("Ngoại lệ: đăng ký trên 12 ca -> BusinessRuleException")
        void vuotSoCaToiDa() {
            List<Long> caIds = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                caIds.add((long) (10 + i));
                caIds.add((long) (20 + i));
            } // 14 ca
            var ex = assertThrows(BusinessRuleException.class, () ->
                    dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, caIds.subList(0, 13))));
            assertTrue(ex.getMessage().contains("tối đa 12 ca"));
        }

        @Test
        @DisplayName("Ngoại lệ: vượt quá số ca tối đa mỗi ngày (3 ca cùng 1 ngày)")
        void vuotQuaCaMoiNgay() {
            CaTrucKhoa caChieu = ca(30, khoaRHM, 0, CaLamViec.builder().id(2L).tenCa("Ca Chiều").build());
            mockCaKhoa.add(caChieu);
            when(caTrucKhoaRepository.findById(30L)).thenReturn(Optional.of(caChieu));

            List<Long> caIds = List.of(10L, 20L, 30L, 11L, 12L, 13L, 14L, 15L);
            var ex = assertThrows(BusinessRuleException.class, () ->
                    dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, caIds)));
            assertTrue(ex.getMessage().contains("tối đa 2 ca"));
        }

        @Test
        @DisplayName("Ngoại lệ: cổng đăng ký đã đóng -> BusinessRuleException")
        void congDaDong() {
            week37.setTrangThai(TrangThaiTuan.DA_DONG_DANG_KY);
            List<Long> caIds = List.of(10L, 20L, 11L, 21L, 12L, 22L, 13L, 23L);
            var ex = assertThrows(BusinessRuleException.class, () ->
                    dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, caIds)));
            assertTrue(ex.getMessage().contains("không trong giai đoạn mở"));
        }

        @Test
        @DisplayName("Ngoại lệ: chọn ca của khoa khác -> BusinessRuleException")
        void caKhoaKhac() {
            CaTrucKhoa caNoi = ca(999, khoaNoi, 0, caSang);
            mockCaKhoa.add(caNoi);
            when(caTrucKhoaRepository.findById(999L)).thenReturn(Optional.of(caNoi));

            List<Long> caIds = List.of(10L, 20L, 11L, 21L, 12L, 22L, 13L, 999L);
            var ex = assertThrows(BusinessRuleException.class, () ->
                    dangKyCaService.luuDangKy("bs.nguyenvana", new DangKyCaRequest(37L, caIds)));
            assertTrue(ex.getMessage().contains("không thuộc"));
        }
    }

    // ==========================================================
    // MODULE 2: PHÂN CÔNG CA TRỰC
    // ==========================================================
    @Nested
    @DisplayName("Module 2 – Phân công ca trực")
    class PhanCong {

        CaTrucKhoa caMot;
        BangPhanCong bang;

        @BeforeEach
        void init() {
            caMot = ca(10, khoaRHM, 0, caSang);
            bang = BangPhanCong.builder().id(1L).khoa(khoaRHM).tuanLamViec(week37).trangThai(TrangThaiBangPhanCong.DANG_XEP).build();
            when(caTrucKhoaRepository.findById(10L)).thenReturn(Optional.of(caMot));
            when(bangPhanCongRepository.findByKhoaIdAndTuanLamViecId(khoaRHM.getId(), 37L)).thenReturn(List.of(bang));
            when(bangPhanCongRepository.findByCaTrucKhoaIdAndVaiTro(anyLong(), any())).thenReturn(Optional.empty());
            when(bangPhanCongRepository.findByCaTrucKhoaId(10L)).thenReturn(List.of());
            when(bangPhanCongRepository.save(any(BangPhanCong.class))).thenAnswer(inv -> inv.getArgument(0));
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
        @DisplayName("Thành công: điều động ngoài đăng ký khi chưa đăng ký")
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
            when(bangPhanCongRepository.countByNhanSuYTeIdAndCaTrucKhoa_NgayTrongTuan_Id(doctorA.getId(), 1L)).thenReturn(2L);
            var ex = assertThrows(BusinessRuleException.class, () -> phanCongService.phanCong("ql.levanc",
                    new PhanCongRequest(10L, doctorA.getId(), VaiTroPhanCong.BAC_SI_CHINH, false, null)));
            assertTrue(ex.getMessage().contains("đã đủ 2 ca"));
        }

        @Test
        @DisplayName("Ngoại lệ: lịch đã công bố thì không được sửa")
        void daCongBo() {
            bang.setTrangThai(TrangThaiBangPhanCong.DA_CONG_BO);
            when(bangPhanCongRepository.findByCaTrucKhoaIdAndVaiTro(10L, VaiTroPhanCong.BAC_SI_CHINH)).thenReturn(Optional.of(bang));
            var ex = assertThrows(BusinessRuleException.class, () -> phanCongService.phanCong("ql.levanc",
                    new PhanCongRequest(10L, doctorA.getId(), VaiTroPhanCong.BAC_SI_CHINH, true, null)));
            assertTrue(ex.getMessage() != null);
        }

        private void stubTongQuan(List<BangPhanCong> phanCong) {
            when(caTrucKhoaRepository.timTheoTuan(37L)).thenReturn(List.of(caMot));
            when(bangPhanCongRepository.findByTuanLamViecId(37L)).thenReturn(phanCong);
            when(nhanSuYTeRepository.findAll()).thenReturn(List.of(doctorA, nurseB));
            when(khoaRepository.findAll(any(Sort.class))).thenReturn(List.of(khoaRHM));
        }

        @Test
        @DisplayName("Ngoại lệ: không công bố được khi còn ca chưa đủ 1 BS + 2 YT")
        void congBo_chuaDu() {
            stubTongQuan(List.of(BangPhanCong.builder().caTrucKhoa(caMot).vaiTro(VaiTroPhanCong.BAC_SI_CHINH).nhanSuYTe(doctorA).khoa(khoaRHM).tuanLamViec(week37).build()));
            var ex = assertThrows(BusinessRuleException.class, () -> phanCongService.congBoLichTuan(37L));
            assertTrue(ex.getMessage().contains("K04 (0/1)"));
        }

        @Test
        @DisplayName("Thành công: công bố khi tất cả các ca đã đủ người")
        void congBo_thanhCong() {
            stubTongQuan(List.of(
                    BangPhanCong.builder().caTrucKhoa(caMot).vaiTro(VaiTroPhanCong.BAC_SI_CHINH).nhanSuYTe(doctorA).khoa(khoaRHM).tuanLamViec(week37).build(),
                    BangPhanCong.builder().caTrucKhoa(caMot).vaiTro(VaiTroPhanCong.Y_TA_1).nhanSuYTe(nurseB).khoa(khoaRHM).tuanLamViec(week37).build(),
                    BangPhanCong.builder().caTrucKhoa(caMot).vaiTro(VaiTroPhanCong.Y_TA_2).nhanSuYTe(nurseB).khoa(khoaRHM).tuanLamViec(week37).build()));

            var kq = phanCongService.congBoLichTuan(37L);

            assertEquals(3, kq.tongSoCa());
            assertEquals(TrangThaiTuan.DA_CONG_BO, week37.getTrangThai());
        }
    }

    // ==========================================================
    // MODULE 3: KHIẾU NẠI & THANH TOÁN
    // ==========================================================
    @Nested
    @DisplayName("Module 3 – Khiếu nại & thanh toán")
    class ThanhToan {

        BangPhanCong pcSang;
        BangPhanCong pcToiLoi;

        @BeforeEach
        void init() {
            pcSang = BangPhanCong.builder().id(50L).nhanSuYTe(doctorA).khoa(khoaRHM).tuanLamViec(week37)
                    .caTrucKhoa(ca(1, khoaRHM, 0, caSang)).vaiTro(VaiTroPhanCong.BAC_SI_CHINH).build();
            pcToiLoi = BangPhanCong.builder().id(51L).nhanSuYTe(doctorA).khoa(khoaRHM).tuanLamViec(week37)
                    .caTrucKhoa(ca(3, khoaRHM, 0, caToi)).vaiTro(VaiTroPhanCong.BAC_SI_CHINH).build();
            when(bangPhanCongRepository.findById(51L)).thenReturn(Optional.of(pcToiLoi));
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
                    .bangPhanCong(pcToiLoi).trangThai("CHO_XU_LY").ngayGui(LocalDateTime.now()).build();
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
            List<BangPhanCong> ds = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                ds.add(BangPhanCong.builder().id(60L + i).khoa(khoaRHM).tuanLamViec(week37).nhanSuYTe(doctorA)
                        .caTrucKhoa(ca(20 + i, khoaRHM, i, caSang)).vaiTro(VaiTroPhanCong.BAC_SI_CHINH).build());
            }
            ds.add(BangPhanCong.builder().id(70L).khoa(khoaRHM).tuanLamViec(week37).nhanSuYTe(doctorA)
                    .caTrucKhoa(ca(30, khoaRHM, 4, caToi)).vaiTro(VaiTroPhanCong.BAC_SI_CHINH).build());
            ds.add(pcToiLoi);
            when(bangPhanCongRepository.findByNhanSuYTeIdAndTuanLamViecId(doctorA.getId(), 37L)).thenReturn(ds);
            when(chamCongRepository.findByBangPhanCongId(anyLong())).thenReturn(Optional.empty());
            when(chamCongRepository.findByBangPhanCongId(51L)).thenReturn(Optional.of(
                    ChamCong.builder().id(1L).bangPhanCong(pcToiLoi).trangThai(TrangThaiChamCong.LOI_THIET_BI).build()));

            YeuCauKhieuNai kn = YeuCauKhieuNai.builder().id(88L).maYeuCau("YCKN-1").nhanSu(doctorA).bangPhanCong(pcToiLoi)
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

    @Nested
    @DisplayName("Hồ sơ cá nhân & Cập nhật thông tin CSDL (AuthService)")
    class HoSoCaNhanTests {

        @Test
        @DisplayName("Cập nhật thông tin cá nhân và lưu vào CSDL thành công")
        void capNhatThongTinCaNhanThanhCong() {
            when(thanhVienRepository.findByTenDangNhap("bs.nguyenvana")).thenReturn(Optional.of(doctorA));
            when(diaChiRepository.save(any(DiaChi.class))).thenAnswer(i -> {
                DiaChi dc = i.getArgument(0);
                dc.setId(10L);
                return dc;
            });
            when(thanhVienRepository.save(any(ThanhVien.class))).thenAnswer(i -> i.getArgument(0));

            var req = UpdateProfileRequest.builder()
                    .fullName("BS. Nguyễn Văn An")
                    .soDienThoai("0988776655")
                    .email("an.nguyen@medcare.vn")
                    .ngaySinh(LocalDate.of(1988, 5, 20))
                    .diaChi("Số 12, Phố Huế, Hoàn Kiếm, Hà Nội")
                    .soTaiKhoan("9999888877")
                    .tenNganHang("MB Bank")
                    .chuTaiKhoan("NGUYEN VAN AN")
                    .build();

            var me = authService.updateProfile("bs.nguyenvana", List.of("ROLE_NHAN_SU"), req);

            assertNotNull(me);
            assertEquals("Nguyễn Văn An", me.fullName());
            assertEquals("0988776655", me.soDienThoai());
            assertEquals("an.nguyen@medcare.vn", me.email());
            assertEquals(LocalDate.of(1988, 5, 20), me.ngaySinh());
            assertEquals("9999888877", me.soTaiKhoan());
            assertEquals("MB Bank", me.tenNganHang());
            assertEquals("NGUYEN VAN AN", me.chuTaiKhoan());
            assertTrue(me.diaChi().contains("Hà Nội"));

            verify(thanhVienRepository, times(1)).save(doctorA);
            verify(diaChiRepository, times(1)).save(any(DiaChi.class));
        }

        @Test
        @DisplayName("Báo lỗi khi cập nhật tài khoản không tồn tại")
        void nguoiDungKhongTonTai() {
            when(thanhVienRepository.findByTenDangNhap("unknown")).thenReturn(Optional.empty());
            var req = UpdateProfileRequest.builder().fullName("Không Tồn Tại").build();

            assertThrows(ResourceNotFoundException.class, () ->
                    authService.updateProfile("unknown", List.of("ROLE_NHAN_SU"), req));
        }
    }
}
