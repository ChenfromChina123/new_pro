package com.aispring.config;

import com.aispring.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import jakarta.servlet.DispatcherType;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                // 允许所有 OPTIONS 请求（预检请求）
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                // 允许异步分发（用于 SSE 等）
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                // WebSocket 端点
                .requestMatchers("/ws/**").permitAll()
                // 允许所有认证端点
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/avatar/**").permitAll()
                .requestMatchers("/api/resources/public").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/public-files", "/api/public-files/**").permitAll()
                // 外部链接公开访问（无需登录）
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/external-links").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/external-links/*/click").permitAll()
                // URL过滤测试端点（公开访问）
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/url-filter/test/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/url-filter/check").permitAll()
                .requestMatchers("/error").permitAll()
                // 其他需要认证
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

