package com.medcare.clinic.entity;

import com.medcare.clinic.enums.TrangThaiChamCong;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblChamCong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChamCong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime gioCheckIn;
    private LocalDateTime gioCheckOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrangThaiChamCong trangThai;

    @Column(columnDefinition = "TEXT")
    private String ghiChu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bangPhanCongId")
    private BangPhanCong bangPhanCong;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phanCongCaId")
    private PhanCongCa phanCongCa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phieuThanhToanId")
    private PhieuThanhToan phieuThanhToan;
}
