package com.linglevel.api.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        rateLimitFilter = new RateLimitFilter();

        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    void testAllowsRequestsWithinLimit() throws Exception {
        // 처음 요청은 통과해야 함
        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void testBlocksRequestsExceedingLimit() throws Exception {
        // 100번 요청 (limit까지)
        for (int i = 0; i < 100; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // 101번째 요청은 차단되어야 함
        rateLimitFilter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).setStatus(429);
        verify(response, atLeastOnce()).setContentType("application/json");

        String responseBody = stringWriter.toString();
        assertTrue(responseBody.contains("Too many requests"));
    }

    @Test
    void testDifferentIpAddressesHaveSeparateLimits() throws Exception {
        HttpServletRequest request2 = mock(HttpServletRequest.class);
        when(request2.getRemoteAddr()).thenReturn("192.168.0.1");

        // 첫 번째 IP로 100번 요청
        for (int i = 0; i < 100; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // 두 번째 IP로 요청 (통과해야 함)
        rateLimitFilter.doFilter(request2, response, filterChain);

        verify(filterChain, times(101)).doFilter(any(), any());
    }

    @Test
    void testXForwardedForHeader() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}
