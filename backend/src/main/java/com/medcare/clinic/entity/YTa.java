package com.medcare.clinic.entity;

import com.medcare.clinic.enums.TrinhDoHocVi;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Entity
@Table(name = "tblYTa")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class YTa extends NhanSuYTe {
    @Column(unique = true, nullable = false, length = 30)
    private String maYTa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrinhDoHocVi trinhDo;
}
