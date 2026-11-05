package com.medcare.clinic.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "tblThanhVien")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ThanhVien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String tenDangNhap;

    @JsonIgnore
    @Column(nullable = false)
    private String matKhau;

    @Column(length = 50)
    private String ho;

    @Column(length = 50)
    private String tenDem;

    @Column(length = 50)
    private String ten;

    private LocalDate ngaySinh;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String soDienThoai;

    @Column(length = 50)
    private String soTaiKhoan;

    @Column(length = 100)
    private String tenNganHang;

    @Column(length = 100)
    private String chuTaiKhoan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diaChiId")
    private DiaChi diaChi;

    public String getHoVaTen() {
        return (ho != null ? ho + " " : "") + (tenDem != null ? tenDem + " " : "") + (ten != null ? ten : "");
    }
}
