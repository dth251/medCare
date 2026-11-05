package com.medcare.clinic.entity;

import com.medcare.clinic.enums.TrangThaiTuan;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblTuanLamViec", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"namLamViecId", "ten"}),
    @UniqueConstraint(columnNames = {"ngayBatDau"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TuanLamViec {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String ten;

    @Column(columnDefinition = "TEXT")
    private String moTa;

    @Column(nullable = false)
    private LocalDate ngayBatDau;

    @Column(nullable = false)
    private LocalDate ngayKetThuc;

    private LocalDateTime thoiDiemMoDangKy;
    private LocalDateTime hanDangKy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrangThaiTuan trangThai;

    @Builder.Default
    @Column(nullable = false)
    private int soCaToiThieu = 8;

    @Builder.Default
    @Column(nullable = false)
    private int soCaToiDa = 12;

    @Builder.Default
    @Column(nullable = false)
    private int soCaToiDaMoiNgay = 2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "namLamViecId", nullable = false)
    private NamLamViec namLamViec;
}
