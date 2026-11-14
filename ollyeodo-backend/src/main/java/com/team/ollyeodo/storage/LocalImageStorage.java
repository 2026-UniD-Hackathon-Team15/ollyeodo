package com.team.ollyeodo.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 로컬 개발용 저장소. Supabase 없이 {@code uploads/} 폴더에 저장하고
 * {@code /local-images/**}로 그대로 서빙한다. 서명·만료는 없다.
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage, WebMvcConfigurer {

    private static final String URL_PREFIX = "/local-images/";

    private final Path root;
    private final String publicBaseUrl;

    public LocalImageStorage(
            @Value("${app.storage.local.dir:uploads}") String dir,
            @Value("${app.storage.local.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public String store(String folder, MultipartFile file) {
        String path = ImageFiles.newPath(folder, file);
        Path target = root.resolve(path);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("이미지를 저장하지 못했습니다.", e);
        }
        return path;
    }

    @Override
    public String signedUrl(String path) {
        ImageFiles.checkPath(path);
        return publicBaseUrl + URL_PREFIX + path;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(URL_PREFIX + "**")
                .addResourceLocations(root.toUri().toString());
    }
}
