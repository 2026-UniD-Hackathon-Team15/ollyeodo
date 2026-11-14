"use client";

/* eslint-disable @next/next/no-img-element */
// 미리보기는 사용자가 고른 파일(blob URL)을 바로 보여줘야 해서 next/image 대신 img를 쓴다.

import { useEffect, useRef, useState, type Ref, type RefObject } from "react";
import { cn } from "@/lib/utils";
import styles from "./new-check.module.css";

function formatSize(bytes: number) {
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)}MB`;
  return `${Math.round(bytes / 1024)}KB`;
}

/**
 * 사진 넣는 칸 — 실제 메뉴 사진·AI 메뉴 사진이 같은 모양을 쓴다.
 * 누르거나 끌어다 놓아 사진을 넣고, 넣은 뒤에는 미리보기와 「바꾸기」「삭제」를 보여 준다.
 */
export function PhotoDrop({
  ref,
  inputRef: outerInputRef,
  label,
  file,
  preview,
  onPick,
  onClear,
}: {
  ref?: Ref<HTMLDivElement>;
  /** 칸 밖 버튼(「최근 사진에서 고르기」 등)에서 파일 선택 창을 열 때 */
  inputRef?: RefObject<HTMLInputElement | null>;
  /** 칸 이름 (미리보기 왼쪽 위 표시·읽어 주기용) */
  label: string;
  file: File | null;
  preview: string;
  onPick: (file: File) => void;
  onClear: () => void;
}) {
  const innerInputRef = useRef<HTMLInputElement>(null);
  const inputRef = outerInputRef ?? innerInputRef;
  const dragDepth = useRef(0);
  const [dragOver, setDragOver] = useState(false);

  // 화면이 가려질 때 끌기 상태도 처음으로 (cacheComponents 가 상태째 숨겨 두기 때문)
  useEffect(() => {
    return () => {
      dragDepth.current = 0;
      setDragOver(false);
    };
  }, []);

  function openPicker() {
    inputRef.current?.click();
  }

  function onClick() {
    if (!file) openPicker();
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLDivElement>) {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      onClick();
    }
  }

  function onChange(e: React.ChangeEvent<HTMLInputElement>) {
    const picked = e.target.files?.[0];
    if (picked) onPick(picked);
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
    const dropped = e.dataTransfer.files[0];
    if (dropped) onPick(dropped);
  }

  return (
    <>
      <div
        ref={ref}
        className={cn(styles.drop, dragOver && styles.dropOver, file && styles.dropHasImage)}
        tabIndex={0}
        role="button"
        aria-label={`${label} 추가`}
        onClick={onClick}
        onKeyDown={onKeyDown}
        onDragEnter={onDragEnter}
        onDragOver={(e) => e.preventDefault()}
        onDragLeave={onDragLeave}
        onDrop={onDrop}
      >
        {!file && (
          <>
            <div className={styles.dropIcon}>
              <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
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

        {file && (
          <div className={styles.preview} key={preview}>
            <img src={preview} alt={`${label} 미리보기`} />
            <span className={styles.previewTag}>{label}</span>
            <div className={styles.previewBar}>
              <span className={styles.previewName}>{file.name}</span>
              <small className={styles.previewSize}>{formatSize(file.size)}</small>
              <button
                type="button"
                className={styles.previewBtn}
                onClick={(e) => {
                  e.stopPropagation();
                  openPicker();
                }}
              >
                바꾸기
              </button>
              <button
                type="button"
                className={styles.previewBtn}
                onClick={(e) => {
                  e.stopPropagation();
                  onClear();
                }}
              >
                삭제
              </button>
            </div>
          </div>
        )}
      </div>
      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png"
        className={styles.hiddenInput}
        onChange={onChange}
      />
    </>
  );
}
