package com.team.ollyeodo.ai;

import java.util.Objects;

/** 저장소나 HTTP 계층에 종속되지 않는 이미지 입력. */
public record ImageInput(byte[] bytes, String mediaType) {
    public ImageInput {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(mediaType, "mediaType");
        if (bytes.length == 0 || !mediaType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 데이터와 image/* 미디어 타입이 필요합니다.");
        }
        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
