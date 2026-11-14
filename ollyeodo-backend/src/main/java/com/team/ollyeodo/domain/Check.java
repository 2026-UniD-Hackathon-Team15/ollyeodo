package com.team.ollyeodo.domain;

import com.team.ollyeodo.ai.ExtractedAttrs;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 검수 1건. 입력은 실물 사진 + AI 이미지 + 메뉴 설명(홍보 문구).
 * Check 1 : N Finding. 결과는 PASS / FAIL 두 가지.
 */
@Entity
@Table(name = "checks")
@Getter
@Setter
@NoArgsConstructor
public class Check {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 실물 사진 경로 (Storage) */
    private String realImagePath;

    /** AI 이미지 경로 (Storage) */
    private String aiImagePath;

    /** 메뉴 설명(홍보 문구) */
    @Column(columnDefinition = "text")
    private String caption;

    /** 판정 기준일 (기본값 오늘, Asia/Seoul) */
    private LocalDate asOf;

    @JdbcTypeCode(SqlTypes.JSON)
    private ExtractedAttrs aiAttrs;

    @JdbcTypeCode(SqlTypes.JSON)
    private ExtractedAttrs realAttrs;

    /** 최종 결과: PASS / FAIL */
    @Enumerated(EnumType.STRING)
    private Verdict verdict;

    @Column(columnDefinition = "text")
    private String captionFix;

    @JdbcTypeCode(SqlTypes.JSON)
    private RegenPrompt regenPrompt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "check", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Finding> findings = new ArrayList<>();

    /** Finding을 추가할 때는 이 메서드를 쓴다 (양쪽 관계를 같이 맞춤) */
    public void addFinding(Finding finding) {
        findings.add(finding);
        finding.setCheck(this);
    }
}