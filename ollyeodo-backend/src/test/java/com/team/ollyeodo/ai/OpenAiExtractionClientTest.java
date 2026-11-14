package com.team.ollyeodo.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.ollyeodo.config.AsyncConfig;
import com.team.ollyeodo.config.ExtractionConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class OpenAiExtractionClientTest {
    @Test
    void schemaKeepsSnakeCaseAndNullableUnknowns() throws Exception {
        var converter = new BeanOutputConverter<>(ExtractedAttrs.class);
        var schema = new ObjectMapper().readTree(OpenAiExtractionClient.nullableSchema(converter.getJsonSchema()));
        var properties = schema.path("properties");
        assertTrue(properties.has("fill_ratio"));
        assertTrue(properties.has("visible_text"));
        assertFalse(properties.has("fillRatio"));
        assertEquals(10, schema.path("required").size());
        assertFalse(schema.path("additionalProperties").asBoolean());
        assertTrue(properties.path("layers").path("type").toString().contains("null"));
        var result = converter.convert("""
                {"items":[],"layers":null,"fill_ratio":null,"plate":null,"garnish":[],
                 "people":[{"attire":"흰 가운","title_text":"약사"}],"visible_text":["추천"]}
                """);
        assertNull(result.layers());
        assertEquals("약사", result.people().getFirst().titleText());
        assertEquals("추천", result.visibleText().getFirst());
    }

    @Test
    void disablesExtractionAlongWithProviderForOfflineBuild() {
        new ApplicationContextRunner().withUserConfiguration(AsyncConfig.class, ExtractionConfig.class)
                .withPropertyValues("spring.ai.model.chat=none")
                .run(context -> assertThat(context).doesNotHaveBean(ExtractionClient.class));
    }
}
