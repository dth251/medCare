package com.medcare.clinic.service;

import com.medcare.clinic.config.JwtTokenProvider;
import com.medcare.clinic.dto.auth.JwtResponse;
import com.medcare.clinic.dto.auth.LoginRequest;
import com.medcare.clinic.dto.auth.MeResponse;
import com.medcare.clinic.entity.NhanSuYTe;
import com.medcare.clinic.entity.NhanVien;
import com.medcare.clinic.entity.ThanhVien;
import com.medcare.clinic.exception.ResourceNotFoundException;
import com.medcare.clinic.repository.ThanhVienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ThanhVienRepository thanhVienRepository;

    @Transactional(readOnly = true)
    public JwtResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String jwt = jwtTokenProvider.generateToken(authentication);
        ThanhVien user = thanhVienRepository.findByTenDangNhap(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return JwtResponse.builder()
                .token(jwt)
                .id(user.getId())
                .username(user.getTenDangNhap())
                .fullName(user.getHoVaTen())
                .roles(roles)
                .build();
    }

    @Transactional(readOnly = true)
    public MeResponse me(String username, List<String> roles) {
        ThanhVien user = HoSoMapper.thuc(thanhVienRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại")));

        String vaiTro;
        if (user instanceof NhanVien nv) {
            vaiTro = nv.getViTriCongViec().name();
        } else {
            vaiTro = "NHAN_SU";
        }

        Long khoaId = null;
        String maKhoa = null;
        String tenKhoa = null;
        java.math.BigDecimal donGia = null;
        if (user instanceof NhanSuYTe ns) {
            donGia = ns.getDonGiaTienCong();
            if (ns.getKhoa() != null) {
                khoaId = ns.getKhoa().getId();
                maKhoa = ns.getKhoa().getMaKhoa();
                tenKhoa = ns.getKhoa().getTen();
            }
        }

        return new MeResponse(
                user.getId(),
                user.getTenDangNhap(),
                user.getHoVaTen(),
                roles,
                vaiTro,
                HoSoMapper.maNhanSu(user),
                HoSoMapper.chucDanh(user),
                HoSoMapper.hocVi(user),
                khoaId, maKhoa, tenKhoa,
                donGia,
                user.getSoTaiKhoan(),
                user.getTenNganHang(),
                user.getChuTaiKhoan());
    }
}
