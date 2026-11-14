package com.team.ollyeodo.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.ollyeodo.ai.ExtractedAttrs;
import com.team.ollyeodo.repository.CheckRepository;
import com.team.ollyeodo.repository.MenuRepository;
import com.team.ollyeodo.repository.RuleRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** jsonb 컬럼이 record로 저장·조회되는지 확인 (H2 메모리 DB에서 실행, Supabase는 건드리지 않음) */
@DataJpaTest
class EntityMappingTest {

    @Autowired MenuRepository menuRepository;
    @Autowired CheckRepository checkRepository;
    @Autowired RuleRepository ruleRepository;
    @Autowired EntityManager em;

    /** 테스트용 메뉴: 딸기 크레이프 케이크 (딸기 4개, 국내산, 8겹) */
    private Menu saveStrawberryCake() {
        MenuFacts facts = new MenuFacts(
                List.of(new MenuFacts.Ingredient("딸기", 4, "국내산")),
                8, "1조각");
        return menuRepository.save(new Menu("딸기 크레이프 케이크", facts, "1/real/cake.jpg"));
    }

    @Test
    @DisplayName("Menu.facts(jsonb) 저장 후 다시 읽으면 값이 같다")
    void menuFactsRoundTrip() {
        Long id = saveStrawberryCake().getId();
        em.flush();   // DB에 실제로 저장
        em.clear();   // 메모리 캐시를 비워서, 다음 조회는 DB에서 다시 읽게 함

        Menu found = menuRepository.findById(id).orElseThrow();
        assertThat(found.getFacts().layers()).isEqualTo(8);
        assertThat(found.getFacts().ingredients().get(0).count()).isEqualTo(4);
        assertThat(found.getFacts().ingredients().get(0).origin()).isEqualTo("국내산");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Check의 aiAttrs·regenPrompt(jsonb)와 Finding(1:N)이 함께 저장된다")
    void checkWithFindingsRoundTrip() {
        Menu menu = saveStrawberryCake();

        Check check = new Check();
        check.setMenu(menu);
        check.setCaption("12겹 크레이프, 디톡스 딸기");
        check.setAsOf(LocalDate.of(2026, 11, 14));
        check.setAiAttrs(new ExtractedAttrs(
                List.of(new ExtractedAttrs.Item("딸기", 9, "윗면")),
                12, 0.9, "흰 원형 접시", List.of("민트 잎"), List.of(), List.of()));
        check.setOverall(Severity.RED);
        check.setRegenPrompt(new RegenPrompt("딸기 크레이프 케이크 1조각", "One slice of strawberry crepe cake"));
        check.addFinding(new Finding("IMG_COUNT", Severity.RED, "딸기 9개", "딸기 4개",
                "실물의 2.25배", "식품표시광고법 제8조①4호"));

        Long id = checkRepository.save(check).getId();
        em.flush();
        em.clear();

        Check found = checkRepository.findById(id).orElseThrow();
        assertThat(found.getAiAttrs().items().get(0).count()).isEqualTo(9);
        assertThat(found.getAiAttrs().fillRatio()).isEqualTo(0.9);
        assertThat(found.getRegenPrompt().en()).startsWith("One slice");
        assertThat(found.getOverall()).isEqualTo(Severity.RED);
        assertThat(found.getFindings()).hasSize(1);
        assertThat(found.getFindings().get(0).getEvidence()).isEqualTo("실물의 2.25배");
    }

    @Test
    @DisplayName("Rule.keywords(jsonb 배열) 저장 후 다시 읽으면 값이 같다")
    void ruleKeywordsRoundTrip() {
        Rule rule = new Rule();
        rule.setCode("CAP_MEDICINE");
        rule.setTitle("의약품 오인");
        rule.setLawRef("식품표시광고법 제8조①2호");
        rule.setEffectiveFrom(LocalDate.of(2025, 9, 19));
        rule.setSeverity(Severity.RED);
        rule.setKeywords(List.of("디톡스", "해독", "면역력 강화"));
        ruleRepository.save(rule);
        em.flush();
        em.clear();

        Rule found = ruleRepository.findById("CAP_MEDICINE").orElseThrow();
        assertThat(found.getKeywords()).containsExactly("디톡스", "해독", "면역력 강화");
    }
}