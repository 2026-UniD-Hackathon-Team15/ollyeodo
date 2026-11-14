package com.team.ollyeodo.service;

import com.team.ollyeodo.domain.Severity;

/**
 * RuleEngine(#9)이 확정한 항목별 판정. #9·#10·#11 공통 인터페이스 (이슈 #9 코멘트 참고).
 *
 * @param severity  GREEN | RED (🟢·🔴 2단계, MVP §6)
 * @param evidence  사유. 🔴면 필수
 * @param lawRef    근거 조항. 🔴면 필수 (Rule.lawRef)
 * @param checkOnce 🟢 안내 표시용 (원산지 직접 확인·시행 예정·연출 이미지 권장). 판정을 바꾸지 않는다
 */
public record JudgedFinding(
        String ruleCode,
        Severity severity,
        String aiValue,
        String realValue,
        String evidence,
        String lawRef,
        boolean checkOnce
) {}
