package com.team.ollyeodo.domain;

/** 재생성 프롬프트 한/영. checks.regen_prompt(jsonb)에 저장된다. (기획서 §8: ko, en) */
public record RegenPrompt(String ko, String en) {}