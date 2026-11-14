package com.team.ollyeodo.service;

import com.team.ollyeodo.domain.Severity;

/** 비교 판정 결과 한 항목. #11에서 Finding 엔티티로 저장한다. */
public record FindingCandidate(
        String ruleCode,
        Severity severity,
        String aiValue,
        String realValue,
        String evidence
) {}