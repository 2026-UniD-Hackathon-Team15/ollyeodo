package com.team.ollyeodo.service;

import com.team.ollyeodo.ai.CaptionClaims;
import com.team.ollyeodo.ai.ExtractionService;
import com.team.ollyeodo.ai.ImageInput;
import com.team.ollyeodo.api.CheckResponse;
import com.team.ollyeodo.domain.Check;
import com.team.ollyeodo.domain.Finding;
import com.team.ollyeodo.domain.Severity;
import com.team.ollyeodo.repository.CheckRepository;
import com.team.ollyeodo.storage.ImageStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckService {
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final ObjectProvider<ExtractionService> extractionProvider;
    private final ObjectProvider<CheckDecisionPipeline> decisionProvider;
    private final ComparisonService comparison;
    private final ImageStorage storage;
    private final CheckRepository checks;
    private final TransactionTemplate transaction;

    public CheckService(ObjectProvider<ExtractionService> extractionProvider,
                        ObjectProvider<CheckDecisionPipeline> decisionProvider,
                        ComparisonService comparison, ImageStorage storage,
                        CheckRepository checks, PlatformTransactionManager manager) {
        this.extractionProvider = extractionProvider;
        this.decisionProvider = decisionProvider;
        this.comparison = comparison;
        this.storage = storage;
        this.checks = checks;
        this.transaction = new TransactionTemplate(manager);
    }

    /** userId: 로그인한 사용자 (토큰의 sub, #71) */
    public CheckResponse create(UUID userId, String menuName, MultipartFile realImage, MultipartFile aiImage,
                                String caption, LocalDate asOf) {
        java.util.Objects.requireNonNull(userId, "userId");
        var missing = new ArrayList<String>();
        if (menuName == null || menuName.isBlank()) missing.add("menuName");
        if (realImage == null || realImage.isEmpty()) missing.add("realImage");
        if (aiImage == null || aiImage.isEmpty()) missing.add("aiImage");
        if (caption == null || caption.isBlank()) missing.add("caption");
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "필수값 누락: " + String.join(", ", missing));
        }
        validateImage(realImage);
        validateImage(aiImage);
        ExtractionService extraction = extractionProvider.getIfAvailable();
        CheckDecisionPipeline decision = decisionProvider.getIfAvailable();
        if (extraction == null || decision == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "검수 서비스가 아직 준비되지 않았습니다.");
        }
        ImageInput realInput = imageInput(realImage);
        ImageInput aiInput = imageInput(aiImage);
        String realPath = storage.store("real", realImage);
        String aiPath = storage.store("ai", aiImage);
        // 문구 추출은 이미지 추출(실물 → AI 2단계)과 동시에 돌려 응답 시간을 줄인다.
        var captionFuture = CompletableFuture.supplyAsync(() -> extraction.extractCaption(caption));
        var images = extraction.extractImages(aiInput, realInput, caption);
        CaptionClaims claims;
        try {
            claims = captionFuture.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause; // ExtractionException → 422 유지
            throw e;
        }
        // §6.3-2: 사진마다 3회 추출한 중앙값끼리 한 번 비교한다 (run1 = run2)
        var median = comparison.compare(images.aiMedian(), images.realMedian(), null);
        LocalDate date = asOf == null ? LocalDate.now(ZoneId.of("Asia/Seoul")) : asOf;
        var result = decision.evaluate(new CheckDecisionPipeline.Input(date, median, median, claims,
                images.realMedian(), images.aiRuns().get(0), images.aiRuns().get(1), caption,
                null, images.review1(), images.review2()));
        validateDecision(result);

        // 외부 AI/Storage 대기 중 DB 트랜잭션을 점유하지 않는다.
        Check check = transaction.execute(status -> {
            Check entity = new Check();
            entity.setUserId(userId);
            entity.setRealImagePath(realPath);
            entity.setAiImagePath(aiPath);
            entity.setMenuName(menuName);
            entity.setCaption(caption);
            entity.setAsOf(date);
            entity.setAiAttrs(images.aiMedian());
            entity.setRealAttrs(images.realMedian());
            entity.setOverall(result.overall());
            entity.setCaptionFix(result.captionFix());
            entity.setRegenPrompt(result.regenPrompt());
            for (var item : result.findings()) {
                Finding finding = new Finding(item.ruleCode(), item.severity(), item.aiValue(),
                        item.realValue(), item.evidence(), item.lawRef());
                finding.setCheckOnce(item.checkOnce());
                entity.addFinding(finding);
            }
            return checks.saveAndFlush(entity);
        });
        return response(check);
    }

    /** 내 검수만 조회한다. 다른 사람 기록이면 있는지도 알리지 않고 404 (#71) */
    public CheckResponse get(UUID userId, long id) {
        return transaction.execute(status -> {
            Check check = checks.findByIdAndUserId(id, userId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "검수 결과를 찾을 수 없습니다."));
            return response(check);
        });
    }

    private CheckResponse response(Check check) {
        return CheckResponse.from(check, storage.signedUrl(check.getRealImagePath()),
                storage.signedUrl(check.getAiImagePath()));
    }

    private static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getContentType() == null
                || !IMAGE_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비어 있지 않은 JPG, PNG, WEBP 이미지를 올려 주세요.");
        }
    }

    private static ImageInput imageInput(MultipartFile file) {
        try {
            return new ImageInput(file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException("이미지를 읽지 못했습니다.", e);
        }
    }

    private static void validateDecision(CheckDecisionPipeline.Result result) {
        if (result == null || (result.overall() != Severity.GREEN && result.overall() != Severity.RED)
                || result.findings() == null || result.captionFix() == null) {
            throw new IllegalStateException("잘못된 검수 결과입니다.");
        }
        boolean red = false;
        for (var finding : result.findings()) {
            if (finding.severity() != Severity.GREEN && finding.severity() != Severity.RED) {
                throw new IllegalStateException("검수 항목은 GREEN 또는 RED여야 합니다.");
            }
            if (finding.severity() == Severity.RED) {
                red = true;
                if (finding.evidence() == null || finding.evidence().isBlank()
                        || finding.lawRef() == null || finding.lawRef().isBlank()) {
                    throw new IllegalStateException("RED 항목에는 사유와 근거 조항이 필요합니다.");
                }
            }
        }
        if (red != (result.overall() == Severity.RED)) {
            throw new IllegalStateException("종합 판정과 항목별 판정이 일치하지 않습니다.");
        }
    }
}
