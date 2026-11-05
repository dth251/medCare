package com.medcare.clinic.entity;

import com.medcare.clinic.enums.TrangThaiPhieuDangKy;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tblPhieuDangKy", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"maPhieu"}),
    @UniqueConstraint(columnNames = {"nhanSuId", "tuanLamViecId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PhieuDangKy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String maPhieu;

    @Column(nullable = false)
    private LocalDateTime ngayDangKy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrangThaiPhieuDangKy trangThai;

    @Column(nullable = false)
    private int tongSoCa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhanSuId", nullable = false)
    private NhanSuYTe nhanSuYTe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tuanLamViecId", nullable = false)
    private TuanLamViec tuanLamViec;

    @OneToMany(mappedBy = "phieuDangKy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DangKyCa> chiTietDangKy = new ArrayList<>();
}
