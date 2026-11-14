package com.team.ollyeodo.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
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

    public record ImagesResult(ImageExtractionResult ai, ImageExtractionResult real) {}

    /** join 전에 네 작업을 모두 제출한다. 한 번이라도 실패하면 부분 결과를 반환하지 않는다. */
    public ImagesResult extractImages(ImageInput ai, ImageInput real) {
        Objects.requireNonNull(ai);
        Objects.requireNonNull(real);
        String prompt = PromptTemplates.IMAGE;
        var aiFirst = image(ai, prompt, "image-ai-1");
        var aiSecond = image(ai, prompt, "image-ai-2");
        var realFirst = image(real, prompt, "image-real-1");
        var realSecond = image(real, prompt, "image-real-2");
        await(CompletableFuture.allOf(aiFirst, aiSecond, realFirst, realSecond));
        return new ImagesResult(ImageExtractionResult.compare(aiFirst.join(), aiSecond.join()),
                ImageExtractionResult.compare(realFirst.join(), realSecond.join()));
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
