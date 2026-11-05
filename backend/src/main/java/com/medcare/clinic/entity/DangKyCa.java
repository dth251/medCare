package com.medcare.clinic.entity;

import com.medcare.clinic.enums.TrangThaiDangKyCa;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblDangKyCa", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"phieuDangKyId", "caTrucKhoaId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DangKyCa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime ngayDangKy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrangThaiDangKyCa trangThai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phieuDangKyId", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private PhieuDangKy phieuDangKy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caTrucKhoaId", nullable = false)
    private CaTrucKhoa caTrucKhoa;
}
