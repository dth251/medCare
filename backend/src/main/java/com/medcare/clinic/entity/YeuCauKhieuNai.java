package com.medcare.clinic.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Yêu cầu khiếu nại của nhân sự y tế về kết quả chấm công / tính lương.
 * Đã gộp toàn bộ nội dung của Biên bản điều chỉnh theo thiết kế cập nhật.
 */
@Entity
@Table(name = "tblYeuCauKhieuNai")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class YeuCauKhieuNai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String maYeuCau;

    @Column(nullable = false)
    private LocalDateTime ngayGui;

    private LocalDateTime ngayXuLy;

    @Column(columnDefinition = "TEXT")
    private String dsMinhChung;

    @Column(columnDefinition = "TEXT")
    private String noiDung;

    @Column(columnDefinition = "TEXT")
    private String lyDoPhanHoi;

    @Column(nullable = false, length = 50)
    private String trangThai; // CHO_XU_LY, DA_XU_LY, TU_CHOI

    // Các trường của Biên bản điều chỉnh được gộp trực tiếp vào đây
    @Builder.Default
    private int soCaChinhDieuChinh = 0;

    @Builder.Default
    private int soCaNgoaiGioDieuChinh = 0;

    @Column(precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal donGiaCaApDung = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal heSoNgoaiGioApDung = BigDecimal.valueOf(2.0);

    @Column(precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal phuCapBoSung = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal khauTru = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal soTienDieuChinh = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String lyDo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhanSuId")
    private NhanSuYTe nhanSu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bangPhanCongId")
    private BangPhanCong bangPhanCong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chamCongId")
    private ChamCong chamCong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoiXuLyId")
    private NhanVien nguoiXuLy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phieuThanhToanId")
    private PhieuThanhToan phieuThanhToan;
}
