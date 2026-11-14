package com.team.ollyeodo.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiTemperatureConfigTest {
    @Test
    void omitsStarterDefaultWhenUnsetOrBlankButKeepsExplicitZero() {
        for (String value : new String[]{"unset", "", "0", "0.5"}) {
            var env = new MockEnvironment();
            if (!value.equals("unset")) env.setProperty("spring.ai.openai.chat.options.temperature", value);
            var properties = new OpenAiChatProperties();
            OpenAiTemperatureConfig.optionalOpenAiTemperature(env)
                    .postProcessBeforeInitialization(properties, "openAiChatProperties");
            if (value.equals("unset") || value.isEmpty()) assertNull(properties.getOptions().getTemperature());
            else assertEquals(Double.valueOf(value), properties.getOptions().getTemperature());
        }
    }
}
