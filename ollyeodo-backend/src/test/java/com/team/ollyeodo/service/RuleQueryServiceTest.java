package com.team.ollyeodo.service;

import com.team.ollyeodo.config.RuleApiConfig;
import com.team.ollyeodo.domain.Rule;
import com.team.ollyeodo.repository.RuleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RuleQueryServiceTest {
    @Test
    void omittedDateUsesClockTimezoneAndExplicitDateOverridesToday() {
        var repository = mock(RuleRepository.class);
        var rule = new Rule();
        rule.setCode("AI_VIRTUAL_EXPERT");
        rule.setEffectiveFrom(LocalDate.of(2026, 11, 27));
        when(repository.findAll()).thenReturn(List.of(rule));
        var instant = Instant.parse("2026-11-26T15:30:00Z");
        var seoul = new RuleQueryService(repository, Clock.fixed(instant, ZoneId.of("Asia/Seoul")));
        var utc = new RuleQueryService(repository, Clock.fixed(instant, ZoneId.of("UTC")));
        assertThat(seoul.findAll(null).getFirst().upcoming()).isFalse();
        assertThat(utc.findAll(null).getFirst().upcoming()).isTrue();
        assertThat(seoul.findAll(LocalDate.of(2026, 11, 26)).getFirst().upcoming()).isTrue();
    }

    @Test
    void clockDefaultsToSeoulAndAcceptsAppTimezone() {
        var runner = new ApplicationContextRunner().withUserConfiguration(RuleApiConfig.class);
        runner.run(context -> assertThat(context.getBean("ruleApiClock", Clock.class).getZone())
                .isEqualTo(ZoneId.of("Asia/Seoul")));
        runner.withPropertyValues("APP_TIMEZONE=Pacific/Honolulu")
                .run(context -> assertThat(context.getBean("ruleApiClock", Clock.class).getZone())
                        .isEqualTo(ZoneId.of("Pacific/Honolulu")));
    }
}
