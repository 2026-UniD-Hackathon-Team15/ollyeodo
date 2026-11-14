package com.team.ollyeodo.ai;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;

import static com.team.ollyeodo.ai.ImageExtractionResult.FillAgreement.*;
import static org.junit.jupiter.api.Assertions.*;

class ImageExtractionResultTest {
    private static ExtractedAttrs.Item item(String name, Integer count, String position) {
        return new ExtractedAttrs.Item(name, count, position);
    }
    private static ExtractedAttrs attrs(List<ExtractedAttrs.Item> items, Double fill, String plate, List<String> garnish) {
        return new ExtractedAttrs(items, 3, fill, plate, garnish, List.of(), List.of());
    }
    private static ExtractedAttrs attrs(List<ExtractedAttrs.Item> items, Double fill) {
        return attrs(items, fill, "접시", List.of());
    }

    @Test
    void comparesExtractedCountsIgnoringPositionPlateGarnishAndItemOrder() {
        var a = attrs(List.of(item("딸기", 3, "윗면"), item("휘핑크림", 4, null)), 0.5, "둥근 접시", List.of("설탕"));
        var b = attrs(List.of(item("휘핑크림", 4, "위"), item("딸기", 3, "상단")), 0.5, "원형 접시", List.of());
        var result = ImageExtractionResult.compare(a, b);
        assertTrue(result.agreement().items().values().stream().allMatch(ImageExtractionResult.ItemAgreement::matches));
        assertEquals(MATCH, result.agreement().fillRatio());
        assertEquals(a, result.first());
        assertEquals(b, result.second());
    }

    @Test
    void unknownCountAffectsOnlyThatIngredient() {
        var a = attrs(List.of(item("딸기", 3, "위"), item("휘핑크림", null, "위")), 0.4);
        var b = attrs(List.of(item("딸기", 3, "상단"), item("휘핑크림", 4, "위")), 0.4);
        var result = ImageExtractionResult.compare(a, b);
        assertTrue(result.agreement().items().get("딸기").matches());
        assertFalse(result.agreement().items().get("휘핑크림").matches());
        assertNull(result.agreement().items().get("휘핑크림").firstCount());
    }

    @Test
    void countDifferenceAndMissingOrDuplicateRowsAreNotConfirmed() {
        var a = attrs(List.of(item("딸기", 3, "위")), 0.5);
        var b = attrs(List.of(item("딸기", 4, "위"), item("휘핑크림", 0, "위")), 0.5);
        assertFalse(ImageExtractionResult.compare(a, b).agreement().items().get("딸기").matches());
        assertFalse(ImageExtractionResult.compare(a, b).agreement().items().get("휘핑크림").matches());
        var duplicate = attrs(List.of(item("딸기", 3, "위"), item("딸기", 3, "위")), 0.5);
        assertNull(ImageExtractionResult.compare(duplicate, a).agreement().items().get("딸기").firstCount());
        var absent = attrs(List.of(item("딸기", 0, null)), 0.5);
        assertTrue(ImageExtractionResult.compare(absent, absent).agreement().items().get("딸기").matches());
    }

    @Test
    void comparesUnionOfRawNamesAndKeepsOneSidedItemsAsMismatch() {
        var a = attrs(List.of(item("딸기", 3, "위"), item("블루베리", 2, "위")), 0.5);
        var b = attrs(List.of(item("딸기", 3, "상단"), item("딸기 조각", 2, "내부")), 0.5);
        var result = ImageExtractionResult.compare(a, b);
        assertEquals(3, result.agreement().items().size());
        assertTrue(result.agreement().items().get("딸기").matches());
        assertFalse(result.agreement().items().get("블루베리").matches());
        assertNull(result.agreement().items().get("블루베리").secondCount());
        assertFalse(result.agreement().items().get("딸기 조각").matches());
        assertNull(result.agreement().items().get("딸기 조각").firstCount());
    }

    @Test
    void unsetToleranceDoesNotInventThreshold() {
        assertTrue(ImageExtractionResult.FILL_RATIO_TOLERANCE.isEmpty());
        var a = attrs(List.of(), 0.4);
        assertEquals(TOLERANCE_UNSET, ImageExtractionResult.compare(a, attrs(List.of(), 0.42)).agreement().fillRatio());
        assertEquals(MATCH, ImageExtractionResult.compare(a, a).agreement().fillRatio());
    }

    @Test
    void testsAbsoluteToleranceBoundaryUsingSyntheticTestValueOnly() {
        var a = attrs(List.of(), 0.4);
        // 0.02는 계산 검증용 입력이며 운영 기준으로 적용하지 않는다.
        var tolerance = OptionalDouble.of(0.02);
        assertEquals(MATCH, ImageExtractionResult.compare(a, attrs(List.of(), 0.42), tolerance).agreement().fillRatio());
        assertEquals(MATCH, ImageExtractionResult.compare(attrs(List.of(), 0.42), a, tolerance).agreement().fillRatio());
        assertEquals(MISMATCH, ImageExtractionResult.compare(a, attrs(List.of(), 0.420001), tolerance).agreement().fillRatio());
        assertThrows(IllegalArgumentException.class, () -> ImageExtractionResult.compare(a, a, OptionalDouble.of(-1)));
        assertThrows(IllegalArgumentException.class, () -> ImageExtractionResult.compare(a, a, OptionalDouble.of(Double.NaN)));
    }

    @Test
    void invalidAndUnknownFillRemainUnknown() {
        var a = attrs(List.of(), 0.5);
        for (Double value : new Double[]{null, Double.NaN, Double.POSITIVE_INFINITY, -0.1, 1.1}) {
            assertEquals(UNKNOWN, ImageExtractionResult.compare(a, attrs(List.of(), value)).agreement().fillRatio());
        }
    }

    @Test
    void keepsPeopleAndVisibleTextInAgreement() {
        var a = attrs(List.of(), 0.5);
        var b = new ExtractedAttrs(List.of(), 4, 0.5, null, null,
                List.of(new ExtractedAttrs.Person("가운", "약사")), List.of("추천"));
        var agreement = ImageExtractionResult.compare(a, b).agreement();
        assertFalse(agreement.layers());
        assertFalse(agreement.people());
        assertFalse(agreement.visibleText());
        var unknown = new ExtractedAttrs(List.of(), null, 0.5, null, null, null, null);
        assertFalse(ImageExtractionResult.compare(unknown, unknown).agreement().people());
    }

    @Test
    void imagePromptUsesOnlyPhotoWithoutMenuHintsOrJudgmentRequest() {
        assertFalse(PromptTemplates.IMAGE.contains("메뉴"));
        assertFalse(PromptTemplates.IMAGE.contains("딸기"));
        assertTrue(PromptTemplates.IMAGE.contains("법적 판단을 출력하지 않는다"));
    }
}
