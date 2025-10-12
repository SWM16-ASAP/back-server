package com.linglevel.api.auth.config;

import com.linglevel.api.auth.filter.AdminAuthenticationFilter;
import com.linglevel.api.auth.filter.TestAuthFilter;
import com.linglevel.api.auth.handler.CustomAuthenticationEntryPoint;
import com.linglevel.api.auth.jwt.JwtFilter;
import com.linglevel.api.common.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtFilter jwtFilter;
    private final AdminAuthenticationFilter adminAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    @Autowired(required = false)
    private TestAuthFilter testAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/actuator/prometheus").hasRole("ADMIN") // 프로메테우스 엔드포인트만 어드민 권한 필요
                        .requestMatchers("/actuator/**").permitAll() // 다른 actuator 엔드포인트들은 공개
                        .requestMatchers("/api/v1/version").permitAll()
                        .requestMatchers("/api/v1/auth/oauth/login").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/custom-contents/webhooks/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                );

        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        if (testAuthFilter != null) {
            http.addFilterBefore(testAuthFilter, UsernamePasswordAuthenticationFilter.class);
        }

        http.addFilterBefore(adminAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
