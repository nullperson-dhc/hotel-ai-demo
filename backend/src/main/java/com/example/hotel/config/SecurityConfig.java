package com.example.hotel.config;

import com.example.hotel.domain.Status;
import com.example.hotel.repository.StaffUserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {
    @Bean
    UserDetailsService users(StaffUserRepository repo) {
        return username -> {
            var s =
                    repo.findByUsername(username)
                            .filter(x -> x.getStatus() == Status.ACTIVE)
                            .orElseThrow(() -> new UsernameNotFoundException("invalid"));
            return User.withUsername(s.getUsername())
                    .password(s.getPasswordHash())
                    .roles("STAFF")
                    .build();
        };
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookiePath("/");
        http.csrf(
                c ->
                        c.csrfTokenRepository(csrf)
                                .ignoringRequestMatchers(
                                        "/api/v1/bookings/**",
                                        "/api/v1/staff/session",
                                        "/api/v1/staff/bookings/**"));
        http.authorizeHttpRequests(
                a ->
                        a.requestMatchers("/api/v1/staff/bookings/**")
                                .hasRole("STAFF")
                                .anyRequest()
                                .permitAll());
        http.exceptionHandling(
                e ->
                        e.authenticationEntryPoint(
                                (q, r, x) -> {
                                    r.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                    r.setContentType("application/json;charset=UTF-8");
                                    r.getWriter()
                                            .write(
                                                    "{\"code\":\"UNAUTHORIZED\",\"message\":\"请先登录\",\"traceId\":\""
                                                            + q.getAttribute("traceId")
                                                            + "\"}");
                                }));
        http.headers(h -> h.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }
}
