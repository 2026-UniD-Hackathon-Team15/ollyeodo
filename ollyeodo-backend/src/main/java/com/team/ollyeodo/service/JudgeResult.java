package com.team.ollyeodo.service;

import com.team.ollyeodo.domain.Severity;

import java.util.List;

/**
 * RuleEngine(#9)의 출력. 종합(overall)은 🔴 하나라도 있으면 RED, 아니면 GREEN (MVP §6.3).
 * #9·#10·#11 공통 인터페이스 (이슈 #9 코멘트 참고).
 */
public record JudgeResult(Severity overall, List<JudgedFinding> findings) {}
