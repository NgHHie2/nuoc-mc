package com.example.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Cấu hình Security Filter Chain cho WebFlux
     * Tắt tất cả các authentication mặc định của Spring Security
     * vì API Gateway sẽ xử lý authentication thông qua JWT filter riêng
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(formLogin -> formLogin.disable()) // Tắt form login
                .httpBasic(httpBasic -> httpBasic.disable()) // Tắt HTTP Basic auth
                .logout(logout -> logout.disable()) // Tắt logout endpoint mặc định
                // Quan trọng: Tắt hết authentication mặc định của Spring Security
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll() // Cho phép tất cả request đi qua
                )
                .build();
    }

    /**
     * Cấu hình CORS (Cross-Origin Resource Sharing)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(
                Arrays.asList("Authorization", "X-User-Id", "X-User-Role", "X-Positions", "Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}