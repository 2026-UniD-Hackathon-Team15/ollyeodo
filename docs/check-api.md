# 검수 API (#11)

## 계약

- `POST /api/checks`: multipart `menuName`, `realImage`, `aiImage`, `caption`, 선택 `asOf` (`yyyy-MM-dd`). 기본 기준일은 Asia/Seoul 오늘.
- 성공: 201과 `Location: /api/checks/{id}`. `GET /api/checks/{id}`: 200, 같은 응답 필드.
- 두 이미지 모두 비어 있지 않은 JPEG/PNG/WEBP여야 한다. MIME 타입 기준 검증이며 실제 픽셀 디코딩 검증은 하지 않는다.
- 잘못된/누락 입력(`menuName`·`caption` 빈 값 포함): 400, AI 추출 실패: 422, 없는 ID: 404, AI/판정 의존성 미설정: 503.
- `findings`는 GREEN 안내도 포함하며 `checkOnce`를 보존한다. GREEN의 `regenPrompt`는 null이다 (#11 확정 댓글).
- 저장하는 것은 이미지 경로이며 POST/GET마다 Storage URL을 새로 발급한다.

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
