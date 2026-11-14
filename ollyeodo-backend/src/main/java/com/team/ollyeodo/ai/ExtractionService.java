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

    /** 사진 한 장을 몇 번 추출해 중앙값을 낼지 (MVP §6.3-2) */
    public static final int RUNS = 3;

    /**
     * ai·real: 1·2회차 비교 (자기일관성 참고값)
     * ingredientNames: 실물 사진에서 만든 재료명 목록 (AI 이미지 추출에 넘긴 값)
     * review1·review2: 메뉴 설명 문구 분류 2회 (MVP §6.2). 실패했거나 요청하지 않았으면 null → 키워드 간이 검수
     * aiMedian·realMedian: 3회 추출의 중앙값·과반 (판정에 쓰는 값)
     * aiRuns·realRuns: 회차별 원본 추출값
     */
    public record ImagesResult(ImageExtractionResult ai, ImageExtractionResult real, List<String> ingredientNames,
                               CaptionReview review1, CaptionReview review2,
                               ExtractedAttrs aiMedian, ExtractedAttrs realMedian,
                               List<ExtractedAttrs> aiRuns, List<ExtractedAttrs> realRuns) {
        public ImagesResult(ImageExtractionResult ai, ImageExtractionResult real, List<String> ingredientNames) {
            this(ai, real, ingredientNames, null, null, ai.first(), real.first(),
                    List.of(ai.first(), ai.second()), List.of(real.first(), real.second()));
        }
    }

    public ImagesResult extractImages(ImageInput ai, ImageInput real) {
        return extractImages(ai, real, null);
    }

    /**
     * A안(실물 먼저, MVP §7.1):
     * 1) 실물 사진을 3회 병렬로 자유 추출하고 중앙값·과반으로 합친다 (MVP §6.3-2).
     * 2) 세 결과의 재료명을 합쳐 목록을 만든다.
     * 3) 그 목록을 넘겨 AI 이미지를 3회 병렬 추출해 중앙값·과반으로 합친다
     *    (같은 이름으로 세게 해서 "없는 재료" 오판을 막는다).
     *    caption이 있으면 같은 때 실물 중앙값을 넘겨 문구 분류도 2회 돌린다 (MVP §6.2).
     * 이미지 단계는 한 번이라도 실패하면 부분 결과를 반환하지 않는다. 문구 분류 실패는 null로 둔다.
     */
    public ImagesResult extractImages(ImageInput ai, ImageInput real, String caption) {
        Objects.requireNonNull(ai);
        Objects.requireNonNull(real);
        List<ExtractedAttrs> realRuns = runAll(real, PromptTemplates.REAL_IMAGE, "image-real");
        List<String> names = ingredientNames(realRuns);
        if (names.isEmpty()) {
            throw new ExtractionException(NO_REAL_INGREDIENTS, null);
        }
        ExtractedAttrs realMedian = MedianAttrs.of(realRuns, true);

        String aiPrompt = PromptTemplates.image(names, realMedian.foodType());
        List<CompletableFuture<ExtractedAttrs>> aiFutures = submitAll(ai, aiPrompt, "image-ai");
        CompletableFuture<CaptionReview> review1 = review(caption, realMedian, "caption-review-1");
        CompletableFuture<CaptionReview> review2 = review(caption, realMedian, "caption-review-2");
        List<ExtractedAttrs> aiRuns = joinAll(aiFutures);
        if (differentFood(aiRuns, names)) {
            throw new DifferentFoodException();
        }
        ExtractedAttrs aiMedian = MedianAttrs.of(aiRuns, false);
        return new ImagesResult(ImageExtractionResult.compare(aiRuns.get(0), aiRuns.get(1)),
                ImageExtractionResult.compare(realRuns.get(0), realRuns.get(1)), names,
                orNull(review1), orNull(review2), aiMedian, realMedian, aiRuns, realRuns);
    }

    private List<CompletableFuture<ExtractedAttrs>> submitAll(ImageInput input, String prompt, String operation) {
        List<CompletableFuture<ExtractedAttrs>> futures = new java.util.ArrayList<>();
        for (int i = 1; i <= RUNS; i++) futures.add(image(input, prompt, operation + "-" + i));
        return futures;
    }

    private List<ExtractedAttrs> joinAll(List<CompletableFuture<ExtractedAttrs>> futures) {
        await(CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)));
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private List<ExtractedAttrs> runAll(ImageInput input, String prompt, String operation) {
        return joinAll(submitAll(input, prompt, operation));
    }

    private CompletableFuture<CaptionReview> review(String caption, ExtractedAttrs observed, String operation) {
        if (caption == null || caption.isBlank()) return CompletableFuture.completedFuture(null);
        String input;
        try {
            var data = new java.util.LinkedHashMap<String, Object>();
            data.put("caption", caption);
            data.put("observed", observed);
            input = JSON.writeValueAsString(data);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return CompletableFuture.completedFuture(null);
        }
        return submit(operation, () -> client.reviewCaption(PromptTemplates.CAPTION_REVIEW, input, timeout))
                .exceptionally(e -> {
                    log.warn("caption review failed, falling back to keywords: {}", e.toString());
                    return null;
                });
    }

    private static CaptionReview orNull(CompletableFuture<CaptionReview> future) {
        try {
            return future.join();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * §6.3-9 다른 음식이면 검수 불가. AI 이미지 3회 중 과반(2회 이상)이
     * ① same_food=false 이거나 ② 실물 재료가 전부 0개로 나오면 다른 음식으로 본다.
     */
    static boolean differentFood(List<ExtractedAttrs> runs, List<String> realNames) {
        long different = runs.stream().filter(r -> looksDifferent(r, realNames)).count();
        return different >= runs.size() / 2 + 1;
    }

    static boolean differentFood(ExtractedAttrs first, ExtractedAttrs second, List<String> realNames) {
        return looksDifferent(first, realNames) && looksDifferent(second, realNames);
    }

    private static boolean looksDifferent(ExtractedAttrs ai, List<String> realNames) {
        if (ai == null) return false;
        if (Boolean.FALSE.equals(ai.sameFood())) return true;
        if (ai.items() == null || realNames.isEmpty()) return false;
        for (String name : realNames) {
            boolean zero = ai.items().stream().anyMatch(i -> i != null && i.name() != null
                    && i.name().strip().equals(name) && Integer.valueOf(0).equals(i.count()));
            if (!zero) return false;
        }
        return true;
    }

    public static final String NO_REAL_INGREDIENTS = "실물 사진에서 재료를 찾지 못했습니다. 실물 사진을 다시 찍어 주세요.";

    /** 두 추출 결과의 재료명 합집합 (공백 정리, 빈 이름·중복 제외, 처음 나온 순서 유지) */
    static List<String> ingredientNames(ExtractedAttrs first, ExtractedAttrs second) {
        return ingredientNames(java.util.Arrays.asList(first, second));
    }

    static List<String> ingredientNames(List<ExtractedAttrs> runs) {
        Set<String> names = new LinkedHashSet<>();
        for (ExtractedAttrs attrs : runs) {
            if (attrs == null || attrs.items() == null) continue;
            attrs.items().stream().filter(Objects::nonNull).map(ExtractedAttrs.Item::name)
                    .filter(Objects::nonNull).map(String::strip).filter(n -> !n.isEmpty())
                    .forEach(names::add);
        }
        return List.copyOf(names);
    }

    private CompletableFuture<ExtractedAttrs> image(ImageInput image, String prompt, String operation) {
        return submit(operation, () -> client.extractImage(prompt, image, timeout))
                .thenApply(ExtractedAttrs::withTotalCounts);
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
