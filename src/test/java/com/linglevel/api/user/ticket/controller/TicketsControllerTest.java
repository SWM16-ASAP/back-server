package com.linglevel.api.user.ticket.controller;

import com.linglevel.api.auth.filter.AdminAuthenticationFilter;
import com.linglevel.api.auth.handler.CustomAuthenticationEntryPoint;
import com.linglevel.api.auth.jwt.JwtClaims;
import com.linglevel.api.auth.jwt.JwtService;
import com.linglevel.api.common.ratelimit.config.RateLimitProperties;
import com.linglevel.api.common.ratelimit.filter.RateLimitFilter;
import com.linglevel.api.common.ratelimit.filter.RateLimitResolver;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.entity.UserRole;
import com.linglevel.api.user.repository.UserRepository;
import com.linglevel.api.user.ticket.dto.TicketBalanceResponse;
import com.linglevel.api.user.ticket.dto.TicketTransactionResponse;
import com.linglevel.api.user.ticket.exception.TicketErrorCode;
import com.linglevel.api.user.ticket.exception.TicketException;
import com.linglevel.api.user.ticket.service.TicketService;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketsController.class)
class TicketsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private UserRepository userRepository;

    // SecurityConfig에 필요한 Mock Bean들
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    @MockitoBean
    private AdminAuthenticationFilter adminAuthenticationFilter;
    @MockitoBean
    private RateLimitFilter rateLimitFilter;
    @MockitoBean
    private ProxyManager<String> proxyManager;
    @MockitoBean
    private RateLimitProperties rateLimitProperties;
    @MockitoBean
    private RateLimitResolver rateLimitResolver;

    private User testUser;
    private TicketBalanceResponse ticketBalanceResponse;
    private TicketTransactionResponse ticketTransactionResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("test-user-id")
                .username("testuser")
                .role(UserRole.USER)
                .build();

        ticketBalanceResponse = TicketBalanceResponse.builder()
                .balance(10)
                .updatedAt(LocalDateTime.now())
                .build();

        ticketTransactionResponse = TicketTransactionResponse.builder()
                .id("transaction-id")
                .amount(-5)
                .description("Test transaction")
                .createdAt(LocalDateTime.now())
                .build();

        // Mock filters to pass through the chain
        try {
            doAnswer(invocation -> {
                invocation.getArgument(2, FilterChain.class).doFilter(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }).when(adminAuthenticationFilter).doFilter(any(), any(), any());

            doAnswer(invocation -> {
                invocation.getArgument(2, FilterChain.class).doFilter(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }).when(rateLimitFilter).doFilter(any(), any(), any());
        } catch (Exception e) {
            // This should not happen in a test
        }
    }

    private Authentication getOauthAuthentication() {
        JwtClaims claims = JwtClaims.builder()
                .id(testUser.getId())
                .username(testUser.getUsername())
                .role(testUser.getRole())
                .issuedAt(new Date())
                .expiresAt(new Date(System.currentTimeMillis() + 3600000)) // 1시간 후 만료
                .build();
        return new UsernamePasswordAuthenticationToken(claims, null, List.of(new SimpleGrantedAuthority(testUser.getRole().getSecurityRole())));
    }

    @Test
    void 티켓잔고조회_성공() throws Exception {
        // given
        when(ticketService.getTicketBalance("test-user-id")).thenReturn(ticketBalanceResponse);

        // when & then
        mockMvc.perform(get("/api/v1/tickets/balance")
                        .with(authentication(getOauthAuthentication()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.balance").value(10))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(ticketService).getTicketBalance("test-user-id");
    }

    @Test
    void 티켓잔고조회_사용자없음_500오류() throws Exception {
        // given
        when(ticketService.getTicketBalance(anyString())).thenThrow(new RuntimeException("User not found"));

        // when & then
        mockMvc.perform(get("/api/v1/tickets/balance")
                        .with(authentication(getOauthAuthentication()))
                        .with(csrf()))
                .andExpect(status().isInternalServerError());

        verify(ticketService).getTicketBalance(anyString());
    }

    @Test
    void 티켓잔고조회_인증없음_401오류() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/tickets/balance")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(ticketService, never()).getTicketBalance(anyString());
    }

    @Test
    void 티켓거래내역조회_성공() throws Exception {
        // given
        Page<TicketTransactionResponse> transactionPage = new PageImpl<>(
                List.of(ticketTransactionResponse),
                PageRequest.of(0, 10),
                1L
        );

        when(ticketService.getTicketTransactions("test-user-id", 1, 10))
                .thenReturn(transactionPage);

        // when & then
        mockMvc.perform(get("/api/v1/tickets/transactions")
                        .param("page", "1")
                        .param("limit", "10")
                        .with(authentication(getOauthAuthentication()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value("transaction-id"))
                .andExpect(jsonPath("$.data[0].amount").value(-5))
                .andExpect(jsonPath("$.data[0].description").value("Test transaction"))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(ticketService).getTicketTransactions("test-user-id", 1, 10);
    }

    @Test
    void 티켓거래내역조회_기본파라미터() throws Exception {
        // given
        Page<TicketTransactionResponse> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10),
                0L
        );

        when(ticketService.getTicketTransactions("test-user-id", 1, 10))
                .thenReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/api/v1/tickets/transactions")
                        .with(authentication(getOauthAuthentication()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.totalCount").value(0));

        verify(ticketService).getTicketTransactions("test-user-id", 1, 10);
    }

    @Test
    void 티켓예외처리_잔고부족() throws Exception {
        // given
        when(ticketService.getTicketBalance("test-user-id"))
                .thenThrow(new TicketException(TicketErrorCode.INSUFFICIENT_BALANCE));

        // when & then
        mockMvc.perform(get("/api/v1/tickets/balance")
                        .with(authentication(getOauthAuthentication()))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.message").value("Insufficient ticket balance."));

        verify(ticketService).getTicketBalance("test-user-id");
    }

    @Test
    void 티켓예외처리_티켓없음() throws Exception {
        // given
        when(ticketService.getTicketBalance("test-user-id"))
                .thenThrow(new TicketException(TicketErrorCode.TICKET_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/tickets/balance")
                        .with(authentication(getOauthAuthentication()))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.message").value("Ticket not found."));
    }
}