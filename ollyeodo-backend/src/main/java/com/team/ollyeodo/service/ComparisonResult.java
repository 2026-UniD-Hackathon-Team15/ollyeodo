package com.team.ollyeodo.service;

import java.util.List;

/** 이미지 비교 결과: 항목별 판정 + 안내 문구 */
public record ComparisonResult(
        List<FindingCandidate> findings,
        List<String> notices
) {}
