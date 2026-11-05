package com.medcare.clinic.entity;

import com.medcare.clinic.enums.VaiTroPhanCong;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Entity
@Table(name = "tblPhanCongCa", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"caTrucKhoaId", "vaiTro"}),
    @UniqueConstraint(columnNames = {"caTrucKhoaId", "nhanSuId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PhanCongCa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VaiTroPhanCong vaiTro;

    @Column(columnDefinition = "TEXT")
    private String ghiChu;

    @Builder.Default
    @Column(nullable = false)
    private boolean dieuDongNgoaiDangKy = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bangPhanCongId", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private BangPhanCong bangPhanCong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhanSuId", nullable = false)
    private NhanSuYTe nhanSuYTe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caTrucKhoaId", nullable = false)
    private CaTrucKhoa caTrucKhoa;
}
