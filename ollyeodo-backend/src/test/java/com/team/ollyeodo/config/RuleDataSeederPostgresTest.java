package com.team.ollyeodo.config;

import com.team.ollyeodo.domain.Severity;
import com.team.ollyeodo.repository.RuleRepository;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/** 명시적으로 실행할 때만 실제 PostgreSQL에 누락 규칙을 적재·검증한다. 기존 행은 유지한다. */
@Tag("live")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
        "spring.ai.model.chat=none",
        "spring.datasource.url=${SUPABASE_DB_URL}",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.datasource.hikari.minimum-idle=0",
        "spring.datasource.hikari.connection-timeout=10000"
})
class RuleDataSeederPostgresTest {
    @Autowired RuleRepository repository;
    @Autowired RuleDataSeeder seeder;
    @Autowired DataSource dataSource;

    @Test
    void seedsAndReadsJsonKeywordsOnPostgresqlWithoutDuplicates() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
        var before = repository.findAll();
        assertThat(before).hasSize(13);
        assertThat(before).extracting(r -> r.getCode()).doesNotHaveDuplicates();
        seeder.run(new DefaultApplicationArguments());
        assertThat(repository.count()).isEqualTo(13);
        assertThat(repository.findById("CAP_MEDICINE").orElseThrow().getKeywords())
                .containsExactly("디톡스", "해독", "면역력 강화");
        var expert = repository.findById("AI_VIRTUAL_EXPERT").orElseThrow();
        assertThat(expert.getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 11, 27));
        assertThat(expert.getSeverity()).isEqualTo(Severity.RED);
        assertThat(expert.getPreEffectiveSeverity()).isEqualTo(Severity.GREEN);
        assertThat(repository.findById("CAP_ORIGIN").orElseThrow().getSeverity()).isEqualTo(Severity.GREEN);
    }
}
