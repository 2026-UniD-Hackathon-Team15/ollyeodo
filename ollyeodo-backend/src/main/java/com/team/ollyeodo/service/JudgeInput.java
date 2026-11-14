package com.team.ollyeodo.service;

import com.team.ollyeodo.ai.CaptionClaims;
import com.team.ollyeodo.ai.ExtractedAttrs;
import java.time.LocalDate;

/** RuleEngine 입력 (PM 확정 인터페이스, 이름·필드 변경 금지) */
public record JudgeInput(
        LocalDate asOf,                 // 판정 기준일 (null이면 오늘, Asia/Seoul)
        ComparisonResult run1,          // compare(ai.first(),  real.first(),  null)
        ComparisonResult run2,          // compare(ai.second(), real.second(), null)
        CaptionClaims captionClaims,
        ExtractedAttrs realFirst,       // CAP_FACT 대조용 (실물 값)
        ExtractedAttrs aiFirst,         // AI_VIRTUAL_EXPERT용 people·visible_text
        ExtractedAttrs aiSecond) {}