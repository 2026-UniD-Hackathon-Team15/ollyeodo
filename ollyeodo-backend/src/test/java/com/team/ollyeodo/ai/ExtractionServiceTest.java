package com.team.ollyeodo.ai;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ExtractionServiceTest {
    private static final ImageInput IMAGE = new ImageInput(new byte[]{1}, "image/png");
    private static final ExtractedAttrs ATTRS = attrs(List.of("a", "b"), 4);

    private static ExtractedAttrs attrs(List<String> garnish, Integer layers) {
        return new ExtractedAttrs(List.of(new ExtractedAttrs.Item("딸기", 4, "윗면")), layers,
                0.5, "접시", garnish, List.of(), List.of());
    }

    private static final ImageInput REAL = new ImageInput(new byte[]{2}, "image/jpeg");

    private static ExtractedAttrs items(Integer layers, String... names) {
        return new ExtractedAttrs(java.util.Arrays.stream(names)
                .map(n -> new ExtractedAttrs.Item(n, 3, null)).toList(), layers, 0.5, "상자", List.of(), List.of(), List.of());
    }

    @Test
    void planA_realPhotoFirstThenAiWithRealIngredientNames() {
        var order = new java.util.concurrent.CopyOnWriteArrayList<String>();
        var aiPrompts = new java.util.concurrent.CopyOnWriteArrayList<String>();
        var realStarted = new CountDownLatch(2);
        try (var executor = Executors.newFixedThreadPool(4)) {
            var client = new ExtractionClient() {
                public ExtractedAttrs extractImage(String prompt, ImageInput image, Duration timeout) {
                    if (image == REAL) {
                        order.add("real");
                        assertEquals(PromptTemplates.IMAGE, prompt); // 실물은 목록 없이 자유 추출
                        realStarted.countDown();
                        try {
                            if (!realStarted.await(2, TimeUnit.SECONDS)) throw new IllegalStateException("real not parallel");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        // 회차마다 나온 재료가 조금 달라도 합집합으로 목록을 만든다
                        return order.size() == 1 ? items(2, "닭강정", "떡") : items(2, "닭강정", " 떡 ", "파");
                    }
                    order.add("ai");
                    aiPrompts.add(prompt);
                    return items(1, "닭강정", "떡");
                }
                public CaptionClaims extractCaption(String prompt, String caption, Duration timeout) {
                    throw new UnsupportedOperationException();
                }
            };
            var result = new ExtractionService(client, executor, Duration.ofSeconds(3)).extractImages(IMAGE, REAL);

            assertEquals(List.of("real", "real", "ai", "ai"), order); // 실물 2회가 끝난 뒤 AI 2회
            assertEquals(List.of("닭강정", "떡", "파"), result.ingredientNames());
            assertEquals(2, aiPrompts.size());
            aiPrompts.forEach(p -> assertTrue(p.contains("[\"닭강정\",\"떡\",\"파\"]"), "AI 프롬프트에 실물 재료명 목록"));
            assertEquals(2, result.real().first().layers());
            assertEquals(1, result.ai().first().layers());
        }
    }

    @Test
    void failsWithRetakeMessageWhenRealPhotoHasNoIngredients() {
        var aiCalls = new AtomicInteger();
        var service = new ExtractionService(new ExtractionClient() {
            public ExtractedAttrs extractImage(String prompt, ImageInput image, Duration timeout) {
                if (image == REAL) return new ExtractedAttrs(List.of(), null, null, null, List.of(), List.of(), List.of());
                aiCalls.incrementAndGet();
                return ATTRS;
            }
            public CaptionClaims extractCaption(String prompt, String caption, Duration timeout) {
                throw new UnsupportedOperationException();
            }
        }, Runnable::run, Duration.ofSeconds(1));
        var e = assertThrows(ExtractionException.class, () -> service.extractImages(IMAGE, REAL));
        assertEquals(ExtractionService.NO_REAL_INGREDIENTS, e.getMessage());
        assertEquals(0, aiCalls.get(), "실물에서 재료가 없으면 AI 이미지는 호출하지 않는다");
    }

    @Test
    void aiPromptRejectsEmptyOrDuplicateNames() {
        assertThrows(IllegalArgumentException.class, () -> PromptTemplates.image(List.of()));
        assertThrows(IllegalArgumentException.class, () -> PromptTemplates.image(List.of("떡", "떡")));
        assertThrows(IllegalArgumentException.class, () -> PromptTemplates.image(List.of(" ")));
    }

    @Test
    void wrapsProviderFailureAndNullResponse() {
        var failure = new IllegalStateException("provider failed");
        var service = new ExtractionService(client(i -> { throw failure; }), Runnable::run, Duration.ofSeconds(1));
        assertSame(failure, assertThrows(ExtractionException.class, () -> service.extractImages(IMAGE, IMAGE)).getCause());
        var empty = new ExtractionService(client(i -> null), Runnable::run, Duration.ofSeconds(1));
        assertInstanceOf(NullPointerException.class,
                assertThrows(ExtractionException.class, () -> empty.extractImage(IMAGE)).getCause());
    }

    @Test
    void timesOutWithoutWaitingForBlockedProvider() {
        var release = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(4);
        try {
            var service = new ExtractionService(client(i -> {
                try { release.await(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return ATTRS;
            }), executor, Duration.ofMillis(50));
            assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                    assertInstanceOf(TimeoutException.class,
                            assertThrows(ExtractionException.class, () -> service.extractImages(IMAGE, IMAGE)).getCause()));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void extractsCaptionWithSeparatePrompt() {
        var expected = new CaptionClaims(List.of(), 8, null, List.of("약사 추천"));
        var client = new ExtractionClient() {
            public ExtractedAttrs extractImage(String prompt, ImageInput image, Duration timeout) {
                assertEquals(PromptTemplates.IMAGE, prompt);
                return ATTRS;
            }
            public CaptionClaims extractCaption(String prompt, String caption, Duration timeout) {
                assertEquals(PromptTemplates.CAPTION, prompt);
                assertEquals("8겹 약사 추천", caption);
                return expected;
            }
        };
        var service = new ExtractionService(client, Runnable::run, Duration.ofSeconds(1));
        assertEquals(ATTRS, service.extractImage(IMAGE));
        assertEquals(expected, service.extractCaption("8겹 약사 추천"));
    }

    private ExtractionClient client(java.util.function.Function<ImageInput, ExtractedAttrs> action) {
        return new ExtractionClient() {
            public ExtractedAttrs extractImage(String prompt, ImageInput image, Duration timeout) {
                return action.apply(image);
            }
            public CaptionClaims extractCaption(String prompt, String caption, Duration timeout) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
