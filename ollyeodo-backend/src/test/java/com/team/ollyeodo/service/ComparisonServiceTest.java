package com.team.ollyeodo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.ollyeodo.ai.ExtractedAttrs;
import com.team.ollyeodo.domain.Severity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** §6.1 판정 기준표 경계값 테스트. 발표에서 "판정은 코드가 한다"의 근거. */
class ComparisonServiceTest {

    private final ComparisonService service = new ComparisonService();

    // ---- 테스트용 도우미 ----
    private static ExtractedAttrs.Item item(String name, int count) {
        return new ExtractedAttrs.Item(name, count, null);
    }

    private static ExtractedAttrs attrs(Integer layers, Double fill, ExtractedAttrs.Item... items) {
        return new ExtractedAttrs(List.of(items), layers, fill, null, List.of(), List.of(), List.of());
    }

    private static FindingCandidate find(ComparisonResult result, String ruleCode) {
        return result.findings().stream()
                .filter(f -> f.ruleCode().equals(ruleCode))
                .findFirst()
                .orElseThrow(() -> new AssertionError(ruleCode + " 판정이 없음"));
    }

    // ---- IMG_COUNT 경계값: 1.19 🟢 / 1.2 🟡 / 1.49 🟡 / 1.5 🔴 ----
    @ParameterizedTest(name = "실물 100개 vs AI {0}개 → {1}")
    @CsvSource({"119, GREEN", "120, YELLOW", "149, YELLOW", "150, RED"})
    @DisplayName("개수 비율 경계값")
    void countBoundary(int aiCount, Severity expected) {
        ComparisonResult result = service.compare(
                attrs(8, null, item("딸기", aiCount)),
                attrs(8, null, item("딸기", 100)),
                null);

        assertThat(find(result, "IMG_COUNT").severity()).isEqualTo(expected);
    }

    // ---- IMG_FILL 경계값 ----
    @ParameterizedTest(name = "실물 0.5 vs AI {0} → {1}")
    @CsvSource({"0.595, GREEN", "0.6, YELLOW", "0.745, YELLOW", "0.75, RED"})
    @DisplayName("양(fill_ratio) 비율 경계값")
    void fillBoundary(double aiFill, Severity expected) {
        ComparisonResult result = service.compare(
                attrs(8, aiFill, item("딸기", 4)),
                attrs(8, 0.5, item("딸기", 4)),
                null);

        assertThat(find(result, "IMG_FILL").severity()).isEqualTo(expected);
    }

    @Test
    @DisplayName("없는 재료 1개 → 🔴")
    void extraItemIsRed() {
        ComparisonResult result = service.compare(
                attrs(8, null, item("딸기", 4), item("블루베리", 3)),
                attrs(8, null, item("딸기", 4)),
                null);

        FindingCandidate extra = find(result, "IMG_EXTRA_ITEM");
        assertThat(extra.severity()).isEqualTo(Severity.RED);
        assertThat(extra.aiValue()).isEqualTo("블루베리");
    }

    @Test
    @DisplayName("층 수 다름 → 🔴, 같음 → 🟢")
    void layerDiffIsRed() {
        ComparisonResult diff = service.compare(attrs(12, null), attrs(8, null), null);
        ComparisonResult same = service.compare(attrs(8, null), attrs(8, null), null);

        assertThat(find(diff, "IMG_LAYER").severity()).isEqualTo(Severity.RED);
        assertThat(find(same, "IMG_LAYER").severity()).isEqualTo(Severity.GREEN);
    }

    @Test
    @DisplayName("데모: 딸기 9 vs 4 → 실물의 2.25배 🔴, 블루베리 🔴, 12겹 vs 8겹 🔴")
    void demoScenario() {
        ComparisonResult result = service.compare(
                attrs(12, null, item("딸기", 9), item("블루베리", 5)),
                attrs(8, null, item("딸기", 4)),
                null);

        FindingCandidate count = find(result, "IMG_COUNT");
        assertThat(count.severity()).isEqualTo(Severity.RED);
        assertThat(count.evidence()).isEqualTo("실물의 2.25배");
        assertThat(find(result, "IMG_EXTRA_ITEM").severity()).isEqualTo(Severity.RED);
        assertThat(find(result, "IMG_LAYER").severity()).isEqualTo(Severity.RED);
    }

    @Test
    @DisplayName("실물 사진 없음 → 메뉴 정보로 비교 + 안내 문구")
    void noRealPhotoUsesFacts() {
        ComparisonResult result = service.compare(
                attrs(12, null, item("딸기", 9)),
                null,
                attrs(8, null, item("딸기", 4)));

        assertThat(result.notices()).contains(ComparisonService.NO_REAL_PHOTO_NOTICE);
        assertThat(find(result, "IMG_COUNT").evidence()).isEqualTo("메뉴 정보의 2.25배");
        assertThat(find(result, "IMG_LAYER").severity()).isEqualTo(Severity.RED);
    }
}