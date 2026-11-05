package com.medcare.clinic.mapper;

import com.medcare.clinic.dto.auth.JwtResponse;
import com.medcare.clinic.dto.auth.MeResponse;
import com.medcare.clinic.entity.*;
import com.medcare.clinic.service.HoSoMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class UserMapper {

    public JwtResponse toJwtResponse(ThanhVien tv, String token, List<String> roles) {
        if (tv == null) return null;
        return JwtResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .id(tv.getId())
                .username(tv.getTenDangNhap())
                .fullName(tv.getHoVaTen())
                .roles(roles)
                .build();
    }

    public MeResponse toMeResponse(ThanhVien tv, String vaiTro, List<String> roles) {
        if (tv == null) return null;

        Long khoaId = null;
        String maKhoa = null;
        String tenKhoa = null;
        BigDecimal donGiaCa = null;

        ThanhVien real = HoSoMapper.thuc(tv);
        if (real instanceof NhanSuYTe ns && ns.getKhoa() != null) {
            khoaId = ns.getKhoa().getId();
            maKhoa = ns.getKhoa().getMaKhoa();
            tenKhoa = ns.getKhoa().getTen();
            donGiaCa = ns.getDonGiaTienCong();
        }

        String diaChiStr = null;
        if (tv.getDiaChi() != null) {
            DiaChi dc = tv.getDiaChi();
            StringBuilder sb = new StringBuilder();
            if (dc.getSoNha() != null && !dc.getSoNha().isBlank()) sb.append(dc.getSoNha()).append(", ");
            if (dc.getToaNha() != null && !dc.getToaNha().isBlank()) sb.append(dc.getToaNha()).append(", ");
            if (dc.getXomPho() != null && !dc.getXomPho().isBlank()) sb.append(dc.getXomPho()).append(", ");
            if (dc.getPhuongXa() != null && !dc.getPhuongXa().isBlank()) sb.append(dc.getPhuongXa()).append(", ");
            if (dc.getQuanHuyen() != null && !dc.getQuanHuyen().isBlank()) sb.append(dc.getQuanHuyen()).append(", ");
            if (dc.getTinhThanh() != null && !dc.getTinhThanh().isBlank()) sb.append(dc.getTinhThanh());
            diaChiStr = sb.toString().replaceAll(", $", "");
        }

        return new MeResponse(
                tv.getId(),
                tv.getTenDangNhap(),
                tv.getHoVaTen(),
                roles,
                vaiTro,
                HoSoMapper.maNhanSu(tv),
                HoSoMapper.chucDanh(tv),
                HoSoMapper.hocVi(tv),
                khoaId,
                maKhoa,
                tenKhoa,
                donGiaCa,
                tv.getSoTaiKhoan(),
                tv.getTenNganHang(),
                tv.getChuTaiKhoan(),
                tv.getEmail(),
                tv.getSoDienThoai(),
                tv.getNgaySinh(),
                diaChiStr
        );
    }
}

