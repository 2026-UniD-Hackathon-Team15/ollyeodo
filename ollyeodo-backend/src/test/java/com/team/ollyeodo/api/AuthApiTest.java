package com.team.ollyeodo.api;

import com.team.ollyeodo.config.SecurityConfig;
import com.team.ollyeodo.domain.Check;
import com.team.ollyeodo.domain.Finding;
import com.team.ollyeodo.domain.Severity;
import com.team.ollyeodo.repository.CheckRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** #71 로그인: 토큰 없으면 401, 내 기록만 조회·집계, 공개 API는 그대로 */
@SpringBootTest(properties = {"spring.ai.model.chat=none",
        "spring.datasource.url=jdbc:h2:mem:auth-api-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class AuthApiTest {
    private static final UUID A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired MockMvc mvc;
    @Autowired CheckRepository checks;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.team.ollyeodo.storage.ImageStorage storage;

    private static RequestPostProcessor as(UUID user) {
        return jwt().jwt(j -> j.subject(user.toString()).audience(List.of("authenticated")));
    }

    private Check check(UUID owner, String menu, Severity overall, Finding... findings) {
        Check c = new Check();
        c.setUserId(owner);
        c.setMenuName(menu);
        c.setRealImagePath("real/test.png");
        c.setAiImagePath("ai/test.png");
        c.setCaption("설명");
        c.setAsOf(LocalDate.of(2026, 11, 15));
        c.setOverall(overall);
        for (Finding f : findings) c.addFinding(f);
        return checks.save(c);
    }

    @BeforeEach
    void setUp() {
        checks.deleteAll();
        org.mockito.Mockito.when(storage.signedUrl(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(call -> "https://example.test/" + call.getArgument(0));
    }

    @Test
    void withoutTokenChecksApiIs401WithKoreanDetail() throws Exception {
        mvc.perform(multipart("/api/checks").param("menuName", "딸기 케이크"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
                .andExpect(jsonPath("$.detail").value(SecurityConfig.UNAUTHORIZED_DETAIL));
        mvc.perform(get("/api/checks/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/checks/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIs401() throws Exception {
        mvc.perform(get("/api/checks/stats").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(SecurityConfig.UNAUTHORIZED_DETAIL));
    }

    @Test
    void publicApisStayOpen() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/rules")).andExpect(status().isOk());
    }

    @Test
    void corsPreflightAllowsAuthorizationHeader() throws Exception {
        mvc.perform(options("/api/checks")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("authorization")));
    }

    @Test
    void otherUsersCheckIs404() throws Exception {
        Check mine = check(A, "닭강정", Severity.RED, new Finding("CAP_MEDICINE", Severity.RED, null, null, "근거", "조항"));
        mvc.perform(get("/api/checks/{id}", mine.getId()).with(as(A))).andExpect(status().isOk());
        mvc.perform(get("/api/checks/{id}", mine.getId()).with(as(B))).andExpect(status().isNotFound());
    }

    @Test
    void checksWithoutOwnerAreHidden() throws Exception {
        Check old = check(null, "로그인 전 기록", Severity.GREEN);
        mvc.perform(get("/api/checks/{id}", old.getId()).with(as(A))).andExpect(status().isNotFound());
    }

    @Test
    void statsCountOnlyMyChecks() throws Exception {
        check(A, "닭강정", Severity.RED, new Finding("CAP_MEDICINE", Severity.RED, null, null, "근거", "조항"));
        check(B, "피자", Severity.RED, new Finding("IMG_COUNT", Severity.RED, null, null, "근거", "조항"));
        check(B, "비빔밥", Severity.GREEN);
        check(null, "로그인 전 기록", Severity.RED, new Finding("IMG_LAYER", Severity.RED, null, null, "근거", "조항"));

        mvc.perform(get("/api/checks/stats").with(as(A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalChecks").value(1))
                .andExpect(jsonPath("$.redChecks").value(1))
                .andExpect(jsonPath("$.ruleCounts.length()").value(1))
                .andExpect(jsonPath("$.ruleCounts[0].ruleCode").value("CAP_MEDICINE"))
                .andExpect(jsonPath("$.recentChecks.length()").value(1))
                .andExpect(jsonPath("$.recentChecks[0].menuName").value("닭강정"));

        mvc.perform(get("/api/checks/stats").with(as(B)))
                .andExpect(jsonPath("$.totalChecks").value(2))
                .andExpect(jsonPath("$.greenChecks").value(1))
                .andExpect(jsonPath("$.ruleCounts[0].ruleCode").value("IMG_COUNT"));
    }
}
