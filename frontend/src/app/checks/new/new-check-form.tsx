"use client";

/* eslint-disable @next/next/no-img-element */
// 미리보기는 사용자가 고른 파일(blob URL)을 바로 보여줘야 해서 next/image 대신 img를 쓴다.

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { useCreateCheck, useMenus } from "@/lib/api/queries";
import { cn } from "@/lib/utils";
import styles from "./new-check.module.css";

const MAX_SIZE = 10 * 1024 * 1024;
const CAPTION_MAX = 300;

function formatSize(bytes: number) {
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)}MB`;
  return `${Math.round(bytes / 1024)}KB`;
}

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

export function NewCheckForm() {
  const router = useRouter();
  const menusQuery = useMenus();
  const createCheck = useCreateCheck();
  const loading = createCheck.isPending;

  const [menuId, setMenuId] = useState<number | null>(null);
  const [aiFile, setAiFile] = useState<File | null>(null);
  const [aiPreview, setAiPreview] = useState("");
  const [dragOver, setDragOver] = useState(false);
  const [caption, setCaption] = useState("");

  const menuFieldRef = useRef<HTMLDivElement>(null);
  const dropRef = useRef<HTMLDivElement>(null);
  const areaRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const aiInputRef = useRef<HTMLInputElement>(null);
  const dragDepth = useRef(0);

  const menus = menusQuery.data ?? [];
  const selectedMenu = menus.find((m) => m.id === (menuId ?? menus[0]?.id));

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

  function setAiPhoto(file: File) {
    if (file.type !== "image/jpeg" && file.type !== "image/png") {
      toast.error("JPG, PNG 사진만 올릴 수 있어요");
      return;
    }
    if (file.size > MAX_SIZE) {
      toast.error("10MB 이하 사진만 올릴 수 있어요");
      return;
    }
    setAiFile(file);
    setAiPreview(URL.createObjectURL(file));
  }

  function clearAiPhoto() {
    setAiFile(null);
    setAiPreview("");
  }

  function openAiPicker() {
    aiInputRef.current?.click();
  }

  function onDropClick() {
    if (!aiFile) openAiPicker();
  }

  function onDropKeyDown(e: React.KeyboardEvent<HTMLDivElement>) {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      onDropClick();
    }
  }

  function onAiFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) setAiPhoto(file);
    e.target.value = "";
  }

  function onDragEnter(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    dragDepth.current += 1;
    setDragOver(true);
  }

  function onDragLeave() {
    dragDepth.current = Math.max(0, dragDepth.current - 1);
    if (dragDepth.current === 0) setDragOver(false);
  }

  function onDrop(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    dragDepth.current = 0;
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) setAiPhoto(file);
  }

  function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (loading) return;

    if (!selectedMenu) {
      shake(menuFieldRef.current);
      toast.error("검수할 메뉴를 먼저 골라 주세요");
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

    createCheck.mutate(
      { menuId: selectedMenu.id, aiImage: aiFile, caption: caption.trim() },
      {
        onSuccess: (result) => router.push(`/checks/${result.checkId}`),
        onError: () => toast.error("검수하지 못했어요. 잠시 뒤 다시 시도해 주세요"),
      },
    );
  }

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
            <label className={styles.label} htmlFor="menu">
              메뉴 <small>메뉴 등록에서 실제 사진을 올린 메뉴</small>
            </label>
            {menusQuery.isPending && <div className={styles.emptyMenus}>메뉴를 불러오는 중이에요</div>}
            {menusQuery.isError && (
              <div className={styles.emptyMenus}>메뉴를 불러오지 못했어요. 새로고침해 주세요.</div>
            )}
            {menusQuery.isSuccess && menus.length === 0 && (
              <div className={styles.emptyMenus}>
                등록된 메뉴가 없어요. <Link href="/menus/new">메뉴 등록하러 가기</Link>
              </div>
            )}
            {menus.length > 0 && (
              <div className={styles.field} ref={menuFieldRef}>
                <select
                  id="menu"
                  value={selectedMenu?.id ?? ""}
                  onChange={(e) => setMenuId(Number(e.target.value))}
                >
                  {menus.map((menu) => (
                    <option key={menu.id} value={menu.id}>
                      {menu.name}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          {selectedMenu && (
            <div className={cn(styles.group, styles.reveal)} style={delay(3)}>
              <div className={styles.label}>
                실제 메뉴 사진 <small>메뉴 등록 때 올린 사진</small>
              </div>
              <div className={styles.photo}>
                <div className={styles.thumb}>
                  {selectedMenu.realImageUrl && (
                    <img src={selectedMenu.realImageUrl} alt={`${selectedMenu.name} 실제 사진`} />
                  )}
                </div>
                <div className={styles.meta}>
                  <span className={styles.fileName}>{selectedMenu.name}</span>
                </div>
              </div>
            </div>
          )}

          <div className={cn(styles.group, styles.reveal)} style={delay(4)}>
            <div className={styles.label}>
              AI 메뉴 사진 <small>AI로 만들거나 보정한 메뉴 사진</small>
            </div>
            <div
              ref={dropRef}
              className={cn(styles.drop, dragOver && styles.dropOver, aiFile && styles.dropHasImage)}
              tabIndex={0}
              role="button"
              aria-label="AI 메뉴 사진 추가"
              onClick={onDropClick}
              onKeyDown={onDropKeyDown}
              onDragEnter={onDragEnter}
              onDragOver={(e) => e.preventDefault()}
              onDragLeave={onDragLeave}
              onDrop={onDrop}
            >
              {!aiFile && (
                <>
                  <div className={styles.dropIcon}>
                    <svg
                      width="24"
                      height="24"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="#087a5e"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <rect width="18" height="18" x="3" y="3" rx="2" />
                      <circle cx="9" cy="9" r="2" />
                      <path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21" />
                    </svg>
                  </div>
                  <div className={styles.dropTitle}>{dragOver ? "여기에 놓아 주세요" : "이미지 추가"}</div>
                  <div className={styles.dropSub}>끌어다 놓아도 돼요 · JPG, PNG 10MB까지</div>
                </>
              )}

              {aiFile && (
                <div className={styles.preview} key={aiPreview}>
                  <img src={aiPreview} alt="AI 메뉴 사진 미리보기" />
                  <span className={styles.previewTag}>AI 메뉴 사진</span>
                  <div className={styles.previewBar}>
                    <span className={styles.previewName}>{aiFile.name}</span>
                    <small className={styles.previewSize}>{formatSize(aiFile.size)}</small>
                    <button
                      type="button"
                      className={styles.previewBtn}
                      onClick={(e) => {
                        e.stopPropagation();
                        openAiPicker();
                      }}
                    >
                      바꾸기
                    </button>
                    <button
                      type="button"
                      className={styles.previewBtn}
                      onClick={(e) => {
                        e.stopPropagation();
                        clearAiPhoto();
                      }}
                    >
                      삭제
                    </button>
                  </div>
                </div>
              )}
            </div>
            <input
              ref={aiInputRef}
              type="file"
              accept="image/jpeg,image/png"
              className={styles.hiddenInput}
              onChange={onAiFileChange}
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
