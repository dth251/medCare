package com.medcare.clinic.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tblNhanSuYTe")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NhanSuYTe extends ThanhVien {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khoaId", nullable = false)
    private Khoa khoa;

    @Column(name = "donGiaTienCong", precision = 18, scale = 2)
    private BigDecimal donGiaTienCong;
}
