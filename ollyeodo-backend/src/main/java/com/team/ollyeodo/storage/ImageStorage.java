package com.team.ollyeodo.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 저장소. DB에는 {@link #store}가 돌려준 경로만 저장하고,
 * 화면에는 {@link #signedUrl}로 발급한 URL을 내려준다.
 */
public interface ImageStorage {

    /**
     * 이미지를 저장하고 저장소 내부 경로(예: {@code menus/3f2a....jpg})를 돌려준다.
     *
     * @param folder 저장 폴더 (예: {@code menus}, {@code checks})
     */
    String store(String folder, MultipartFile file);

    /** 화면 표시용 서명 URL. 일정 시간 뒤 만료된다. */
    String signedUrl(String path);
}
