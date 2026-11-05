package com.medcare.clinic.entity;

import com.medcare.clinic.enums.TrinhDoHocVi;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Entity
@Table(name = "tblBacSi")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BacSi extends NhanSuYTe {
    @Column(unique = true, nullable = false, length = 30)
    private String maBacSi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrinhDoHocVi hocVi;
}
