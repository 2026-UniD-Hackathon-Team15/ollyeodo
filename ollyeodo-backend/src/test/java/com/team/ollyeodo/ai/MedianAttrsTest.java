package com.team.ollyeodo.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** MVP §6.3-2: 3회 추출을 중앙값·과반으로 합친다 */
class MedianAttrsTest {

    private static ExtractedAttrs run(Double fill, Boolean countApplicable, Boolean sameFood, ExtractedAttrs.Item... items) {
        return new ExtractedAttrs(List.of(items), null, fill, null, List.of(), List.of(), List.of(), "닭강정",
                countApplicable == null ? null
                        : List.of(new ExtractedAttrs.Applicability("IMG_COUNT", countApplicable, "이유")),
                sameFood);
    }

    private static ExtractedAttrs.Item item(String name, Integer count) {
        return new ExtractedAttrs.Item(name, count, null);
    }

    @Test
    @DisplayName("개수·담긴 비율은 중앙값이라 한 번 튄 값이 빠진다")
    void medianDropsOutlier() {
        var m = MedianAttrs.of(List.of(
                run(0.7, null, null, item("떡", 11)),
                run(0.9, null, null, item("떡", 30)),
                run(0.8, null, null, item("떡", 13))), true);
        assertThat(m.items()).singleElement().extracting(ExtractedAttrs.Item::count).isEqualTo(13);
        assertThat(m.fillRatio()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("AI 사진은 과반 회차에 나온 재료만 남긴다 (한 번만 보인 재료는 버림)")
    void aiKeepsMajorityItemsOnly() {
        var m = MedianAttrs.of(List.of(
                run(null, null, null, item("떡", 5), item("깨", 3)),
                run(null, null, null, item("떡", 6)),
                run(null, null, null, item("떡", 5))), false);
        assertThat(m.items()).extracting(ExtractedAttrs.Item::name).containsExactly("떡");
    }

    @Test
    @DisplayName("실물 사진은 한 번이라도 나온 재료를 남긴다 (없는 회차는 0개)")
    void realKeepsAllItems() {
        var m = MedianAttrs.of(List.of(
                run(null, null, null, item("떡", 5), item("깨", 3)),
                run(null, null, null, item("떡", 6)),
                run(null, null, null, item("떡", 5))), true);
        assertThat(m.items()).extracting(ExtractedAttrs.Item::name).containsExactly("떡", "깨");
        assertThat(m.items().get(1).count()).isZero();
    }

    @Test
    @DisplayName("적용 판단·같은 음식은 과반으로 정한다")
    void majorityForApplicabilityAndSameFood() {
        var m = MedianAttrs.of(List.of(
                run(null, false, false, item("떡", 1)),
                run(null, true, false, item("떡", 1)),
                run(null, false, true, item("떡", 1))), true);
        assertThat(m.applicability()).singleElement()
                .extracting(ExtractedAttrs.Applicability::applicable).isEqualTo(false);
        assertThat(m.sameFood()).isFalse();

        var once = MedianAttrs.of(List.of(
                run(null, false, null, item("떡", 1)),
                run(null, true, null, item("떡", 1)),
                run(null, true, null, item("떡", 1))), true);
        assertThat(once.applicability()).singleElement()
                .extracting(ExtractedAttrs.Applicability::applicable).isEqualTo(true);
    }

    @Test
    @DisplayName("셀 수 없는 재료(null)가 과반이면 개수는 null")
    void uncountableStaysNull() {
        var m = MedianAttrs.of(List.of(
                run(null, null, null, item("소스", null)),
                run(null, null, null, item("소스", null)),
                run(null, null, null, item("소스", 2))), true);
        assertThat(m.items().getFirst().count()).isNull();
    }
}
