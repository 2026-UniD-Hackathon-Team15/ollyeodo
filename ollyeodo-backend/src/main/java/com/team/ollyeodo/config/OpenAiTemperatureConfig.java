package com.team.ollyeodo.config;

import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class OpenAiTemperatureConfig {
    /** Starter 기본값(0.7)을 지워 사용자가 명시한 값만 전송한다. */
    @Bean
    static BeanPostProcessor optionalOpenAiTemperature(Environment environment) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof OpenAiChatProperties properties) {
                    String value = environment.getProperty("spring.ai.openai.chat.options.temperature");
                    properties.getOptions().setTemperature(
                            value == null || value.isBlank() ? null : Double.valueOf(value));
                }
                return bean;
            }
        };
    }
}
