package com.linglevel.api.content.custom.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CustomContentErrorCode {
    // 요청 관련 (4xx)
    CONTENT_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "콘텐츠 처리 요청을 찾을 수 없습니다."),
    CONTENT_REQUEST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 콘텐츠 요청에 대한 접근 권한이 없습니다."),
    INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 콘텐츠 타입입니다."),
    INVALID_DIFFICULTY_LEVEL(HttpStatus.BAD_REQUEST, "유효하지 않은 난이도 레벨입니다."),
    INVALID_REQUEST_STATUS(HttpStatus.CONFLICT, "요청이 처리 가능한 상태가 아닙니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    URL_REQUIRED(HttpStatus.BAD_REQUEST, "URL is required for LINK content type."),
    INVALID_URL_FORMAT(HttpStatus.BAD_REQUEST, "Invalid URL format provided."),
    URL_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "URL is not supported for crawling. Please check supported domains."),
    
    // 커스텀 콘텐츠 관련 (4xx)
    CUSTOM_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "커스텀 콘텐츠를 찾을 수 없습니다."),
    CUSTOM_CONTENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 커스텀 콘텐츠에 대한 접근 권한이 없습니다."),
    CUSTOM_CONTENT_CHUNK_NOT_FOUND(HttpStatus.NOT_FOUND, "커스텀 콘텐츠 청크를 찾을 수 없습니다."),
    
    // 인증/인가 관련 (4xx)
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "인증된 사용자 정보를 찾을 수 없습니다."),
    
    // 외부 서비스 관련 (5xx)
    AI_INPUT_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "AI 서비스 입력 전송에 실패했습니다."),
    AI_RESULT_PROCESSING_FAILED(HttpStatus.BAD_GATEWAY, "AI 서비스 결과 처리에 실패했습니다."),
    
    // 내부 서비스 관련 (5xx)
    IMPORT_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "콘텐츠 가져오기 처리 중 오류가 발생했습니다."),
    WEBHOOK_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "웹훅 처리 중 내부 오류가 발생했습니다."),
    
    // 서비스 상태 관련 (5xx)
    SERVICE_TEMPORARILY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서비스가 일시적으로 이용할 수 없습니다.");
    
    private final HttpStatus status;
    private final String message;
}