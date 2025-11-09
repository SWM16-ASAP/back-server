package com.linglevel.api.common.util;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class UrlNormalizer {

    /**
     * URL을 정규화하여 반환
     *
     * @param url 원본 URL
     * @return 정규화된 URL
     */
    public static String normalize(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }

        try {
            String decodedUrl = decodeUrl(url.trim());

            URI uri = new URI(decodedUrl);

            // 프로토콜 정규화 (https로 통일)
            String scheme = normalizeScheme(uri.getScheme());

            // 호스트 정규화 (소문자, www 제거)
            String host = normalizeHost(uri.getHost());

            // 포트 정규화 (기본 포트는 제거)
            int port = normalizePort(uri.getPort(), scheme);

            // 경로 정규화 (트레일링 슬래시 제거, 중복 슬래시 제거)
            String path = normalizePath(uri.getPath());

            // 쿼리 파라미터 정규화 (정렬)
            String query = normalizeQuery(uri.getQuery());
            
            // 정규화된 URL 구성
            StringBuilder normalized = new StringBuilder();
            normalized.append(scheme).append("://").append(host);

            if (port != -1) {
                normalized.append(":").append(port);
            }

            if (path != null && !path.isEmpty()) {
                normalized.append(path);
            }

            if (query != null && !query.isEmpty()) {
                normalized.append("?").append(query);
            }

            return normalized.toString();

        } catch (URISyntaxException e) {
            log.warn("Failed to normalize URL: {}", url, e);
            return url; // 정규화 실패 시 원본 반환
        }
    }

    /**
     * URL 디코딩 (재귀적으로 완전히 디코딩)
     */
    private static String decodeUrl(String url) {
        try {
            String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
            // 디코딩 결과가 동일하면 더 이상 인코딩되지 않음
            if (decoded.equals(url)) {
                return url;
            }
            return decodeUrl(decoded);
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * 스키마 정규화 (https로 통일)
     */
    private static String normalizeScheme(String scheme) {
        if (scheme == null) {
            return "https";
        }

        scheme = scheme.toLowerCase();

        // http를 https로 변환
        if ("http".equals(scheme)) {
            return "https";
        }

        return scheme;
    }

    /**
     * 호스트 정규화 (소문자 변환, www 제거)
     */
    private static String normalizeHost(String host) {
        if (host == null) {
            return null;
        }

        host = host.toLowerCase();

        // www 제거
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }

        return host;
    }

    /**
     * 포트 정규화 (기본 포트 제거)
     */
    private static int normalizePort(int port, String scheme) {
        // 기본 포트인 경우 -1 반환 (URL에서 제외)
        if (port == 80 && "http".equals(scheme)) {
            return -1;
        }
        if (port == 443 && "https".equals(scheme)) {
            return -1;
        }

        return port;
    }

    /**
     * 경로 정규화
     */
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "";
        }

        // 트레일링 슬래시 제거
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        // 중복 슬래시 제거
        path = path.replaceAll("/+", "/");

        return path;
    }

    /**
     * 쿼리 파라미터 정규화 (알파벳 순 정렬)
     */
    private static String normalizeQuery(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        // 쿼리 파라미터를 & 기준으로 분리하고 정렬
        return Arrays.stream(query.split("&"))
                .filter(param -> !param.isEmpty())
                .sorted()
                .collect(Collectors.joining("&"));
    }
}