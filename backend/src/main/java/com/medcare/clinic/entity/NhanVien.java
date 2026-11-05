package com.medcare.clinic.entity;

import com.medcare.clinic.enums.ViTriCongViec;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Entity
@Table(name = "tblNhanVien")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NhanVien extends ThanhVien {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ViTriCongViec viTriCongViec;
}
