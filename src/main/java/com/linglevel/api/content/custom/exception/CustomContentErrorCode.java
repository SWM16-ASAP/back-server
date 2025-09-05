package com.linglevel.api.content.custom.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CustomContentErrorCode {
    // 요청 관련
    CONTENT_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "콘텐츠 처리 요청을 찾을 수 없습니다."),
    CONTENT_REQUEST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 콘텐츠 요청에 대한 권한이 없습니다."),
    INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 콘텐츠 타입입니다."),
    INVALID_DIFFICULTY_LEVEL(HttpStatus.BAD_REQUEST, "유효하지 않은 난이도 레벨입니다."),
    INVALID_REQUEST_STATUS(HttpStatus.BAD_REQUEST, "요청이 처리 중 상태가 아닙니다."),
    
    // 커스텀 콘텐츠 관련
    CUSTOM_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "커스텀 콘텐츠를 찾을 수 없습니다."),
    CUSTOM_CONTENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인이 생성한 커스텀 콘텐츠만 삭제할 수 있습니다."),
    CUSTOM_CONTENT_CHUNK_NOT_FOUND(HttpStatus.NOT_FOUND, "커스텀 콘텐츠 청크를 찾을 수 없습니다."),
    
    // 서비스 관련
    SERVICE_NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "해당 기능이 아직 구현되지 않았습니다."),
    IMPORT_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "콘텐츠 처리 중 오류가 발생했습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    AI_INPUT_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 입력 데이터 업로드에 실패했습니다.");
    
    private final HttpStatus status;
    private final String message;
}