package com.medcare.clinic.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Entity
@Table(name = "tblDiaChi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DiaChi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String soNha;

    @Column(length = 100)
    private String toaNha;

    @Column(length = 150)
    private String xomPho;

    @Column(length = 100)
    private String phuongXa;

    @Column(length = 100)
    private String quanHuyen;

    @Column(length = 100)
    private String tinhThanh;
}
