package com.team.ollyeodo.api;

import com.team.ollyeodo.ai.ExtractionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(assignableTypes = CheckController.class)
public class CheckExceptionHandler {
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
