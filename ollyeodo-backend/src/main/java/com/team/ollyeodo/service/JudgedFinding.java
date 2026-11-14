package com.team.ollyeodo.service;

import com.team.ollyeodo.domain.Severity;

/** 최종 판정 한 항목 (PM 확정 인터페이스, 이름·필드 변경 금지) */
public record JudgedFinding(
        String ruleCode,
        Severity severity,      // GREEN | RED (2단계)
        String aiValue,
        String realValue,
        String evidence,        // 🔴면 필수
        String lawRef,          // 🔴면 필수 (Rule.lawRef)
        boolean checkOnce) {}   // 🟢 안내 표시용 (원산지 직접 확인·시행 예정·연출 이미지 권장). 판정 안 바꿈