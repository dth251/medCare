package com.medcare.clinic.entity;

import com.medcare.clinic.enums.LoaiCa;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "tblCaLamViec")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CaLamViec {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String tenCa;

    @Column(columnDefinition = "TEXT")
    private String moTa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoaiCa loaiCa;

    @Column(nullable = false)
    private LocalTime gioBatDau;

    @Column(nullable = false)
    private LocalTime gioKetThuc;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal heSoLuong;
}
