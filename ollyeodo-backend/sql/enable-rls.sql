-- 3개 테이블 RLS 켜기 (정책은 만들지 않음 → anon 키로는 조회 불가, Spring(postgres 계정)만 접근)
-- ddl-auto: update로 테이블 생성 직후 Supabase SQL Editor에서 실행
-- 스코프 변경(#33)으로 menus 테이블은 없음
alter table public.checks   enable row level security;
alter table public.findings enable row level security;
alter table public.rules    enable row level security;