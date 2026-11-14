package com.team.ollyeodo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.ai.model.chat=none",
        "spring.datasource.url=jdbc:h2:mem:context;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OllyeodoBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
