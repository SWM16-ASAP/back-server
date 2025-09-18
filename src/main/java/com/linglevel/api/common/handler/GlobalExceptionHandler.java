package com.linglevel.api.common.handler;

import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.exception.CommonErrorCode;
import com.linglevel.api.common.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ExceptionResponse> handleCommonException(CommonException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ExceptionResponse> handleNoHandlerFoundException(NoResourceFoundException e) {
        CommonException commonException = new CommonException(CommonErrorCode.RESOURCE_NOT_FOUND);
        return ResponseEntity.status(commonException.getStatus())
                .body(new ExceptionResponse(commonException));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationException(MethodArgumentNotValidException e) {
        String specificError = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("입력 값 검증에 실패했습니다");
        
        CommonException commonException = new CommonException(CommonErrorCode.INVALID_INPUT, specificError);
        log.warn("Validation error: {}", specificError);
        return ResponseEntity.status(commonException.getStatus())
                .body(new ExceptionResponse(commonException));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionResponse> handleConstraintViolationException(ConstraintViolationException e) {
        String specificError = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .findFirst()
                .orElse("입력 값 제약 조건 위반");
        
        CommonException commonException = new CommonException(CommonErrorCode.INVALID_INPUT, specificError);
        log.warn("Constraint violation: {}", specificError);
        return ResponseEntity.status(commonException.getStatus())
                .body(new ExceptionResponse(commonException));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ExceptionResponse> handleOptimisticLockingFailureException(OptimisticLockingFailureException e) {
        CommonException commonException = new CommonException(CommonErrorCode.REQUEST_CONFLICT);
        log.warn("Optimistic locking failure occurred: {}", e.getMessage());
        return ResponseEntity.status(commonException.getStatus())
                .body(new ExceptionResponse(commonException));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGenericException(Exception e) {
        CommonException commonException = new CommonException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        log.error("Unexpected error occurred", e);
        return ResponseEntity.status(commonException.getStatus())
                .body(new ExceptionResponse(commonException));
    }
}