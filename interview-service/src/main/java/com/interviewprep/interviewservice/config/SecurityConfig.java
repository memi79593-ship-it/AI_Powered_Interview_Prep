package com.interviewprep.interviewservice.config;

import com.interviewprep.interviewservice.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Security configuration – Day 8.
 *
 * Public routes: /api/auth/** , /api/interview/health , /h2-console/**
 * Protected routes: everything else requires JWT Bearer token
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Public endpoints ──────────────────────────────
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/interview/health",
                                "/h2-console/**",
                                "/actuator/**")
                        .permitAll()
                        // ── All other endpoints require valid JWT ──────────
                        .anyRequest().authenticated())
                // Allow H2 console frames
                .headers(h -> h.frameOptions(fo -> fo.sameOrigin()))
                // Plug in JWT filter before default auth filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration – allows React frontend on port 3000.
     * Change allowedOrigins for production (your actual domain).
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns(
                                "http://localhost:*", // All localhost ports
                                "http://127.0.0.1:*", // All localhost IP ports
                                "https://*.vercel.app",
                                "https://*.onrender.com"
                )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false);
            }
        };
    }
}
