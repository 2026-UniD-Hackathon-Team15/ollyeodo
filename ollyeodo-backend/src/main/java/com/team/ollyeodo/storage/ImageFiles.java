package com.team.ollyeodo.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** 저장소 구현체가 공통으로 쓰는 검증·경로 생성 로직. */
final class ImageFiles {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );
    private static final Pattern FOLDER = Pattern.compile("[a-z0-9_-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_-]+/[a-f0-9-]+\\.(jpg|png|webp)");

    private ImageFiles() {
    }

    static String newPath(String folder, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어 있습니다.");
        }
        if (folder == null || !FOLDER.matcher(folder).matches()) {
            throw new IllegalArgumentException("잘못된 폴더 이름입니다: " + folder);
        }
        String ext = EXTENSIONS.get(file.getContentType());
        if (ext == null) {
            throw new IllegalArgumentException("JPG, PNG, WEBP 이미지만 올릴 수 있습니다.");
        }
        return folder + "/" + UUID.randomUUID() + "." + ext;
    }

    static void checkPath(String path) {
        if (path == null || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("잘못된 이미지 경로입니다: " + path);
        }
    }
}
