package com.team.ollyeodo.storage;

import com.team.ollyeodo.config.SupabaseProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Supabase Storage에 올리고 서명 URL로 다시 받아본다.
 * 환경변수가 있을 때만 돈다:
 * {@code set -a; . ../.env; set +a; ./gradlew test --tests '*SupabaseImageStorageSmokeTest'}
 */
@EnabledIfEnvironmentVariable(named = "SUPABASE_SERVICE_ROLE_KEY", matches = "(?!replace_me).+")
class SupabaseImageStorageSmokeTest {

    // 1x1 투명 PNG
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @Test
    void 업로드한_이미지를_서명_URL로_조회한다() {
        var props = new SupabaseProperties(
                System.getenv("SUPABASE_URL"),
                System.getenv("SUPABASE_SERVICE_ROLE_KEY"),
                new SupabaseProperties.Storage("images", 60));
        var storage = new SupabaseImageStorage(props);

        String path = storage.store("smoke", new MockMultipartFile("image", "a.png", "image/png", PNG));
        String url = storage.signedUrl(path);

        byte[] downloaded = RestClient.create().get().uri(URI.create(url)).retrieve().body(byte[].class);
        assertThat(url).contains("token=");
        assertThat(downloaded).isEqualTo(PNG);
    }
}
