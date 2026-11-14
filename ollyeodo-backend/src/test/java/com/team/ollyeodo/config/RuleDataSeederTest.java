package com.team.ollyeodo.config;

import com.team.ollyeodo.domain.Severity;
import com.team.ollyeodo.repository.RuleRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.ai.model.chat=none",
        "spring.datasource.url=jdbc:h2:mem:rule-seed;MODE=PostgreSQL",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class RuleDataSeederTest {
    @Autowired RuleRepository repository;
    @Autowired RuleDataSeeder seeder;
    @Autowired EntityManager em;

    @Test
    void seedsExactlyThirteenUniqueCodesAndDocumentedKeywords() {
        em.clear();
        var rules = repository.findAll();
        assertThat(rules).hasSize(13);
        assertThat(rules).extracting(r -> r.getCode()).doesNotHaveDuplicates().containsExactlyInAnyOrder(
                "IMG_EXTRA_ITEM", "IMG_COUNT", "IMG_FILL", "IMG_LAYER", "CAP_FACT", "CAP_ORIGIN",
                "CAP_DISEASE", "CAP_MEDICINE", "CAP_HFOOD", "CAP_SUPERLATIVE", "CAP_FAKE_REVIEW",
                "AI_VIRTUAL_EXPERT", "INFO_AI_LABEL");
        assertThat(rules).allSatisfy(r -> {
            assertThat(r.getSeverity()).isIn(Severity.GREEN, Severity.RED);
            assertThat(r.getPreEffectiveSeverity()).isEqualTo(Severity.GREEN);
            assertThat(r.getEffectiveTo()).isNull();
            assertThat(r.getTitle()).doesNotContain("위법", "불법");
            assertThat(r.getSourceUrl()).isEqualTo(r.getLawRef() == null ? null : "https://www.law.go.kr");
        });
        assertThat(repository.findById("CAP_MEDICINE").orElseThrow().getKeywords())
                .containsExactly("디톡스", "해독", "면역력 강화");
        assertThat(repository.findById("CAP_DISEASE").orElseThrow().getKeywords())
                .containsExactly("감기 예방", "당뇨에 좋은");
    }

    @Test
    void preservesExistingRowsAndRestoresOnlyMissingCodes() {
        var medicine = repository.findById("CAP_MEDICINE").orElseThrow();
        medicine.setTitle("기존 제목 유지");
        repository.saveAndFlush(medicine);
        repository.deleteById("IMG_COUNT");
        repository.flush();
        seeder.run(new DefaultApplicationArguments());
        seeder.run(new DefaultApplicationArguments());
        repository.flush();
        em.clear();
        assertThat(repository.count()).isEqualTo(13);
        assertThat(repository.findById("CAP_MEDICINE").orElseThrow().getTitle()).isEqualTo("기존 제목 유지");
    }

    @Test
    void virtualExpertAndOriginFollowTwoSignalScope() {
        var expert = repository.findById("AI_VIRTUAL_EXPERT").orElseThrow();
        assertThat(expert.getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 11, 27));
        assertThat(expert.getSeverity()).isEqualTo(Severity.RED);
        assertThat(expert.getPreEffectiveSeverity()).isEqualTo(Severity.GREEN);
        var origin = repository.findById("CAP_ORIGIN").orElseThrow();
        assertThat(origin.getSeverity()).isEqualTo(Severity.GREEN);
        assertThat(origin.getTitle()).isEqualTo("원산지 표기가 실제와 같은지 직접 확인해 주세요");
        assertThat(origin.getLawRef()).isEqualTo("원산지표시법 제5조, 제6조①②");
        assertThat(repository.findById("CAP_FACT").orElseThrow().getTitle()).contains("실물 사진 추출값");
        assertThat(repository.findById("INFO_AI_LABEL").orElseThrow().getKeywords()).isEqualTo(List.of());
    }
}
