package com.team.ollyeodo.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** #48 데모 품질: 개수·층 수를 읽는 규칙이 프롬프트에 들어 있는지 확인한다 (판정 기준은 바꾸지 않는다). */
class PromptTemplatesTest {

    @Test
    @DisplayName("개수는 보이는 것만 세고, '불확실하면 null'로 빠지지 않는다")
    void countsWhatIsVisible() {
        assertThat(PromptTemplates.IMAGE)
                .contains("보이는 것만 센다", "가려지거나 사진 밖이라 안 보이는 것은 세지 않는다")
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
        assertThat(PromptTemplates.IMAGE).contains("수북이 쌓여", "양은 fill_ratio로", "0.1 단위로 반올림");
    }

    @Test
    @DisplayName("본체 밖 곁들임은 garnish, AI 이미지는 목록 이름으로 맞춘다")
    void garnishAndNameMatching() {
        assertThat(PromptTemplates.IMAGE).contains("garnish에 넣는다");
        assertThat(PromptTemplates.image(List.of("떡", "닭강정")))
                .contains("목록의 이름을 쓴다", "count는 0이다");
    }
}
