# 팀 규칙

## 대회 요건 (어기면 감점·수상 제외)

- **커밋 이력 유지 필수**: force push, rebase로 이력 지우기, 레포 재생성 금지
- 연습용 test 레포 코드를 복사하지 않는다 (기존 코드와 80% 이상 유사 시 부정행위 검토)
- 작은 단위로 자주 커밋한다
- 11/15(일) **10:30 코드 프리즈** → 이후 버그 수정만

## 브랜치

```text
feat/#8-comparison-service ─┐
feat/#14-menu-form ─────────┼─► develop ──(통합 확인 후)──► main
fix/#9-asof-timezone ───────┘
```

- `main`: 최종 제출·배포용. **`develop`에서 통합 확인이 끝난 것만** PR로 합친다. 직접 push 금지
- `develop`: 통합 브랜치. 모든 작업 브랜치는 **`develop`에서 따고 `develop`으로 PR**한다. 직접 push 금지
- 작업 브랜치: `feat/#이슈번호-설명`, `fix/#이슈번호-설명`
  - 예: `feat/#8-comparison-service`
  - 시작: `git switch develop && git pull && git switch -c feat/#8-comparison-service`
- `develop → main` 머지: 체크포인트(`OLLYEODO_MVP.md` §12.1)마다 PM이 올린다. 11/15 10:30 코드 프리즈 직전에 마지막으로 올린다
- 제출용 zip은 `main`의 최종 커밋으로 만든다

## 커밋 메시지

```text
feat: 이미지 개수 비교 판정 추가 (#8)
fix: asOf 기본값을 Asia/Seoul로 수정 (#9)
docs: README 실행 방법 추가
test: ComparisonService 경계값 테스트 (#8)
```

## PR

- **base 브랜치는 `develop`** (`main`으로 PR 금지. `develop → main` 통합 PR만 예외)
- 본문에 `Closes #이슈번호`
- ⚖️ `legal` 라벨 이슈는 **판정 기준표(OLLYEODO_MVP.md §6)와 일치하는지** 리뷰어가 확인한 뒤 머지
- 리뷰어 1명 승인 후 머지 (Squash 금지 — 커밋 이력 유지를 위해 Merge commit 사용)

## 라벨

| 라벨 | 의미 |
|---|---|
| `backend` / `frontend` | 담당 영역 |
| `⚖️ legal` | 법령 근거 판정 규칙을 다루는 이슈. 임계값·근거 조항 임의 변경 금지 |
| `P0 필수` / `P1 권장` | 코드 프리즈 전 필수 / 시간 남으면 |
| `setup` | 프로젝트·인프라 세팅 |
| `blocked` | 선행 이슈 미완료 |
