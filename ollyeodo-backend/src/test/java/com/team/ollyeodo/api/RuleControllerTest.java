package com.team.ollyeodo.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.ollyeodo.domain.Severity;
import com.team.ollyeodo.repository.RuleRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.ai.model.chat=none", "APP_TIMEZONE=Asia/Seoul",
        "spring.datasource.url=jdbc:h2:mem:rule-api;MODE=PostgreSQL",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class RuleControllerTest {
    private static final List<String> CODES = List.of("IMG_EXTRA_ITEM", "IMG_COUNT", "IMG_FILL", "IMG_LAYER",
            "CAP_FACT", "CAP_ORIGIN", "CAP_DISEASE", "CAP_MEDICINE", "CAP_HFOOD", "CAP_SUPERLATIVE",
            "CAP_FAKE_REVIEW", "AI_VIRTUAL_EXPERT", "INFO_AI_LABEL");
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired RuleRepository repository;

    private JsonNode response(String asOf) throws Exception {
        var request = get("/api/rules");
        if (asOf != null) request.param("asOf", asOf);
        return mapper.readTree(mvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void returnsThirteenOrderedUniqueDtosWithDocumentedValues() throws Exception {
        var result = response("2026-11-26");
        assertThat(result.size()).isEqualTo(13);
        var report = java.nio.file.Path.of("build/reports/rules-api-response.json");
        java.nio.file.Files.createDirectories(report.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(report.toFile(), result);
        for (int i = 0; i < CODES.size(); i++) {
            var row = result.get(i);
            String code = CODES.get(i);
            assertThat(row.path("code").asText()).isEqualTo(code);
            assertThat(row.size()).isEqualTo(9);
            assertThat(row.has("keywords")).isFalse();
            assertThat(row.path("title").asText()).doesNotContain("위법", "불법");
            assertThat(row.path("severity").asText()).isEqualTo(
                    List.of("CAP_ORIGIN", "INFO_AI_LABEL").contains(code) ? "GREEN" : "RED");
            assertThat(row.path("preEffectiveSeverity").asText()).isEqualTo("GREEN");
            assertThat(row.path("effectiveTo").isNull()).isTrue();
            if (code.equals("INFO_AI_LABEL")) {
                assertThat(row.path("lawRef").isNull()).isTrue();
                assertThat(row.path("sourceUrl").isNull()).isTrue();
                assertThat(row.path("effectiveFrom").isNull()).isTrue();
                assertThat(row.path("upcoming").asBoolean()).isFalse();
            } else {
                assertThat(row.path("lawRef").asText()).isNotBlank();
                assertThat(row.path("sourceUrl").asText()).isNotBlank();
                String date = code.equals("AI_VIRTUAL_EXPERT") ? "2026-11-27"
                        : code.equals("CAP_ORIGIN") ? "2026-10-22" : "2025-09-19";
                assertThat(row.path("effectiveFrom").asText()).isEqualTo(date);
            }
        }
    }

    @Test
    void upcomingChangesOnEffectiveDateWithoutChangingStoredSeverity() throws Exception {
        assertThat(response("2026-11-26").get(11).path("upcoming").asBoolean()).isTrue();
        var effective = response("2026-11-27").get(11);
        assertThat(effective.path("upcoming").asBoolean()).isFalse();
        assertThat(effective.path("severity").asText()).isEqualTo("RED");
    }

    @Test
    void returnsDatabaseChangesRatherThanHardcodedValues() throws Exception {
        var rule = repository.findById("IMG_COUNT").orElseThrow();
        rule.setTitle("DB에서 수정한 제목");
        rule.setLawRef("DB에서 수정한 근거");
        rule.setSourceUrl("https://example.test/source");
        rule.setEffectiveFrom(LocalDate.of(2030, 1, 1));
        rule.setSeverity(Severity.GREEN);
        repository.saveAndFlush(rule);
        var row = response("2026-11-26").get(1);
        assertThat(row.path("title").asText()).isEqualTo(rule.getTitle());
        assertThat(row.path("lawRef").asText()).isEqualTo(rule.getLawRef());
        assertThat(row.path("sourceUrl").asText()).isEqualTo(rule.getSourceUrl());
        assertThat(row.path("effectiveFrom").asText()).isEqualTo("2030-01-01");
        assertThat(row.path("severity").asText()).isEqualTo("GREEN");
        assertThat(row.path("upcoming").asBoolean()).isTrue();
    }

    @Test
    void rejectsInvalidDatesAndUsesExistingCors() throws Exception {
        mvc.perform(get("/api/rules").param("asOf", "2026-02-30")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/rules").param("asOf", "not-a-date")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/rules").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
