package com.medcare.clinic.entity;

import com.medcare.clinic.enums.HinhThucThanhToan;
import com.medcare.clinic.enums.TrangThaiThanhToan;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblPhieuThanhToan", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"maPhieu"}),
    @UniqueConstraint(columnNames = {"nhanSuId", "tuanLamViecId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PhieuThanhToan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String maPhieu;

    @Column(nullable = false)
    private LocalDateTime ngayLap;

    private LocalDateTime ngayThanhToan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HinhThucThanhToan hinhThucThanhToan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrangThaiThanhToan trangThai;

    @Column(nullable = false)
    private int soCaChinhGoc;

    @Column(nullable = false)
    private int soCaNgoaiGioGoc;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal donGiaCaApDung;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal heSoNgoaiGioApDung;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal phuCapTrachNhiem = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal phuCapThuThuat = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal thuong = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal phat = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tongThucLinh;

    @Column(length = 50)
    private String soTaiKhoanNhan;

    @Column(length = 100)
    private String nganHangNhan;

    @Column(length = 100)
    private String chuTaiKhoanNhan;

    @Column(length = 100)
    private String maGiaoDich;

    @Column(length = 255)
    private String minhChungNhanTien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhanSuId", nullable = false)
    private NhanSuYTe nhanSuYTe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tuanLamViecId", nullable = false)
    private TuanLamViec tuanLamViec;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keToanId", nullable = false)
    private NhanVien keToan;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "yeuCauKhieuNaiId")
    private YeuCauKhieuNai yeuCauKhieuNai;
}
