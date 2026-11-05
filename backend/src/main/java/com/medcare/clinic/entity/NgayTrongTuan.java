package com.medcare.clinic.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Entity
@Table(name = "tblNgayTrongTuan", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ngay"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NgayTrongTuan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DayOfWeek thu;

    @Column(nullable = false)
    private LocalDate ngay;

    @Column(columnDefinition = "TEXT")
    private String moTa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tuanLamViecId", nullable = false)
    private TuanLamViec tuanLamViec;
}
