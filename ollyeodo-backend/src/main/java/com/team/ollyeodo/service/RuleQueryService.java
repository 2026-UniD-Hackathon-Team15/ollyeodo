package com.team.ollyeodo.service;

import com.team.ollyeodo.api.RuleResponse;
import com.team.ollyeodo.domain.Rule;
import com.team.ollyeodo.repository.RuleRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleQueryService {
    // §6 표의 표시 순서만 정의한다. 제목·근거·시행일·심각도는 DB에서 읽는다.
    private static final List<String> ORDER = List.of("IMG_EXTRA_ITEM", "IMG_COUNT", "IMG_FILL", "IMG_LAYER",
            "CAP_FACT", "CAP_ORIGIN", "CAP_DISEASE", "CAP_MEDICINE", "CAP_HFOOD", "CAP_SUPERLATIVE",
            "CAP_FAKE_REVIEW", "AI_VIRTUAL_EXPERT", "INFO_AI_LABEL");
    private final RuleRepository repository;
    private final Clock clock;

    public RuleQueryService(RuleRepository repository, @Qualifier("ruleApiClock") Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RuleResponse> findAll(LocalDate asOf) {
        LocalDate date = asOf == null ? LocalDate.now(clock) : asOf;
        return repository.findAll().stream()
                .sorted(Comparator.comparingInt((Rule r) -> {
                    int index = ORDER.indexOf(r.getCode());
                    return index < 0 ? ORDER.size() : index;
                }).thenComparing(Rule::getCode))
                .map(rule -> RuleResponse.from(rule, date)).toList();
    }
}
