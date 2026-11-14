"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { ApiError } from "@/lib/api/client";
import { useCreateCheck } from "@/lib/api/queries";
import { cn } from "@/lib/utils";
import { AnalyzingView } from "./analyzing-view";
import { PhotoDrop } from "./photo-drop";
import styles from "./new-check.module.css";

const MAX_SIZE = 10 * 1024 * 1024;
const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const CAPTION_MAX = 300;

/** 등장 순서: 0.1초부터 0.06초 간격 */
function delay(index: number) {
  return { animationDelay: `${0.1 + index * 0.06}s` };
}

function shake(el: HTMLElement | null) {
  if (!el) return;
  el.classList.remove(styles.invalid);
  void el.offsetWidth;
  el.classList.add(styles.invalid);
  setTimeout(() => el.classList.remove(styles.invalid), 1200);
}

function isValidImage(file: File) {
  // 백엔드가 받는 형식과 같게 (CheckService: JPEG·PNG·WEBP)
  if (!IMAGE_TYPES.includes(file.type)) {
    toast.error("JPG, PNG, WEBP 사진만 올릴 수 있어요");
    return false;
  }
  if (file.size > MAX_SIZE) {
    toast.error("10MB 이하 사진만 올릴 수 있어요");
    return false;
  }
  return true;
}

/**
 * 기준일 — 데모용 숨은 옵션. `/checks/new?asOf=2026-11-26` 처럼 주소에 붙이면 그날 시행 규칙으로 판정한다
 * (§1.2·§5.1 AI_VIRTUAL_EXPERT 11/26 🟢 → 11/27 🔴). 없으면 서버가 오늘로 처리한다.
 */
function asOfFromUrl() {
  const value = new URLSearchParams(window.location.search).get("asOf");
  return value && /^\d{4}-\d{2}-\d{2}$/.test(value) ? value : undefined;
}

