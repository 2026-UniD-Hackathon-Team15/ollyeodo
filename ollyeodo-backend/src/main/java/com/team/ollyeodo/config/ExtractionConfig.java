package com.team.ollyeodo.config;

import com.team.ollyeodo.ai.ExtractionClient;
import com.team.ollyeodo.ai.ExtractionService;
import com.team.ollyeodo.ai.OpenAiExtractionClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;

@Configuration
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai", matchIfMissing = true)
public class ExtractionConfig {
    @Bean
    public ExtractionClient extractionClient(ChatClient.Builder builder,
            @Value("${app.extraction.image-reasoning-effort:}") String imageReasoningEffort) {
        return new OpenAiExtractionClient(builder.build(), imageReasoningEffort);
    }

    /** OpenAI starter가 사용하는 HTTP 클라이언트에도 서비스와 같은 제한시간을 적용한다. */
    @Bean
    public RestClientCustomizer extractionHttpTimeout(
            @Value("${app.extraction.timeout:60s}") Duration timeout) {
        return builder -> {
            var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout).build());
            factory.setReadTimeout(timeout);
            builder.requestFactory(factory);
        };
    }

    @Bean
    public ExtractionService extractionService(ExtractionClient client,
            @Qualifier("extractionExecutor") Executor executor,
            @Value("${app.extraction.timeout:60s}") Duration timeout) {
        return new ExtractionService(client, executor, timeout);
    }
}
