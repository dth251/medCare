package com.medcare.clinic.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Entity
@Table(name = "tblCaTrucKhoa", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"khoaId", "ngayTrongTuanId", "caLamViecId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CaTrucKhoa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String moTa;

    @Builder.Default
    @Column(nullable = false)
    private boolean dangMoCua = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khoaId", nullable = false)
    private Khoa khoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ngayTrongTuanId", nullable = false)
    private NgayTrongTuan ngayTrongTuan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caLamViecId", nullable = false)
    private CaLamViec caLamViec;
}
