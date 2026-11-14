package com.team.ollyeodo.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AI(OpenAI) 구성이 꺼져 있으면(키 없음) 검증되지 않은 통과 결과를 만들지 않고 503을 준다. */
@SpringBootTest(properties = "spring.ai.model.chat=none")
@AutoConfigureMockMvc
class CheckUnavailableTest {
    @Autowired MockMvc mvc;

    @Test
    void disabledDependenciesDoNotProduceAnUnverifiedGreenResult() throws Exception {
        mvc.perform(checkRequest()
                        .file(new MockMultipartFile("realImage", "real.png", "image/png", new byte[]{1}))
                        .file(new MockMultipartFile("aiImage", "ai.png", "image/png", new byte[]{1}))
                        .param("caption", "설명"))
                .andExpect(status().isServiceUnavailable());
    }
    private static org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder checkRequest() {
        var request = multipart("/api/checks");
        request.param("menuName", "딸기 케이크");
        return request;
    }

}
