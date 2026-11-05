package com.medcare.clinic.entity;

import com.medcare.clinic.enums.TrangThaiBangPhanCong;
import com.medcare.clinic.enums.VaiTroPhanCong;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bảng phân công ca trực của khoa trong tuần.
 * Đã gộp toàn bộ thông tin phân công ca (nhân sự, ca trực, vai trò, điều động, ghi chú)
 * trực tiếp vào entity này theo thiết kế cập nhật.
 */
@Entity
@Table(name = "tblBangPhanCong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BangPhanCong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String maBang;

    @Column(nullable = false)
    private LocalDateTime ngayLap;

    private LocalDateTime ngayCongBo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrangThaiBangPhanCong trangThai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khoaId", nullable = false)
    private Khoa khoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tuanLamViecId", nullable = false)
    private TuanLamViec tuanLamViec;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoiLapId")
    private NhanVien nguoiLap;

    // Chi tiết phân công ca được gộp trực tiếp vào BangPhanCong
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caTrucKhoaId")
    private CaTrucKhoa caTrucKhoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhanSuId")
    private NhanSuYTe nhanSuYTe;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private VaiTroPhanCong vaiTro;

    @Column(columnDefinition = "TEXT")
    private String ghiChu;

    @Builder.Default
    @Column(nullable = false)
    private boolean dieuDongNgoaiDangKy = false;
}
