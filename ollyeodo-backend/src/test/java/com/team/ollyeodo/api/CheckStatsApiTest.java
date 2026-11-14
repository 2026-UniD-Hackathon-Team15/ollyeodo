package com.team.ollyeodo.api;

import com.team.ollyeodo.domain.Check;
import com.team.ollyeodo.domain.Finding;
import com.team.ollyeodo.domain.Severity;
import com.team.ollyeodo.repository.CheckRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** #20 검수 기록 통계: 규칙별 🔴 검수 건수와 최근 검수 목록 (H2 실제 집계) */
@SpringBootTest(properties = {"spring.ai.model.chat=none",
        "spring.datasource.url=jdbc:h2:mem:check-stats-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class CheckStatsApiTest {
    @Autowired MockMvc mvc;
    @Autowired CheckRepository checks;

    private Check check(String menu, Severity overall, Finding... findings) {
        Check c = new Check();
        c.setUserId(CheckApiTest.USER);
        c.setMenuName(menu);
        c.setCaption("설명");
        c.setAsOf(LocalDate.of(2026, 11, 15));
        c.setOverall(overall);
        for (Finding f : findings) c.addFinding(f);
        return checks.save(c);
    }

    private static Finding red(String code) {
        return new Finding(code, Severity.RED, null, null, "근거", "조항");
    }

    private static Finding green(String code) {
        return new Finding(code, Severity.GREEN, null, null, "안내", null);
    }

    @BeforeEach
    void setUp() {
        checks.deleteAll();
    }

    @Test
    void emptyStats() throws Exception {
        mvc.perform(get("/api/checks/stats").with(CheckApiTest.asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalChecks").value(0))
                .andExpect(jsonPath("$.ruleCounts", hasSize(0)))
                .andExpect(jsonPath("$.recentChecks", hasSize(0)));
    }

    @Test
    void countsRedRulesPerCheckAndListsRecent() throws Exception {
        // 한 검수에 같은 규칙이 두 번 걸려도 1건으로 센다
        check("딸기 케이크", Severity.RED, red("IMG_COUNT"), red("IMG_COUNT"), red("CAP_MEDICINE"));
        check("닭강정", Severity.RED, red("CAP_MEDICINE"), green("CAP_ORIGIN"));
        Check last = check("비빔밥", Severity.GREEN, green("INFO_AI_LABEL"));

        mvc.perform(get("/api/checks/stats").with(CheckApiTest.asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalChecks").value(3))
                .andExpect(jsonPath("$.redChecks").value(2))
                .andExpect(jsonPath("$.greenChecks").value(1))
                // 🔴만 센다 (CAP_ORIGIN, INFO_AI_LABEL 🟢는 빠짐), 많은 순
                .andExpect(jsonPath("$.ruleCounts", hasSize(2)))
                .andExpect(jsonPath("$.ruleCounts[0].ruleCode").value("CAP_MEDICINE"))
                .andExpect(jsonPath("$.ruleCounts[0].count").value(2))
                .andExpect(jsonPath("$.ruleCounts[0].title").value("의약품 오인"))
                .andExpect(jsonPath("$.ruleCounts[0].lawRef").isNotEmpty())
                .andExpect(jsonPath("$.ruleCounts[1].ruleCode").value("IMG_COUNT"))
                .andExpect(jsonPath("$.ruleCounts[1].count").value(1))
                // 최근 검수 최신순, 검수별 🔴 항목 수
                .andExpect(jsonPath("$.recentChecks", hasSize(3)))
                .andExpect(jsonPath("$.recentChecks[0].checkId").value(last.getId()))
                .andExpect(jsonPath("$.recentChecks[0].menuName").value("비빔밥"))
                .andExpect(jsonPath("$.recentChecks[0].redCount").value(0))
                .andExpect(jsonPath("$.recentChecks[2].menuName").value("딸기 케이크"))
                .andExpect(jsonPath("$.recentChecks[2].redCount").value(3));
    }

    @Test
    void limitsRecentChecks() throws Exception {
        for (int i = 0; i < 4; i++) check("메뉴" + i, Severity.GREEN);
        mvc.perform(get("/api/checks/stats").with(CheckApiTest.asUser()).param("limit", "2"))
                .andExpect(jsonPath("$.totalChecks").value(4))
                .andExpect(jsonPath("$.recentChecks", hasSize(2)));
        mvc.perform(get("/api/checks/stats").with(CheckApiTest.asUser()).param("limit", "0"))
                .andExpect(jsonPath("$.recentChecks", hasSize(1)));
    }
}
