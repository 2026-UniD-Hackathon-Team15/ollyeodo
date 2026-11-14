package com.team.ollyeodo.ai;

/**
 * 실제 사진과 AI 사진이 다른 음식이라 비교할 수 없을 때 (검수 불가, MVP §6.3-9).
 * 판정(🔴/🟢)을 내지 않고 422로 바로 돌려보낸다.
 */
public class DifferentFoodException extends ExtractionException {
    public static final String MESSAGE = "실제 사진과 AI 사진이 다른 음식이라 검수할 수 없어요. 같은 메뉴의 사진을 올려 주세요.";

    public DifferentFoodException() {
        super(MESSAGE, null);
    }
}
