package com.team.ollyeodo.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 판정 규칙. 근거 조항과 시행일을 두어, 개정 시 코드 수정 없이 행만 추가한다.
 * RuleEngine은 effectiveFrom ≤ asOf 인 규칙만 적용한다. (#5에서 13행 적재)
 */
@Entity
@Table(name = "rules")
@Getter
@Setter
@NoArgsConstructor
public class Rule {

    @Id
    private String code;               // 예: IMG_COUNT, CAP_MEDICINE

    @Column(nullable = false)
    private String title;

    private String lawRef;             // 예: 식품표시광고법 제8조①2호

    @Column(columnDefinition = "text")
    private String sourceUrl;

    private LocalDate effectiveFrom;   // 시행일
    private LocalDate effectiveTo;     // 종료일 (없으면 null)

    @Enumerated(EnumType.STRING)
    private Severity severity;         // 시행 중일 때 심각도

    @Enumerated(EnumType.STRING)
    private Severity preEffectiveSeverity;   // 시행 전 심각도 (예: AI_VIRTUAL_EXPERT 11/26까지 GREEN)

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> keywords;     // 캡션 검수용 표현 사전
}