export function NewCheckForm() {
  const router = useRouter();
  const createCheck = useCreateCheck();
  const [loading, setLoading] = useState(false);

  const [menuName, setMenuName] = useState("");
  const [realFile, setRealFile] = useState<File | null>(null);
  const [realPreview, setRealPreview] = useState("");
  const [aiFile, setAiFile] = useState<File | null>(null);
  const [aiPreview, setAiPreview] = useState("");
  const [caption, setCaption] = useState("");

  const nameFieldRef = useRef<HTMLDivElement>(null);
  const nameInputRef = useRef<HTMLInputElement>(null);
  const realRowRef = useRef<HTMLDivElement>(null);
  const dropRef = useRef<HTMLDivElement>(null);
  const areaRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const realInputRef = useRef<HTMLInputElement>(null);
  /** 이 화면이 지금 보이는지 — 결과를 기다리는 동안 떠나면 false */
  const activeRef = useRef(false);

  useEffect(() => {
    const el = textareaRef.current;
    if (el) {
      el.style.height = "auto";
      el.style.height = `${el.scrollHeight}px`;
    }
  }, [caption]);

  useEffect(() => {
    return () => {
      if (aiPreview) URL.revokeObjectURL(aiPreview);
    };
  }, [aiPreview]);

  useEffect(() => {
    return () => {
      if (realPreview) URL.revokeObjectURL(realPreview);
    };
  }, [realPreview]);

  /**
   * 결과 화면으로 넘어가 이 화면이 가려질 때 처음 상태로 되돌린다.
   * cacheComponents 를 켜 두면 Next.js 가 떠난 화면을 지우지 않고 상태째 숨겨 두기 때문에,
   * 되돌리지 않으면 「다시 검수」로 돌아왔을 때 분석 중 화면이 그대로 남는다.
   * (가려질 때 위 정리 코드가 사진 미리보기 주소도 해제하므로 사진도 함께 비운다)
   */
  useEffect(() => {
    activeRef.current = true;
    return () => {
      activeRef.current = false;
      setLoading(false);
      setMenuName("");
      setRealFile(null);
      setRealPreview("");
      setAiFile(null);
      setAiPreview("");
      setCaption("");
    };
  }, []);

  function setRealPhoto(file: File) {
    if (!isValidImage(file)) return;
    setRealFile(file);
    setRealPreview(URL.createObjectURL(file));
  }

  function clearRealPhoto() {
    setRealFile(null);
    setRealPreview("");
  }

  function setAiPhoto(file: File) {
    if (!isValidImage(file)) return;
    setAiFile(file);
    setAiPreview(URL.createObjectURL(file));
  }

  function clearAiPhoto() {
    setAiFile(null);
    setAiPreview("");
  }

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (loading) return;

    const name = menuName.trim();
    if (name === "") {
      shake(nameFieldRef.current);
      nameInputRef.current?.focus();
      toast.error("메뉴 이름을 적어 주세요");
      return;
    }
    if (!realFile) {
      shake(realRowRef.current);
      toast.error("실제 메뉴 사진을 넣어 주세요");
      return;
    }
    if (!aiFile) {
      shake(dropRef.current);
      dropRef.current?.scrollIntoView({ behavior: "smooth", block: "center" });
      toast.error("AI 메뉴 사진을 넣어 주세요");
      return;
    }
    if (caption.trim() === "") {
      shake(areaRef.current);
      textareaRef.current?.focus();
      toast.error("메뉴 설명을 넣어 주세요");
      return;
    }

    setLoading(true);
    try {
      // 실제 사진·AI 사진·설명을 한 번에 보낸다 (POST /api/checks, 메뉴 등록 API는 없음)
      const result = await createCheck.mutateAsync({
        menuName: name,
        realImage: realFile,
        aiImage: aiFile,
        caption: caption.trim(),
        asOf: asOfFromUrl(),
      });
      // 기다리는 동안 다른 화면으로 떠났으면 결과 화면으로 끌고 가지 않는다
      if (activeRef.current) router.push(`/checks/${result.checkId}`);
    } catch (error) {
      if (!activeRef.current) return;
      toast.error(error instanceof ApiError ? error.message : "검수하지 못했어요. 잠시 뒤 다시 시도해 주세요");
      setLoading(false);
    }
  }

  if (loading) return <AnalyzingView image={aiPreview} />;

  return (
    <div className={styles.page}>
      <div className={styles.col}>
        <form onSubmit={onSubmit} noValidate>
          <h1 className={cn(styles.title, styles.reveal)} style={delay(0)}>
            검수할 메뉴를 넣어 주세요
          </h1>
          <p className={cn(styles.lead, styles.reveal)} style={delay(1)}>
            직접 찍은 실제 메뉴 사진과 AI 메뉴 사진·설명을 대조해 드려요.
          </p>

          <div className={cn(styles.group, styles.groupFirst, styles.reveal)} style={delay(2)}>
            <label className={styles.label} htmlFor="menuName">
              메뉴 이름
            </label>
            <div className={styles.field} ref={nameFieldRef}>
              <input
                id="menuName"
                ref={nameInputRef}
                type="text"
                value={menuName}
                placeholder="예: 딸기 크레이프 케이크"
                onChange={(e) => setMenuName(e.target.value)}
              />
            </div>
          </div>

          <div className={cn(styles.group, styles.reveal)} style={delay(3)}>
            <div className={styles.label}>
              실제 메뉴 사진 <small>직접 찍은 사진</small>
              {/* 피그마 「최근 사진에서 고르기」 — 휴대폰에서는 파일 선택 창이 최근 사진(사진 보관함)을 먼저 보여 준다 */}
              <button
                type="button"
                className={cn(styles.changeBtn, styles.labelAction)}
                onClick={() => realInputRef.current?.click()}
              >
                최근 사진에서 고르기
              </button>
            </div>
            <PhotoDrop
              ref={realRowRef}
              inputRef={realInputRef}
              label="실제 메뉴 사진"
              file={realFile}
              preview={realPreview}
              onPick={setRealPhoto}
              onClear={clearRealPhoto}
            />
            <p className={styles.hint}>
              AI 메뉴 사진과 비슷한 각도에서 찍어 주세요. 더 정확하게 비교할 수 있어요.
            </p>
          </div>

          <div className={cn(styles.group, styles.reveal)} style={delay(4)}>
            <div className={styles.label}>
              AI 메뉴 사진 <small>AI로 만들거나 보정한 메뉴 사진</small>
            </div>
            <PhotoDrop
              ref={dropRef}
              label="AI 메뉴 사진"
              file={aiFile}
              preview={aiPreview}
              onPick={setAiPhoto}
              onClear={clearAiPhoto}
            />
          </div>

          <div className={cn(styles.group, styles.reveal)} style={delay(5)}>
            <label className={styles.label} htmlFor="caption">
              메뉴 설명 <small>AI 메뉴 사진과 함께 쓸 메뉴 설명</small>
            </label>
            <div className={styles.area} ref={areaRef}>
              <textarea
                id="caption"
                ref={textareaRef}
                rows={2}
                maxLength={CAPTION_MAX}
                value={caption}
                placeholder="메뉴 설명을 그대로 붙여 넣어 주세요"
                onChange={(e) => setCaption(e.target.value)}
              />
              <div className={styles.count}>
                {caption.length}/{CAPTION_MAX}자
              </div>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className={cn(styles.submit, styles.reveal)}
            style={delay(6)}
          >
            {loading && <span className={styles.fill} />}
            {loading && <span className={styles.spinner} />}
            <span>{loading ? "분석하는 중이에요" : "검수하기"}</span>
          </button>
        </form>
      </div>

    </div>
  );
}
