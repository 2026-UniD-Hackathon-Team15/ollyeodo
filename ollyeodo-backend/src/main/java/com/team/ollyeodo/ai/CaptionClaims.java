package com.team.ollyeodo.ai;

import java.util.List;

/**
 * 캡션 주장 추출 초안. MVP §6.2의 수치·재료·원산지와 주의 표현을 담는다.
 * 표현은 원문 그대로 보관하고, 규칙 코드와 심각도는 Java 판정 단계에서 정한다.
 * 캡션 스키마는 MVP에 확정되어 있지 않다.
 */
public record CaptionClaims(
        List<IngredientClaim> ingredients,
        Integer layers,
        String volume,
        List<String> expressions
) {
    public record IngredientClaim(String name, Integer count, String origin) {
    }
}
