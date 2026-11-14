package com.team.ollyeodo.ai;

import java.time.Duration;

/**
 * 제공자 어댑터 경계. 구현은 동시 호출에 안전해야 하며 전달된 제한시간을
 * HTTP 연결/응답에도 적용해야 한다. 서비스 타임아웃만으로 원격 요청은 중단되지 않는다.
 * 프롬프트와 입력은 별도 메시지로 전달하고 구조화 응답을 record로 변환한다.
 */
public interface ExtractionClient {
    ExtractedAttrs extractImage(String prompt, ImageInput image, Duration timeout);
    CaptionClaims extractCaption(String prompt, String caption, Duration timeout);
}
