import { createClient } from "@supabase/supabase-js";
import { env } from "@/lib/env";

/** 환경 변수가 없으면 null — 목업 모드나 설정 전에도 화면이 깨지지 않게 한다 */
export const supabase =
  env.supabaseUrl && env.supabaseAnonKey
    ? createClient(env.supabaseUrl, env.supabaseAnonKey)
    : null;
