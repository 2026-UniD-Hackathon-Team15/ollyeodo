package com.team.ollyeodo.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.ai.model.chat=none", "app.checks.stub-enabled=false"})
@AutoConfigureMockMvc
class CheckUnavailableTest {
    @Autowired MockMvc mvc;

    @Test
    void disabledDependenciesDoNotProduceAnUnverifiedGreenResult() throws Exception {
        mvc.perform(multipart("/api/checks")
                        .file(new MockMultipartFile("realImage", "real.png", "image/png", new byte[]{1}))
                        .file(new MockMultipartFile("aiImage", "ai.png", "image/png", new byte[]{1}))
                        .param("caption", "설명"))
                .andExpect(status().isServiceUnavailable());
    }
}
