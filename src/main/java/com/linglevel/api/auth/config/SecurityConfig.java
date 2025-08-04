package com.linglevel.api.auth.config;

import com.linglevel.api.auth.jwt.JwtTokenFilter;
import com.linglevel.api.auth.jwt.JwtTokenProvider;
import com.linglevel.api.auth.filter.TestAuthFilter;
import com.linglevel.api.auth.handler.CustomAuthenticationEntryPoint;
import com.linglevel.api.users.repository.UserRepository;
import jakarta.servlet.Filter;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final UserRepository userRepository;
    private final String secretKey;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    
    @Autowired(required = false)
    private TestAuthFilter testAuthFilter;

    public SecurityConfig(UserRepository userRepository, @Value("${jwt.secret}") String secretKey, 
                         CustomAuthenticationEntryPoint authenticationEntryPoint) {
        this.userRepository = userRepository;
        this.secretKey = secretKey;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll() // TODO: 임시 접근 허용
                        .requestMatchers("/api/v1/auth/oauth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                );

        if (testAuthFilter != null) {
            http.addFilterBefore(testAuthFilter, UsernamePasswordAuthenticationFilter.class);
        }
        
        http.addFilterBefore(new JwtTokenFilter(userRepository, secretKey), UsernamePasswordAuthenticationFilter.class);
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

    @Bean
    public JwtTokenProvider jwtTokenProvider() { return new JwtTokenProvider(jwtSecret, accessTokenExpiration, refreshTokenExpiration); }
}
