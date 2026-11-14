package com.team.ollyeodo.config;

import com.team.ollyeodo.repository.RuleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** JSON 배열은 Hibernate로 저장하므로 H2/ PostgreSQL 전용 SQL 캐스팅이 필요 없다. */
@Component
public class RuleDataSeeder implements ApplicationRunner {
    private final RuleRepository repository;

    public RuleDataSeeder(RuleRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (var rule : RuleSeedData.rules()) {
            if (!repository.existsById(rule.getCode())) {
                repository.save(rule);
            }
        }
    }
}
