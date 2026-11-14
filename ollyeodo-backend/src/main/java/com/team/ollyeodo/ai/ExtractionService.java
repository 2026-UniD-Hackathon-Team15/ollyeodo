package com.team.ollyeodo.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** 제공자 결정 후 ExtractionClient와 extractionExecutor 빈으로 구성한다. */
public class ExtractionService {
    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);
    private final ExtractionClient client;
    private final Executor executor;
    private final Duration timeout;

    public ExtractionService(ExtractionClient client, Executor executor, Duration timeout) {
        this.client = Objects.requireNonNull(client);
        this.executor = Objects.requireNonNull(executor);
        if (timeout == null || timeout.toMillis() <= 0) {
            throw new IllegalArgumentException("timeout은 1ms 이상이어야 합니다.");
        }
        this.timeout = timeout;
    }

    public ExtractedAttrs extractImage(ImageInput image) {
        Objects.requireNonNull(image);
        return await(image(image, PromptTemplates.IMAGE, "image-single"));
    }

    public CaptionClaims extractCaption(String caption) {
        Objects.requireNonNull(caption);
        return await(submit("caption", () -> client.extractCaption(PromptTemplates.CAPTION, caption, timeout)));
    }

    /** ingredientNames: 실물 사진에서 만든 재료명 목록 (AI 이미지 추출에 넘긴 값) */
    public record ImagesResult(ImageExtractionResult ai, ImageExtractionResult real, List<String> ingredientNames) {}

    /**
     * A안(실물 먼저, MVP §7.1):
     * 1) 실물 사진을 2회 병렬로 자유 추출한다.
     * 2) 두 결과의 재료명을 합쳐 목록을 만든다.
     * 3) 그 목록을 넘겨 AI 이미지를 2회 병렬 추출한다 (같은 이름으로 세게 해서 "없는 재료" 오판을 막는다).
     * 각 단계는 한 번이라도 실패하면 부분 결과를 반환하지 않는다.
     */
    public ImagesResult extractImages(ImageInput ai, ImageInput real) {
        Objects.requireNonNull(ai);
        Objects.requireNonNull(real);
        var realFirst = image(real, PromptTemplates.IMAGE, "image-real-1");
        var realSecond = image(real, PromptTemplates.IMAGE, "image-real-2");
        await(CompletableFuture.allOf(realFirst, realSecond));
        ExtractedAttrs r1 = realFirst.join();
        ExtractedAttrs r2 = realSecond.join();

        List<String> names = ingredientNames(r1, r2);
        if (names.isEmpty()) {
            throw new ExtractionException(NO_REAL_INGREDIENTS, null);
        }

        String aiPrompt = PromptTemplates.image(names);
        var aiFirst = image(ai, aiPrompt, "image-ai-1");
        var aiSecond = image(ai, aiPrompt, "image-ai-2");
        await(CompletableFuture.allOf(aiFirst, aiSecond));
        return new ImagesResult(ImageExtractionResult.compare(aiFirst.join(), aiSecond.join()),
                ImageExtractionResult.compare(r1, r2), names);
    }

    public static final String NO_REAL_INGREDIENTS = "실물 사진에서 재료를 찾지 못했습니다. 실물 사진을 다시 찍어 주세요.";

    /** 두 추출 결과의 재료명 합집합 (공백 정리, 빈 이름·중복 제외, 처음 나온 순서 유지) */
    static List<String> ingredientNames(ExtractedAttrs first, ExtractedAttrs second) {
        Set<String> names = new LinkedHashSet<>();
        for (ExtractedAttrs attrs : List.of(first, second)) {
            if (attrs == null || attrs.items() == null) continue;
            attrs.items().stream().filter(Objects::nonNull).map(ExtractedAttrs.Item::name)
                    .filter(Objects::nonNull).map(String::strip).filter(n -> !n.isEmpty())
                    .forEach(names::add);
        }
        return List.copyOf(names);
    }

    private CompletableFuture<ExtractedAttrs> image(ImageInput image, String prompt, String operation) {
        return submit(operation, () -> client.extractImage(prompt, image, timeout));
    }

    private <T> CompletableFuture<T> submit(String operation, Supplier<T> action) {
        long start = System.nanoTime();
        CompletableFuture<T> future;
        try {
            future = CompletableFuture.supplyAsync(() -> Objects.requireNonNull(action.get(),
                    "추출 응답이 없습니다."), executor).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RuntimeException e) {
            future = CompletableFuture.failedFuture(e);
        }
        return future.whenComplete((result, error) -> log.info(
                "extraction operation={} elapsedMs={} success={}", operation,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), error == null));
    }

    private <T> T await(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            throw new ExtractionException("AI 값 추출에 실패했거나 제한시간을 초과했습니다.", e.getCause());
        }
    }
}
