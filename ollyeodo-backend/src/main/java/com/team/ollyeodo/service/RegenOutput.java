package com.team.ollyeodo.service;

import com.team.ollyeodo.domain.RegenPrompt;

/**
 * 개선 프롬프트(#10) 결과.
 *
 * @param regenPrompt 한/영 개선 프롬프트. 종합이 GREEN이면 null
 * @param captionFix  문구 수정안. 종합이 GREEN이면 원래 문구 그대로
 */
public record RegenOutput(RegenPrompt regenPrompt, String captionFix) {}
