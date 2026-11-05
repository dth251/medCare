package com.medcare.clinic.config;

import com.medcare.clinic.entity.*;
import com.medcare.clinic.repository.ThanhVienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final ThanhVienRepository thanhVienRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ThanhVien user = thanhVienRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

        user = (ThanhVien) org.hibernate.Hibernate.unproxy(user);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user instanceof NhanVien nv) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + nv.getViTriCongViec().name()));
        } else if (user instanceof BacSi) {
            authorities.add(new SimpleGrantedAuthority("ROLE_BAC_SI"));
            authorities.add(new SimpleGrantedAuthority("ROLE_NHAN_SU"));
        } else if (user instanceof YTa) {
            authorities.add(new SimpleGrantedAuthority("ROLE_Y_TA"));
            authorities.add(new SimpleGrantedAuthority("ROLE_NHAN_SU"));
        } else if (user instanceof NhanSuYTe) {
            authorities.add(new SimpleGrantedAuthority("ROLE_NHAN_SU"));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getTenDangNhap(),
                user.getMatKhau(),
                authorities
        );
    }
}
