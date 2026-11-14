package com.team.ollyeodo.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.ollyeodo.ai.*;
import com.team.ollyeodo.domain.RegenPrompt;
import com.team.ollyeodo.domain.Severity;
import com.team.ollyeodo.repository.CheckRepository;
import com.team.ollyeodo.service.CheckDecisionPipeline;
import com.team.ollyeodo.storage.ImageStorage;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP 계약과 H2 실제 영속화 검증. AI/Storage 외부 요청은 실행하지 않는다. */
@SpringBootTest(properties = {"spring.ai.model.chat=none",
        "spring.datasource.url=jdbc:h2:mem:check-api-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class CheckApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CheckRepository checks;
    @MockitoBean ExtractionService extraction;
    @MockitoBean ImageStorage storage;
    @MockitoBean CheckDecisionPipeline decision;

    private ExtractedAttrs real;
    private ExtractedAttrs ai;

    @BeforeEach
    void setUp() {
        checks.deleteAll();
        real = attrs(4);
        ai = attrs(9);
        when(storage.store(eq("real"), any())).thenReturn("real/photo.png");
        when(storage.store(eq("ai"), any())).thenReturn("ai/photo.png");
        when(storage.signedUrl(anyString())).thenAnswer(call -> "https://example.test/" + call.getArgument(0));
        when(extraction.extractImages(any(), any())).thenReturn(new ExtractionService.ImagesResult(
                ImageExtractionResult.compare(ai, ai), ImageExtractionResult.compare(real, real), List.of("딸기")));
        when(extraction.extractCaption(anyString())).thenReturn(new CaptionClaims(List.of(), null, null, List.of()));
        when(decision.evaluate(any())).thenReturn(new CheckDecisionPipeline.Result(Severity.RED, List.of(
                new CheckResponse.FindingResponse("IMG_COUNT", Severity.RED, "딸기 9개", "딸기 4개",
                        "실물의 2.25배", "테스트 근거", false),
                new CheckResponse.FindingResponse("CAP_ORIGIN", Severity.GREEN, null, null,
                        "원산지를 직접 확인해 주세요", null, true)),
                "수정 문구", new RegenPrompt("한글 프롬프트", "English prompt")));
    }

    @Test
    void createsAndRetrievesCheckWithFindingsAndFreshImageUrls() throws Exception {
        String body = mvc.perform(checkRequest().file(image("realImage"))
                        .file(image("aiImage")).param("caption", "딸기 케이크").param("asOf", "2026-11-26"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.overall").value("RED"))
                .andExpect(jsonPath("$.menuName").value("딸기 케이크"))
                .andExpect(jsonPath("$.caption").value("딸기 케이크"))
                .andExpect(jsonPath("$.findings[1].checkOnce").value(true))
                .andExpect(jsonPath("$.asOf").value("2026-11-26"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).get("checkId").asLong();
        assertThat(checks.count()).isEqualTo(1);
        when(storage.signedUrl("real/photo.png")).thenReturn("https://example.test/new-real-signature");
        mvc.perform(get("/api/checks/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.menuName").value("딸기 케이크"))
                .andExpect(jsonPath("$.caption").value("딸기 케이크"))
                .andExpect(jsonPath("$.realImageUrl").value("https://example.test/new-real-signature"))
                .andExpect(jsonPath("$.aiImageUrl").value("https://example.test/ai/photo.png"))
                .andExpect(jsonPath("$.regenPrompt.ko").value("한글 프롬프트"))
                .andExpect(jsonPath("$.findings[0].evidence").value("실물의 2.25배"))
                .andExpect(jsonPath("$.findings[1].severity").value("GREEN"))
                .andExpect(jsonPath("$.findings[1].checkOnce").value(true));
        verify(extraction).extractCaption("딸기 케이크");
        var input = ArgumentCaptor.forClass(CheckDecisionPipeline.Input.class);
        verify(decision).evaluate(input.capture());
        assertThat(input.getValue().run1().findings()).anySatisfy(f -> {
            assertThat(f.ruleCode()).isEqualTo("IMG_COUNT");
            assertThat(f.evidence()).isEqualTo("실물의 2.25배");
        });
        assertThat(input.getValue().run2()).isEqualTo(input.getValue().run1());
    }

    @Test
    void defaultsToSeoulDateAndAllowsNullRegenPromptOnGreen() throws Exception {
        LocalDate before = LocalDate.now(ZoneId.of("Asia/Seoul"));
        when(decision.evaluate(any())).thenReturn(new CheckDecisionPipeline.Result(
                Severity.GREEN, List.of(), "원문", null));
        String body = mvc.perform(checkRequest().file(image("realImage"))
                        .file(image("aiImage")).param("caption", "원문"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.overall").value("GREEN"))
                .andExpect(jsonPath("$.findings").isEmpty())
                .andExpect(jsonPath("$.captionFix").value("원문"))
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(body).get("regenPrompt").isNull()).isTrue();
        assertThat(LocalDate.parse(json.readTree(body).get("asOf").asText()))
                .isIn(before, LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    @Test
    void rejectsInvalidSecondImageBeforeAnyUpload() throws Exception {
        mvc.perform(checkRequest().file(image("realImage"))
                        .file(new MockMultipartFile("aiImage", "bad.txt", "text/plain", new byte[]{1}))
                        .param("caption", "설명"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(storage, extraction, decision);
        assertThat(checks.count()).isZero();
    }

    @Test
    void rejectsMissingAndEmptyImages() throws Exception {
        mvc.perform(checkRequest().file(image("realImage")).param("caption", "설명"))
                .andExpect(status().isBadRequest());
        mvc.perform(checkRequest().file(image("realImage"))
                        .file(new MockMultipartFile("aiImage", "empty.png", "image/png", new byte[0]))
                        .param("caption", "설명"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(storage, extraction, decision);
    }

    @Test
    void rejectsInvalidDateAndBlankOrMissingCaption() throws Exception {
        mvc.perform(checkRequest().file(image("realImage")).file(image("aiImage"))
                        .param("caption", "설명").param("asOf", "not-a-date"))
                .andExpect(status().isBadRequest());
        mvc.perform(checkRequest().file(image("realImage")).file(image("aiImage"))
                        .param("caption", "   "))
                .andExpect(status().isBadRequest());
        mvc.perform(checkRequest().file(image("realImage")).file(image("aiImage")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(storage, extraction, decision);
    }

    @Test
    void extractionFailureReturns422WithoutSavingCheck() throws Exception {
        when(extraction.extractImages(any(), any())).thenThrow(new ExtractionException("upstream detail", null));
        mvc.perform(checkRequest().file(image("realImage")).file(image("aiImage"))
                        .param("caption", "설명"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("이미지 분석에 실패했습니다. 실물 사진을 다시 찍어 주세요."));
        assertThat(checks.count()).isZero();
        verifyNoInteractions(decision);
    }

    @Test
    void captionFailureDoesNotSavePartialResult() throws Exception {
        when(extraction.extractCaption(anyString())).thenThrow(new ExtractionException("failed", null));
        mvc.perform(checkRequest().file(image("realImage")).file(image("aiImage"))
                        .param("caption", "설명"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(checks.count()).isZero();
        verifyNoInteractions(decision);
    }

    @Test
    void missingCheckReturns404() throws Exception {
        mvc.perform(get("/api/checks/999999")).andExpect(status().isNotFound());
        verifyNoInteractions(extraction, decision);
    }

    @Test
    void reportsAllMissingRequiredFields() throws Exception {
        mvc.perform(multipart("/api/checks"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("필수값 누락: menuName, realImage, aiImage, caption"));
        verifyNoInteractions(storage, extraction, decision);
    }

    @Test
    void reportsBlankTextAndEmptyFileNames() throws Exception {
        mvc.perform(multipart("/api/checks").file(new MockMultipartFile("realImage", "empty.png", "image/png", new byte[0]))
                        .file(image("aiImage")).param("menuName", " ").param("caption", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("필수값 누락: menuName, realImage, caption"));
        verifyNoInteractions(storage, extraction, decision);
    }

    private static MockMultipartFile image(String name) {
        return new MockMultipartFile(name, "cake.png", "image/png", new byte[]{1, 2, 3});
    }

    private static ExtractedAttrs attrs(int count) {
        return new ExtractedAttrs(List.of(new ExtractedAttrs.Item("딸기", count, "위")),
                8, 0.5, "접시", List.of(), List.of(), List.of());
    }
    private static org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder checkRequest() {
        var request = multipart("/api/checks");
        request.param("menuName", "딸기 케이크");
        return request;
    }

}
