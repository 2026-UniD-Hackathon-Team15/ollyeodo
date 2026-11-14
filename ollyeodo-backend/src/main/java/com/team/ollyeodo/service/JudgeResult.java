package com.team.ollyeodo.service;

import com.team.ollyeodo.domain.Severity;
import java.util.List;

/** RuleEngine 출력 (PM 확정 인터페이스, 이름·필드 변경 금지) */
public record JudgeResult(Severity overall, List<JudgedFinding> findings) {}