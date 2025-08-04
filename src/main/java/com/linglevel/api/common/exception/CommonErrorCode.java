package com.linglevel.api.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommonErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다");
    
    private final HttpStatus status;
    private final String message;
}
