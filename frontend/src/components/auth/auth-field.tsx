import type { InputHTMLAttributes } from "react";

export function AuthField({
  label,
  hint,
  ...input
}: InputHTMLAttributes<HTMLInputElement> & { label: string; hint?: string }) {
  return (
    <label className="flex flex-col gap-2.5">
      <span className="text-[15px] leading-[23px] font-medium text-sub">{label}</span>
      <input
        {...input}
        className="h-14 rounded-[14px] bg-surface px-[18px] text-[17px] leading-6 text-ink placeholder:text-faint outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-60"
      />
      {hint && <span className="-mt-1 text-[13px] leading-5 text-faint">{hint}</span>}
    </label>
  );
}

export function AuthCard({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex flex-1 justify-center bg-surface-subtle px-4 pt-[88px] pb-[120px]">
      <div className="h-fit w-full max-w-[528px] rounded-3xl border border-line bg-white px-6 py-12 sm:px-11">
        {children}
      </div>
    </div>
  );
}
