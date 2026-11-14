package com.team.ollyeodo.storage;

import com.team.ollyeodo.config.SupabaseProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Supabase Storage 비공개 버킷에 service_role 키로 업로드하고 서명 URL을 발급한다.
 * service_role 키는 서버에만 있고 프론트로 내려가지 않는다.
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "supabase")
public class SupabaseImageStorage implements ImageStorage {

    private final RestClient client;
    private final String storageBaseUrl;
    private final String bucket;
    private final int signedUrlExpiresIn;

    public SupabaseImageStorage(SupabaseProperties props) {
        Assert.hasText(props.url(), "SUPABASE_URL 환경변수가 필요합니다.");
        Assert.hasText(props.serviceRoleKey(), "SUPABASE_SERVICE_ROLE_KEY 환경변수가 필요합니다.");
        this.storageBaseUrl = props.url().replaceAll("/+$", "") + "/storage/v1";
        this.bucket = props.storage().bucket();
        this.signedUrlExpiresIn = props.storage().signedUrlExpiresIn();
        this.client = RestClient.builder()
                .baseUrl(storageBaseUrl)
                .defaultHeader("apikey", props.serviceRoleKey())
                .defaultHeader("Authorization", "Bearer " + props.serviceRoleKey())
                .build();
    }

    @Override
    public String store(String folder, MultipartFile file) {
        String path = ImageFiles.newPath(folder, file);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("이미지를 읽지 못했습니다.", e);
        }
        client.post()
                .uri(objectUri("/object/", path))
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(bytes)
                .retrieve()
                .toBodilessEntity();
        return path;
    }

    @Override
    public String signedUrl(String path) {
        ImageFiles.checkPath(path);
        SignResponse res = client.post()
                .uri(objectUri("/object/sign/", path))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("expiresIn", signedUrlExpiresIn))
                .retrieve()
                .body(SignResponse.class);
        if (res == null || res.signedURL() == null) {
            throw new IllegalStateException("서명 URL 발급에 실패했습니다: " + path);
        }
        // signedURL은 "/object/sign/images/...?token=..." 형태의 상대 경로다.
        return storageBaseUrl + res.signedURL();
    }

    /**
     * URI 템플릿 변수({path})로 넘기면 "/"가 %2F로 인코딩되고,
     * Supabase가 그 경로로 서명한 URL은 조회 시 InvalidSignature가 난다.
     * path는 ImageFiles가 [a-z0-9_-]/[a-f0-9-].ext 형태로만 만들므로 그대로 이어 붙여도 안전하다.
     */
    private String objectUri(String prefix, String path) {
        return prefix + bucket + "/" + path;
    }

    private record SignResponse(String signedURL) {
    }
}
