package com.team.ollyeodo.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** #48 데모 품질: 개수·층 수를 읽는 규칙이 프롬프트에 들어 있는지 확인한다 (판정 기준은 바꾸지 않는다). */
class PromptTemplatesTest {

    @Test
    @DisplayName("개수는 구역별로 보이는 개수 + 가려진 추정 개수로 세고, '불확실하면 null'로 빠지지 않는다")
    void countsVisibleAndHidden() {
        assertThat(PromptTemplates.IMAGE)
                .contains("칸·구역별로", "visible_count", "hidden_estimate", "count = visible_count + hidden_estimate")
                .contains("그릇·상자 밖이나 사진 밖은 추정하지 않는다")
                .doesNotContain("불확실하면 null");
        assertThat(PromptTemplates.image(List.of("떡"))).doesNotContain("불확실하면 null");
    }

    @Test
    @DisplayName("층으로 쌓인 음식이 아니면 layers는 null (층 없는 음식의 IMG_LAYER 오판 방지)")
    void layersOnlyForLayeredFood() {
        assertThat(PromptTemplates.IMAGE)
                .contains("층으로 쌓인 음식이 아니면 layers는 null")
                .contains("크림·잼은 따로 세지 않고");
    }

    @Test
    @DisplayName("수북한 재료는 개수 대신 fill_ratio로, fill_ratio는 0.1 단위")
    void pilesUseFillRatio() {
        assertThat(PromptTemplates.IMAGE).contains("양은 fill_ratio로", "0.1 단위로 반올림");
    }

    @Test
    @DisplayName("본체 밖 곁들임은 garnish, AI 이미지는 목록 이름으로 맞춘다")
    void garnishAndNameMatching() {
        assertThat(PromptTemplates.IMAGE).contains("garnish에 넣는다");
        assertThat(PromptTemplates.image(List.of("떡", "닭강정")))
                .contains("목록의 이름을 쓴다", "count는 0이다");
    }

    @Test
    @DisplayName("§6.1.1 실물 사진만 적용 판단을 하고, AI 이미지는 null로 둔다")
    void applicabilityOnlyForRealPhoto() {
        assertThat(PromptTemplates.REAL_IMAGE).contains("[적용 판단]", "IMG_COUNT", "IMG_FILL", "IMG_LAYER");
        assertThat(PromptTemplates.image(List.of("떡"), "닭강정"))
                .contains("applicability는 null로 둔다", "same_food", "\"닭강정\"");
    }

    @Test
    @DisplayName("§6.2 문구 분류는 원문 인용·표의 코드만, 심각도는 출력하지 않는다")
    void captionReviewQuotesAndCodes() {
        assertThat(PromptTemplates.CAPTION_REVIEW)
                .contains("그대로 복사", "CAP_MEDICINE", "AI_VIRTUAL_EXPERT", "심각도");
    }

    @Test
    @DisplayName("count는 코드가 보이는 개수 + 가려진 추정 개수로 다시 맞춘다")
    void totalCountIsVisiblePlusHidden() {
        var item = new ExtractedAttrs.Item("닭강정", "왼쪽 칸 7개, 벽에 2개 가려짐", 7, 2, 5, "상자");
        assertThat(item.withTotalCount().count()).isEqualTo(9);
        assertThat(new ExtractedAttrs.Item("떡", "", 4, null, null, "").withTotalCount().count()).isEqualTo(4);
        assertThat(new ExtractedAttrs.Item("소스", null, "위").withTotalCount().count()).isNull();
    }
}
