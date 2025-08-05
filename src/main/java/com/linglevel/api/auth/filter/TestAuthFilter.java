package com.linglevel.api.auth.filter;

import com.linglevel.api.users.entity.User;
import com.linglevel.api.users.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@Profile({"dev", "local"})
public class TestAuthFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public TestAuthFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        String testUsername = request.getHeader("X-Test-Username");
        
        if (testUsername != null && !testUsername.trim().isEmpty()) {
            Optional<User> userOptional = userRepository.findByUsername(testUsername);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(user.getId(), null, List.of(new SimpleGrantedAuthority(user.getRole().getSecurityRole())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        
        chain.doFilter(request, response);
    }
}