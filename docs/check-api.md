# 검수 API (#11)

## 계약

- `POST /api/checks`: multipart `realImage`, `aiImage`, `caption`, 선택 `asOf` (`yyyy-MM-dd`). 기본 기준일은 Asia/Seoul 오늘.
- 성공: 201과 `Location: /api/checks/{id}`. `GET /api/checks/{id}`: 200, 같은 응답 필드.
- 두 이미지 모두 비어 있지 않은 JPEG/PNG/WEBP여야 한다. MIME 타입 기준 검증이며 실제 픽셀 디코딩 검증은 하지 않는다.
- 잘못된/누락 입력: 400, AI 추출 실패: 422, 없는 ID: 404, AI/판정 의존성 미설정: 503.
- `findings`는 GREEN 안내도 포함하며 `checkOnce`를 보존한다. GREEN의 `regenPrompt`는 null이다 (#11 확정 댓글).
- 저장하는 것은 이미지 경로이며 POST/GET마다 Storage URL을 새로 발급한다.

## #9/#10 머지 전 개발 모드

`app.checks.stub-enabled=true` (`APP_CHECKS_STUB_ENABLED=true`)일 때만 이슈에서 허용한
임시 GREEN/빈 findings/null regenPrompt가 활성화된다. 실제 검수가 아니므로 운영에 사용하지 않는다.
기본은 비활성화이며 판정 구현 없이 결과가 통과로 저장되는 것을 막는다.
AI 추출은 별도 실제 구성 또는 테스트 대역이 필요하다.

`CheckDecisionPipeline`은 #11 소유의 통합 경계다. #9/#10 클래스나 확정 시그니처는 수정/선점하지 않았다.
머지 후 임시 구현을 제거하고 실제 구현에서 다음 순서로 연결한다.

1. `Input`을 확정 계약의 `JudgeInput`으로 변환한다 (`caption` 외 나머지 필드 대응).
2. `RuleEngine.judge(input, ruleRepository.findAll())`를 호출한다.
3. `RegenPromptBuilder.build(judged, realFirst, caption)`를 호출한다.
4. `JudgedFinding` 전체와 `RegenOutput`을 `Result`로 변환한다.

`CheckService`는 실물/AI 이미지 저장 → `extractImages(ai, real)` → `extractCaption` →
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
