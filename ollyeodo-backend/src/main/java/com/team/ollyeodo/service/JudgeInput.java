package com.team.ollyeodo.service;

import com.team.ollyeodo.ai.CaptionClaims;
import com.team.ollyeodo.ai.CaptionReview;
import com.team.ollyeodo.ai.ExtractedAttrs;
import java.time.LocalDate;

/** RuleEngine 입력 (PM 확정 인터페이스). 뒤 4개는 MVP §6.1.1·§6.2 AI 판단용으로 추가 (null 허용) */
public record JudgeInput(
        LocalDate asOf,                 // 판정 기준일 (null이면 오늘, Asia/Seoul)
        ComparisonResult run1,          // compare(ai.first(),  real.first(),  null)
        ComparisonResult run2,          // compare(ai.second(), real.second(), null)
        CaptionClaims captionClaims,
        ExtractedAttrs realFirst,       // CAP_FACT 대조용 (실물 값), 적용 판단 1회차
        ExtractedAttrs aiFirst,         // AI_VIRTUAL_EXPERT용 people·visible_text
        ExtractedAttrs aiSecond,
        ExtractedAttrs realSecond,      // 적용 판단 2회차 (§6.3-5)
        String caption,                 // 메뉴 설명 원문 (인용 검증·간이 검수용)
        CaptionReview review1,          // 문구 분류 1회차 (null이면 간이 검수)
        CaptionReview review2) {        // 문구 분류 2회차

    public JudgeInput(LocalDate asOf, ComparisonResult run1, ComparisonResult run2, CaptionClaims captionClaims,
                      ExtractedAttrs realFirst, ExtractedAttrs aiFirst, ExtractedAttrs aiSecond) {
        this(asOf, run1, run2, captionClaims, realFirst, aiFirst, aiSecond, null, null, null, null);
    }
}
