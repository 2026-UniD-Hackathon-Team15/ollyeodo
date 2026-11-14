package com.team.ollyeodo.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleApiConfig {
    @Bean
    public Clock ruleApiClock(@Value("${APP_TIMEZONE:Asia/Seoul}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
