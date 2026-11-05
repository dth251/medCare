package com.medcare.clinic.service.impl;

import com.medcare.clinic.config.JwtTokenProvider;
import com.medcare.clinic.dto.auth.JwtResponse;
import com.medcare.clinic.dto.auth.LoginRequest;
import com.medcare.clinic.dto.auth.MeResponse;
import com.medcare.clinic.dto.auth.UpdateProfileRequest;
import com.medcare.clinic.entity.DiaChi;
import com.medcare.clinic.entity.NhanVien;
import com.medcare.clinic.entity.ThanhVien;
import com.medcare.clinic.exception.ResourceNotFoundException;
import com.medcare.clinic.mapper.UserMapper;
import com.medcare.clinic.repository.DiaChiRepository;
import com.medcare.clinic.repository.ThanhVienRepository;
import com.medcare.clinic.service.AuthService;
import com.medcare.clinic.service.HoSoMapper;
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
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ThanhVienRepository thanhVienRepository;
    private final DiaChiRepository diaChiRepository;
    private final UserMapper userMapper;

    @Override
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

        return userMapper.toJwtResponse(user, jwt, roles);
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse me(String username, List<String> roles) {
        ThanhVien user = HoSoMapper.thuc(thanhVienRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại")));

        String vaiTro = xacDinhVaiTro(user);
        return userMapper.toMeResponse(user, vaiTro, roles);
    }

    @Override
    @Transactional
    public MeResponse updateProfile(String username, List<String> roles, UpdateProfileRequest request) {
        ThanhVien user = HoSoMapper.thuc(thanhVienRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại")));

        // 1. Cập nhật họ và tên
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            capNhatHoVaTen(user, request.getFullName());
        } else {
            if (request.getHo() != null) user.setHo(request.getHo().trim());
            if (request.getTenDem() != null) user.setTenDem(request.getTenDem().trim());
            if (request.getTen() != null && !request.getTen().isBlank()) user.setTen(request.getTen().trim());
        }

        // 2. Cập nhật thông tin liên hệ & nhân thân
        if (request.getSoDienThoai() != null) {
            user.setSoDienThoai(request.getSoDienThoai().trim());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim());
        }
        if (request.getNgaySinh() != null) {
            user.setNgaySinh(request.getNgaySinh());
        }

        // 3. Cập nhật thông tin quyết toán & ngân hàng
        if (request.getSoTaiKhoan() != null) {
            user.setSoTaiKhoan(request.getSoTaiKhoan().trim());
        }
        if (request.getTenNganHang() != null) {
            user.setTenNganHang(request.getTenNganHang().trim());
        }
        if (request.getChuTaiKhoan() != null) {
            user.setChuTaiKhoan(request.getChuTaiKhoan().trim());
        }

        // 4. Cập nhật địa chỉ cư trú / công tác
        if (request.getDiaChi() != null) {
            capNhatDiaChi(user, request.getDiaChi());
        }

        // Lưu vào CSDL
        user = thanhVienRepository.save(user);

        String vaiTro = xacDinhVaiTro(user);
        return userMapper.toMeResponse(user, vaiTro, roles);
    }

    private String xacDinhVaiTro(ThanhVien user) {
        if (user instanceof NhanVien nv) {
            return nv.getViTriCongViec().name();
        }
        return "NHAN_SU";
    }

    private void capNhatHoVaTen(ThanhVien tv, String fullName) {
        String trimmed = fullName.trim();
        // Lược bỏ tiền tố xưng hô nếu người dùng nhập kèm (BS., ThS., TS., Y tá...)
        String clean = trimmed.replaceAll("^(?i)(BS\\.|ThS\\.|TS\\.|BSCKI\\.|BSCKII\\.|Bác sĩ|Y tá|CN\\.)\\s*", "").trim();
        if (clean.isBlank()) clean = trimmed;

        String[] parts = clean.split("\\s+");
        if (parts.length == 1) {
            tv.setHo(null);
            tv.setTenDem(null);
            tv.setTen(parts[0]);
        } else if (parts.length == 2) {
            tv.setHo(parts[0]);
            tv.setTenDem(null);
            tv.setTen(parts[1]);
        } else {
            tv.setHo(parts[0]);
            StringBuilder dem = new StringBuilder();
            for (int i = 1; i < parts.length - 1; i++) {
                if (!dem.isEmpty()) dem.append(" ");
                dem.append(parts[i]);
            }
            tv.setTenDem(dem.toString());
            tv.setTen(parts[parts.length - 1]);
        }
    }

    private void capNhatDiaChi(ThanhVien tv, String diaChiStr) {
        String trimmed = diaChiStr.trim();
        if (trimmed.isBlank()) {
            tv.setDiaChi(null);
            return;
        }

        DiaChi dc = tv.getDiaChi();
        if (dc == null) {
            dc = new DiaChi();
        }

        String[] parts = trimmed.split(",");
        if (parts.length == 1) {
            dc.setTinhThanh(parts[0].trim());
        } else if (parts.length == 2) {
            dc.setSoNha(parts[0].trim());
            dc.setTinhThanh(parts[1].trim());
        } else if (parts.length == 3) {
            dc.setSoNha(parts[0].trim());
            dc.setQuanHuyen(parts[1].trim());
            dc.setTinhThanh(parts[2].trim());
        } else {
            dc.setSoNha(parts[0].trim());
            dc.setPhuongXa(parts[1].trim());
            dc.setQuanHuyen(parts[2].trim());
            dc.setTinhThanh(parts[parts.length - 1].trim());
        }

        dc = diaChiRepository.save(dc);
        tv.setDiaChi(dc);
    }
}
