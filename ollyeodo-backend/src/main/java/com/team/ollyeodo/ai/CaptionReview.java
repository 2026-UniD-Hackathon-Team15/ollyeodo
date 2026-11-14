package com.team.ollyeodo.ai;

import java.util.List;

/**
 * 메뉴 설명 문구 분류 결과 (MVP §6.2). AI는 표현을 원문 그대로 인용하고 §6.2 규칙 코드로 분류만 한다.
 * 코드가 ① 인용이 실제 문구에 있는지 ② 규칙 코드가 표에 있는지 ③ 2회 분류가 같은지 확인한 뒤 판정한다.
 */
public record CaptionReview(List<Item> findings) {

    /**
     * @param expression 메뉴 설명에서 그대로 복사한 표현
     * @param category   §6.2 규칙 코드 (CAP_FACT, CAP_ORIGIN, CAP_DISEASE, CAP_MEDICINE, CAP_HFOOD,
     *                   CAP_SUPERLATIVE, CAP_FAKE_REVIEW, AI_VIRTUAL_EXPERT)
     * @param claimed    CAP_FACT일 때 문구가 주장한 값 (예: "12겹")
     * @param observed   CAP_FACT일 때 실물 사진에서 확인된 값 (예: "8겹")
     */
    public record Item(String expression, String category, String claimed, String observed) {
    }
}
