package com.team.ollyeodo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 항목별 판정 결과. Check N : 1 */
@Entity
@Table(name = "findings")
@Getter
@Setter
@NoArgsConstructor
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "check_id", nullable = false)
    private Check check;

    @Column(nullable = false)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    private String aiValue;
    private String realValue;

    @Column(columnDefinition = "text")
    private String evidence;

    private String lawRef;

    @Column(nullable = false)
    private boolean checkOnce;

    public Finding(String ruleCode, Severity severity, String aiValue,
                   String realValue, String evidence, String lawRef) {
        this.ruleCode = ruleCode;
        this.severity = severity;
        this.aiValue = aiValue;
        this.realValue = realValue;
        this.evidence = evidence;
        this.lawRef = lawRef;
    }
}
