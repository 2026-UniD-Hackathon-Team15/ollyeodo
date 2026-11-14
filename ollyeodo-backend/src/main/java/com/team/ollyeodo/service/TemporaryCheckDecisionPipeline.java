package com.team.ollyeodo.service;

import com.team.ollyeodo.domain.Severity;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 이슈 #11에서 허용한 임시 구현. #9/#10 머지 후 실제 어댑터로 교체해야 한다. */
@Component
@ConditionalOnProperty(name = "app.checks.stub-enabled", havingValue = "true")
public class TemporaryCheckDecisionPipeline implements CheckDecisionPipeline {
    private static final Logger log = LoggerFactory.getLogger(TemporaryCheckDecisionPipeline.class);

    @Override
    public Result evaluate(Input input) {
        log.warn("임시 검수 파이프라인 사용: 실제 판정이 아닌 API 개발용 GREEN 응답입니다.");
        return new Result(Severity.GREEN, List.of(), input.caption(), null);
    }
}
