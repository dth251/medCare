package com.medcare.clinic.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Entity
@Table(name = "tblMinhChungKhieuNai")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MinhChungKhieuNai {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String duongDan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "yeuCauKhieuNaiId")
    private YeuCauKhieuNai yeuCauKhieuNai;
}
