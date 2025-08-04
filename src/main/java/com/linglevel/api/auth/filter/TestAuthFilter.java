package com.linglevel.api.auth.filter;

import com.linglevel.api.users.entity.User;
import com.linglevel.api.users.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@Profile({"dev", "local"})
public class TestAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TestAuthFilter.class);
    private final UserRepository userRepository;

    public TestAuthFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        String testUsername = request.getHeader("X-Test-Username");
        logger.info("TestAuthFilter - Header value: '{}'", testUsername);
        
        if (testUsername != null && !testUsername.trim().isEmpty()) {
            logger.info("TestAuthFilter - Searching for username: '{}'", testUsername);
            Optional<User> userOptional = userRepository.findByUsername(testUsername);
            logger.info("TestAuthFilter - User found: {}", userOptional.isPresent());
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(user.getId(), null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        
        chain.doFilter(request, response);
    }
}