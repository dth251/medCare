package com.medcare.clinic.entity;

import com.medcare.clinic.enums.TrangThaiBangPhanCong;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng phân công của một khoa trong một tuần.
 * Chi tiết từng vị trí (BS chính / Y tá 1 / Y tá 2) nằm ở PhanCongCa.
 */
@Entity
@Table(name = "tblBangPhanCong", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"khoaId", "tuanLamViecId"})
})
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
    @JoinColumn(name = "nguoiLapId", nullable = false)
    private NhanVien nguoiLap;

    @OneToMany(mappedBy = "bangPhanCong", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PhanCongCa> chiTietPhanCong = new ArrayList<>();
}
