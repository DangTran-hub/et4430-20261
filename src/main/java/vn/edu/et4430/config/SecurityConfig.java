package vn.edu.et4430.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import vn.edu.et4430.repository.UserAccountRepository;

/**
 * Đăng nhập bằng user_account (username + password_hash BCrypt).
 * role = "PDT"  -> /pdt/**
 * role = "STUDENT" -> /sv/**
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/error").permitAll()
                        .requestMatchers("/pdt/**").hasRole("PDT")
                        .requestMatchers("/sv/**").hasRole("STUDENT")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                // Máy chủ khởi động lại (deploy, hoặc Render ngủ rồi thức dậy) làm mất phiên:
                // form gửi lên mang mã CSRF cũ -> quay về trang đăng nhập thay vì báo lỗi 403.
                .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, denied) -> {
                    if (denied instanceof CsrfException) {
                        response.sendRedirect(request.getContextPath() + "/login?expired");
                    } else {
                        response.sendError(403);
                    }
                }));
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UserAccountRepository userRepo) {
        return username -> userRepo.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .roles(u.getRole())
                        .disabled(!u.isActive())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản " + username));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
