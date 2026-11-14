package com.team.ollyeodo.api;

import com.team.ollyeodo.service.CheckService;
import com.team.ollyeodo.service.CheckStatsService;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/checks")
public class CheckController {
    private final CheckService service;
    private final CheckStatsService stats;

    public CheckController(CheckService service, CheckStatsService stats) {
        this.service = service;
        this.stats = stats;
    }

    /** 검수 기록 통계 (#20): 자주 걸리는 규칙 + 최근 검수 목록. limit 기본 10, 최대 50 */
    @GetMapping("/stats")
    public CheckStatsResponse stats(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(value = "limit", required = false) Integer limit) {
        return stats.stats(userId(jwt), limit);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CheckResponse> create(@AuthenticationPrincipal Jwt jwt,
            @RequestPart(value = "realImage", required = false) MultipartFile realImage,
            @RequestPart(value = "aiImage", required = false) MultipartFile aiImage, @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "menuName", required = false) String menuName,
            @RequestParam(value = "asOf", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        CheckResponse response = service.create(userId(jwt), menuName, realImage, aiImage, caption, asOf);
        return ResponseEntity.created(URI.create("/api/checks/" + response.checkId())).body(response);
    }

    @GetMapping("/{id}")
    public CheckResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") long id) {
        return service.get(userId(jwt), id);
    }

    /** 토큰의 sub = Supabase 사용자 ID (SecurityConfig에서 UUID인지 이미 확인) */
    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
