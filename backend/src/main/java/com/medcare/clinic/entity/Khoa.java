package com.medcare.clinic.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Entity
@Table(name = "tblKhoa", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"phongKhamId", "maKhoa"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Khoa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String maKhoa;

    @Column(nullable = false, length = 100)
    private String ten;

    @Column(columnDefinition = "TEXT")
    private String moTa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phongKhamId", nullable = false)
    private PhongKham phongKham;
}
