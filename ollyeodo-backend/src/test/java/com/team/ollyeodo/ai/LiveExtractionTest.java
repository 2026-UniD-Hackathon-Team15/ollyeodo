package com.team.ollyeodo.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import com.team.ollyeodo.OllyeodoBackendApplication;
import org.springframework.core.io.ClassPathResource;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@Tag("live")
class LiveExtractionTest {

    @Test
    void extractsDemoFourTimesAndCaption() throws Exception {
        var resource = new ClassPathResource("demo/strawberry-cake.png");
        String mediaType = "image/png";
        if (!resource.exists()) {
            resource = new ClassPathResource("demo/strawberry-cake.jpg");
            mediaType = "image/jpeg";
        }
        assumeTrue(resource.exists(), "demo/strawberry-cake.png 또는 .jpg가 없어 live 테스트를 건너뜁니다.");
        byte[] bytes;
        try (var input = resource.getInputStream()) {
            bytes = input.readAllBytes();
        }
        var image = new ImageInput(bytes, mediaType);
        try (var context = new SpringApplicationBuilder(OllyeodoBackendApplication.class)
                .properties("spring.main.web-application-type=none").run()) {
            var service = context.getBean(ExtractionService.class);
            var images = service.extractImages(image, image);
            assertNotNull(images.ai().first());
            assertNotNull(images.ai().second());
            assertNotNull(images.real().first());
            assertNotNull(images.real().second());
            assertNotNull(images.ai().first().items());
            assertFalse(images.ai().first().items().isEmpty());
            var claims = service.extractCaption("국내산 딸기 4개를 올린 8겹 케이크, 200g. 약사 추천 디저트");
            assertEquals(8, claims.layers());
            assertTrue(claims.ingredients().stream().anyMatch(i -> "딸기".equals(i.name()) && Integer.valueOf(4).equals(i.count())));
            assertFalse(claims.expressions().isEmpty());
            var mapper = new ObjectMapper();
            Path report = Path.of("build/reports/ai-extraction-demo.json");
            Files.createDirectories(report.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(report.toFile(),
                    java.util.Map.of("fillRatioTolerance", "UNSET", "images", images, "caption", claims));
            System.out.println("AI agreement: " + images.ai().agreement());
            System.out.println("Real agreement: " + images.real().agreement());
        }
    }
}
