package com.medcare.clinic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medcare.clinic.dto.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Phân quyền theo vai trò:
 *  - ROLE_NHAN_SU (Bác sĩ, Y tá): đăng ký ca, xem chấm công & phiếu lương của chính mình, gửi khiếu nại.
 *  - ROLE_QUAN_LY: lên lịch, phân công, công bố lịch.
 *  - ROLE_KE_TOAN: đối soát chấm công, xử lý khiếu nại, chi trả thù lao.
 * Quyền sở hữu dữ liệu (chỉ xem của chính mình) được kiểm tra thêm ở tầng service.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins:http://localhost:8080,http://127.0.0.1:8080,http://localhost:5500,http://127.0.0.1:5500}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                        writeJson(res, HttpServletResponse.SC_UNAUTHORIZED, "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn"))
                .accessDeniedHandler((req, res, e) ->
                        writeJson(res, HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện chức năng này")))
            .authorizeHttpRequests(auth -> auth
                // Tài nguyên tĩnh của giao diện
                .requestMatchers("/", "/index.html", "/favicon.ico", "/*.html", "/css/**", "/js/**",
                        "/dangky/**", "/phancong/**", "/thanhtoan/**", "/trangchu/**", "/error").permitAll()

                // Xác thực
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/tuan/**").authenticated()

                // 1. Bác sĩ / Y tá
                .requestMatchers("/api/dang-ky-ca/**").hasRole("NHAN_SU")
                .requestMatchers("/api/khieu-nai/gui", "/api/khieu-nai/cua-toi").hasRole("NHAN_SU")
                .requestMatchers("/api/thanh-toan/phieu-cua-toi/**").hasRole("NHAN_SU")
                .requestMatchers(HttpMethod.GET, "/api/thanh-toan/phieu/**", "/api/thanh-toan/cham-cong/**")
                        .hasAnyRole("NHAN_SU", "KE_TOAN")

                // 2. Quản lý điều phối
                .requestMatchers(HttpMethod.GET, "/api/phan-cong/khoa/**").authenticated()
                .requestMatchers("/api/phan-cong/**").hasRole("QUAN_LY")

                // 3. Kế toán tiền lương
                .requestMatchers("/api/khieu-nai/**").hasRole("KE_TOAN")
                .requestMatchers("/api/thanh-toan/**").hasRole("KE_TOAN")

                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeJson(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(res.getOutputStream(), ApiResponse.error(message));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
