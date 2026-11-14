package com.team.ollyeodo.api;

import com.team.ollyeodo.ai.ExtractionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = CheckController.class)
public class CheckExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail requestFailed(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }

    /** 다른 음식이라 비교 불가 → 검수 불가 안내를 그대로 보여 준다 */
    @ExceptionHandler(com.team.ollyeodo.ai.DifferentFoodException.class)
    public ProblemDetail differentFood(com.team.ollyeodo.ai.DifferentFoodException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(ExtractionException.class)
    public ProblemDetail extractionFailed(ExtractionException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
                "이미지 분석에 실패했습니다. 실물 사진을 다시 찍어 주세요.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail uploadTooLarge(MaxUploadSizeExceededException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "이미지 용량 제한을 초과했습니다.");
    }
}
