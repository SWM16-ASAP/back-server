package com.linglevel.api.common.filter;

import com.linglevel.api.auth.jwt.JwtClaims;
import com.linglevel.api.common.AbstractRedisTest;
import com.linglevel.api.common.ratelimit.annotation.RateLimit;
import com.linglevel.api.common.ratelimit.config.RateLimitProperties;
import com.linglevel.api.common.ratelimit.filter.RateLimitFilter;
import com.linglevel.api.common.ratelimit.filter.RateLimitResolver;
import com.linglevel.api.user.entity.UserRole;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.GenericContainer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RateLimitFilterTest extends AbstractRedisTest {

    private RateLimitFilter rateLimitFilter;
    private ProxyManager<String> proxyManager;
    private RedisClient redisClient;
    private StatefulRedisConnection<String, byte[]> redisConnection;
    private RateLimitResolver rateLimitResolver;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        // Redis 연결 설정
        GenericContainer<?> redis = getRedisContainer();
        String host = redis.getHost();
        Integer port = redis.getMappedPort(6379);

        RedisURI redisUri = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withTimeout(Duration.ofSeconds(10))
                .build();

        redisClient = RedisClient.create(redisUri);
        redisConnection = redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );

        // ProxyManager 생성
        proxyManager = LettuceBasedProxyManager.builderFor(redisConnection).build();

        // RateLimitProperties 설정
        RateLimitProperties properties = new RateLimitProperties();
        properties.setCapacity(100);
        RateLimitProperties.Refill refill = new RateLimitProperties.Refill();
        RateLimitProperties.Refill.Duration duration = new RateLimitProperties.Refill.Duration();
        duration.setMinutes(1);
        refill.setDuration(duration);
        properties.setRefill(refill);

        // RateLimitResolver mock 생성
        rateLimitResolver = mock(RateLimitResolver.class);
        when(rateLimitResolver.resolveRateLimit(any())).thenReturn(null); // No annotation (default)

        // RateLimitFilter 생성
        rateLimitFilter = new RateLimitFilter(proxyManager, properties, rateLimitResolver);

        // Redis 플러시
        redisConnection.sync().flushall();

        // Clear SecurityContext before each test
        SecurityContextHolder.clearContext();

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getWriter()).thenReturn(printWriter);
    }

    @AfterEach
    void tearDown() {
        if (redisConnection != null) {
            redisConnection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
        SecurityContextHolder.clearContext();
    }

    private JwtClaims createTestUser(String userId, String email, String displayName) {
        return JwtClaims.builder()
                .id(userId)
                .username("google_" + userId)
                .email(email)
                .role(UserRole.USER)
                .provider("google")
                .displayName(displayName)
                .issuedAt(new Date())
                .expiresAt(new Date(System.currentTimeMillis() + 3600000))
                .build();
    }

    /**
     * 사용자를 인증된 상태로 설정
     */
    private void authenticateUser(JwtClaims claims) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                claims,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
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

    @Test
    void testAuthenticatedUserUsesUserId() throws Exception {
        // Given: 인증된 사용자 설정
        JwtClaims user = createTestUser("user123", "user@example.com", "Test User");
        authenticateUser(user);

        // When: 100번 요청
        for (int i = 0; i < 100; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // Then: 101번째 요청은 차단되어야 함
        rateLimitFilter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).setStatus(429);
        String responseBody = stringWriter.toString();
        assertTrue(responseBody.contains("Too many requests"));
    }

    @Test
    void testDifferentUsersHaveSeparateLimits() throws Exception {
        // Given: 첫 번째 사용자
        JwtClaims user1 = createTestUser("user123", "user1@example.com", "User 1");
        authenticateUser(user1);

        // When: 첫 번째 사용자로 100번 요청
        for (int i = 0; i < 100; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // Given: 두 번째 사용자로 변경
        JwtClaims user2 = createTestUser("user456", "user2@example.com", "User 2");
        authenticateUser(user2);

        // When: 두 번째 사용자로 요청 (통과해야 함)
        rateLimitFilter.doFilter(request, response, filterChain);

        // Then: 첫 번째 사용자 100회 + 두 번째 사용자 1회 = 101회 통과
        verify(filterChain, times(101)).doFilter(any(), any());
    }

    @Test
    void testSameUserDifferentIpUseSameLimit() throws Exception {
        // Given: 인증된 사용자
        JwtClaims user = createTestUser("user123", "user@example.com", "Test User");
        authenticateUser(user);

        // When: 첫 번째 IP에서 50번 요청
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        for (int i = 0; i < 50; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // When: 두 번째 IP에서 50번 요청 (같은 사용자)
        when(request.getRemoteAddr()).thenReturn("192.168.0.1");
        for (int i = 0; i < 50; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // Then: 같은 사용자이므로 101번째 요청은 차단되어야 함
        rateLimitFilter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).setStatus(429);
    }

    @Test
    void testUnauthenticatedUserUsesIp() throws Exception {
        // Given: 인증되지 않은 사용자 (SecurityContext가 비어있음)
        SecurityContextHolder.clearContext();

        // When: 100번 요청
        for (int i = 0; i < 100; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // Then: 101번째 요청은 차단되어야 함
        rateLimitFilter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).setStatus(429);
        String responseBody = stringWriter.toString();
        assertTrue(responseBody.contains("Too many requests"));
    }

    // ========== Annotation-based Rate Limiting Tests ==========

    @Test
    void testCustomIpBasedRateLimitWithAnnotation() throws Exception {
        // Given: Mock annotation with 5 requests per minute, IP-based
        RateLimit annotation = mock(RateLimit.class);
        when(annotation.capacity()).thenReturn(5);
        when(annotation.refillMinutes()).thenReturn(1L);
        when(annotation.keyType()).thenReturn(RateLimit.KeyType.IP);
        when(rateLimitResolver.resolveRateLimit(any())).thenReturn(annotation);

        // When: Make 5 requests (should all succeed)
        for (int i = 0; i < 5; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // Then: 6th request should be rate limited
        rateLimitFilter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).setStatus(429);
        String responseBody = stringWriter.toString();
        assertTrue(responseBody.contains("Too many requests"));
    }

    @Test
    void testCustomUserBasedRateLimitWithAnnotation() throws Exception {
        // Given: Mock annotation with 10 requests per minute, USER-based
        RateLimit annotation = mock(RateLimit.class);
        when(annotation.capacity()).thenReturn(10);
        when(annotation.refillMinutes()).thenReturn(1L);
        when(annotation.keyType()).thenReturn(RateLimit.KeyType.USER);
        when(rateLimitResolver.resolveRateLimit(any())).thenReturn(annotation);

        // Authenticate user
        JwtClaims user = createTestUser("test-user-123", "test@example.com", "Test User");
        authenticateUser(user);

        // When: Make 10 requests (should all succeed)
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // Then: 11th request should be rate limited
        rateLimitFilter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).setStatus(429);
        String responseBody = stringWriter.toString();
        assertTrue(responseBody.contains("Too many requests"));
    }

    @Test
    void testCustomAutoRateLimitWithAuthentication() throws Exception {
        // Given: Mock annotation with 15 requests per minute, AUTO (with authenticated user)
        RateLimit annotation = mock(RateLimit.class);
        when(annotation.capacity()).thenReturn(15);
        when(annotation.refillMinutes()).thenReturn(1L);
        when(annotation.keyType()).thenReturn(RateLimit.KeyType.AUTO);
        when(rateLimitResolver.resolveRateLimit(any())).thenReturn(annotation);

        // Authenticate user
        JwtClaims user = createTestUser("auto-user-123", "auto@example.com", "Auto User");
        authenticateUser(user);

        // When: Make 15 requests (should all succeed)
        for (int i = 0; i < 15; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // Then: 16th request should be rate limited
        rateLimitFilter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).setStatus(429);
    }

    @Test
    void testAnnotationOverridesGlobalConfig() throws Exception {
        // Given: Annotation with stricter limit (3 requests) than global (100 requests)
        RateLimit annotation = mock(RateLimit.class);
        when(annotation.capacity()).thenReturn(3);
        when(annotation.refillMinutes()).thenReturn(1L);
        when(annotation.keyType()).thenReturn(RateLimit.KeyType.IP);
        when(rateLimitResolver.resolveRateLimit(any())).thenReturn(annotation);

        // When: Make 3 requests (should succeed)
        for (int i = 0; i < 3; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // Then: 4th request should be rate limited (not 101st, proving annotation overrides global)
        rateLimitFilter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).setStatus(429);
        String responseBody = stringWriter.toString();
        assertTrue(responseBody.contains("Too many requests"));
    }

    @Test
    void testUserBasedKeyTypeWithoutAuthenticationFallsBackToIp() throws Exception {
        // Given: USER key type but no authentication
        RateLimit annotation = mock(RateLimit.class);
        when(annotation.capacity()).thenReturn(5);
        when(annotation.refillMinutes()).thenReturn(1L);
        when(annotation.keyType()).thenReturn(RateLimit.KeyType.USER);
        when(rateLimitResolver.resolveRateLimit(any())).thenReturn(annotation);

        SecurityContextHolder.clearContext(); // Ensure no authentication

        // When: Make 5 requests (should succeed using IP fallback)
        for (int i = 0; i < 5; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // Then: 6th request should be rate limited
        rateLimitFilter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).setStatus(429);
    }
}
