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

    @Test
    void submitsAllFourCallsConcurrentlyAndKeepsImagesSeparate() {
        var calls = new AtomicInteger();
        var started = new CountDownLatch(4);
        try (var executor = Executors.newFixedThreadPool(4)) {
            var service = new ExtractionService(client(image -> {
                calls.incrementAndGet();
                started.countDown();
                try {
                    if (!started.await(2, TimeUnit.SECONDS)) throw new IllegalStateException("not parallel");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                return attrs(List.of(), (int) image.bytes()[0]);
            }), executor, Duration.ofSeconds(3));
            var result = service.extractImages(IMAGE, new ImageInput(new byte[]{2}, "image/jpeg"));
            assertEquals(4, calls.get());
            assertEquals(1, result.ai().first().layers());
            assertEquals(1, result.ai().second().layers());
            assertEquals(2, result.real().first().layers());
            assertEquals(2, result.real().second().layers());
            assertTrue(result.ai().agreement().layers());
        }
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
