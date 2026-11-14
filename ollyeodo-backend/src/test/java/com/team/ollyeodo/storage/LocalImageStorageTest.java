package com.team.ollyeodo.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalImageStorageTest {

    @TempDir
    Path dir;

    @Test
    void 저장하면_경로를_돌려주고_URL로_조회할_수_있다() throws Exception {
        var storage = new LocalImageStorage(dir.toString(), "http://localhost:8080/");
        var file = new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2, 3});

        String path = storage.store("menus", file);

        assertThat(path).matches("menus/[a-f0-9-]+\\.png");
        assertThat(Files.readAllBytes(dir.resolve(path))).containsExactly(1, 2, 3);
        assertThat(storage.signedUrl(path)).isEqualTo("http://localhost:8080/local-images/" + path);
    }

    @Test
    void 이미지가_아니면_거절한다() {
        var storage = new LocalImageStorage(dir.toString(), "http://localhost:8080");
        var file = new MockMultipartFile("image", "a.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> storage.store("menus", file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 경로_조작을_막는다() {
        var storage = new LocalImageStorage(dir.toString(), "http://localhost:8080");

        assertThatThrownBy(() -> storage.signedUrl("../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
