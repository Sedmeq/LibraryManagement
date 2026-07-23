package org.example.librarymanagement.config;

import lombok.RequiredArgsConstructor;

import org.example.librarymanagement.security.CustomAccessDeniedHandler;
import org.example.librarymanagement.security.CustomAuthenticationEntryPoint;
import org.example.librarymanagement.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final
    CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    /** Oxu (GET) və yazma (POST/PUT/DELETE) əməliyyatları eyni resurslar üzərində. */
    private static final String[] LIBRARY_RESOURCES = {
            "/api/v1/books/**",
            "/api/v1/authors/**",
            "/api/v1/members/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Qeydiyyat/login və Swagger sənədləri hər kəsə açıqdır
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // Yazma (create/update/delete) əməliyyatları yalnız ADMIN üçün
                        .requestMatchers(HttpMethod.POST, LIBRARY_RESOURCES).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, LIBRARY_RESOURCES).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, LIBRARY_RESOURCES).hasRole("ADMIN")

                        // Yalnız ADMIN-ə açıq idarəetmə endpoint-ləri
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Oxuma (GET) əməliyyatları həm USER, həm ADMIN üçün əlçatandır
                        .requestMatchers(HttpMethod.GET, LIBRARY_RESOURCES).hasAnyRole("USER", "ADMIN")

                        // Cari istifadəçinin öz profili — istənilən doğrulanmış istifadəçi
                        .requestMatchers("/api/v1/users/me").hasAnyRole("USER", "ADMIN")

                        // Qalan hər şey üçün ən azı autentifikasiya tələb olunur
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // 401 — token yoxdur/yanlışdır/vaxtı bitib
                        .authenticationEntryPoint(authenticationEntryPoint)
                        // 403 — token etibarlıdır, amma rol kifayət etmir
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
