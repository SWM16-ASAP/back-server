package com.linglevel.api.auth.config;

import com.linglevel.api.users.entity.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Profile({"dev", "local"})
@Configuration
@EnableWebSecurity
public class SwaggerFormLoginSecurityConfig {

    private final PasswordEncoder passwordEncoder;
    @Value("${api.docs.user.username}")
    private String username;
    @Value("${api.docs.user.password}")
    private String password;

    public SwaggerFormLoginSecurityConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        
        http.securityMatcher("/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**", "/login")
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form
                        .defaultSuccessUrl("/swagger-ui/index.html", true)
                        .permitAll()
                )
                .userDetailsService(swaggerUserDetailsService());

        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager swaggerUserDetailsService() {
        UserDetails user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .roles("SWAGGER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }
}
