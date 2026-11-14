package com.team.ollyeodo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Supabase 접속 설정. 비밀값(service-role-key)은 환경변수로만 주입한다.
 */
@ConfigurationProperties(prefix = "supabase")
public record SupabaseProperties(
        String url,
        String serviceRoleKey,
        @DefaultValue Storage storage
) {
    public record Storage(
            @DefaultValue("images") String bucket,
            @DefaultValue("3600") int signedUrlExpiresIn
    ) {
    }
}
