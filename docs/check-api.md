# 검수 API (#11)

## 계약

### 인증 (#71)
- 회원가입·로그인은 프론트가 **Supabase Auth**(이메일 + 비밀번호, 가입 이메일 인증 없음)로 하고, 받은 access token을 `Authorization: Bearer <access_token>`으로 보낸다.
- 서버는 비밀번호를 다루지 않고 토큰만 검증한다: 서명(`${SUPABASE_URL}/auth/v1/.well-known/jwks.json`, ES256), `iss` = `${SUPABASE_URL}/auth/v1`, `aud` = `authenticated`, 만료, `sub`(사용자 ID, uuid).
- **로그인 필요**: `POST /api/checks`, `GET /api/checks/{id}`, `GET /api/checks/stats` — 토큰이 없거나 잘못되면 **401** `{"detail": "로그인이 필요해요. 다시 로그인해 주세요."}`
- **공개**: `GET /api/health`, `GET /api/rules`
- 검수는 만든 사용자에게만 보인다. 다른 사람 검수 ID로 조회하면 **404** (있는지도 알리지 않음). 통계도 본인 기록만. 로그인 도입 전 기록(`user_id` 없음)은 아무에게도 안 보인다.
- 예전 HS256 프로젝트라면 서버 환경 변수 `SUPABASE_JWT_SECRET`로 검증할 수 있다 (지금 프로젝트는 필요 없음).

### 검수

- `POST /api/checks`: multipart `menuName`, `realImage`, `aiImage`, `caption`, 선택 `asOf` (`yyyy-MM-dd`). 기본 기준일은 Asia/Seoul 오늘.
- 성공: 201과 `Location: /api/checks/{id}`. `GET /api/checks/{id}`: 200, 같은 응답 필드.
- 두 이미지 모두 비어 있지 않은 JPEG/PNG/WEBP여야 한다. MIME 타입 기준 검증이며 실제 픽셀 디코딩 검증은 하지 않는다.
- 잘못된/누락 입력(`menuName`·`caption` 빈 값 포함): 400, 로그인 안 함: 401, AI 추출 실패: 422, 없는 ID: 404, AI/판정 의존성 미설정: 503.
- `findings`는 GREEN 안내도 포함하며 `checkOnce`를 보존한다. GREEN의 `regenPrompt`는 null이다 (#11 확정 댓글).
- 저장하는 것은 이미지 경로이며 POST/GET마다 Storage URL을 새로 발급한다.
- 422 중 실제 사진과 AI 사진이 다른 음식이면 `detail`에 "실제 사진과 AI 사진이 다른 음식이라 검수할 수 없어요…"가 그대로 온다 (§6.3 9).

## 검수 기록 통계 (#20)

`GET /api/checks/stats?limit=10` — **로그인한 사용자 기록만** 센다. 저장된 판정을 세기만 하고 다시 판정하지 않는다. `limit`은 최근 검수 개수 (기본 10, 1~50).

```json
{
  "totalChecks": 21, "redChecks": 15, "greenChecks": 6,
  "ruleCounts": [
    {"ruleCode": "CAP_MEDICINE", "title": "의약품 오인", "lawRef": "식품표시광고법 제8조①2호", "count": 9}
  ],
  "recentChecks": [
    {"checkId": 21, "menuName": "닭강정", "overall": "RED", "redCount": 1,
     "asOf": "2026-11-15", "createdAt": "2026-11-14T17:51:02Z"}
  ]
}
```

- `ruleCounts`: 🔴로 걸린 규칙별 **검수 건수**, 많은 순. 한 검수에서 같은 규칙이 여러 번 걸려도 1건. 🟢 안내(원산지 확인, 해당 없음 등)는 세지 않는다.
- `recentChecks`: 최신순. `redCount`는 그 검수의 🔴 항목 수. 결과 화면은 `/checks/{checkId}`.

## 판정 연결 (#9 · #10)

`CheckDecisionPipeline`은 #11 소유의 통합 경계이고, 실제 구현은 `RuleEngineCheckDecisionPipeline`이다.
(#9·#10 머지 전 임시 GREEN 구현은 제거했다.)

1. `Input`을 `JudgeInput`으로 변환한다.
2. `RuleEngine.judge(input, ruleRepository.findAll())` — 규칙(근거 조항·시행일·키워드)은 rules 테이블(#5)
3. `RegenPromptBuilder.build(judged, realFirst, caption)` — 🔴면 개선 프롬프트·문구 수정안, 🟢면 null·원문
4. `JudgedFinding` 전체와 `RegenOutput`을 `Result`로 변환한다.

AI(OpenAI) 구성이 꺼져 있으면(`SPRING_AI_MODEL_CHAT=none`, 키 없음) 503으로 응답해 검증되지 않은 통과 결과를 만들지 않는다.

`CheckService`는 실물/AI 이미지 저장 → `extractImages(ai, real)`와 `extractCaption`을 **동시에** 실행 →
`compare` 2회(facts=null) → 위 통합 경계 → Check/Finding 원자 저장 순서다.
실물 먼저 추출하는 A안은 기존 ExtractionService 안에 유지한다. AI 요청 중에는 DB 트랜잭션을 열지 않는다.

## 스키마 및 제한

Finding에 `checkOnce`를 추가했다. 기존 데이터가 있는 DB에는 다음을 적용한 뒤 배포한다.

```sql
ALTER TABLE findings ADD COLUMN IF NOT EXISTS check_once boolean NOT NULL DEFAULT false;
```

이 작업에서 원격 Supabase 스키마는 변경하지 않았다. 원격 반영 후 PM에게 컬럼 추가 사실을 공유한다.
Storage 인터페이스에 삭제 기능이 없으므로 추출/DB 실패 시 이미 업로드된 파일은 남을 수 있다.
재시도는 새 검수를 생성하며 요청 중복 방지 키는 이번 범위에 없다.

프론트의 `regenPrompt` 타입(null 허용), 이전 `menuId` 전송 제거는 #17에서 맞춰야 한다.

## 검증

`ollyeodo-backend`에서 `./gradlew test`와 `./gradlew build` 실행.
CheckApiTest는 외부 AI/Storage를 대역으로 사용하고 H2에 실제 저장 후 GET하여 cascade,
checkOnce, 이미지 URL 재발급, 날짜 기본값, 입력 오류와 422/404를 검증한다.
