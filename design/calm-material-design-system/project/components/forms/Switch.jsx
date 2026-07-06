import React from "react";

/** M3-style switch: 52×32 track, thumb grows 16→24 when on. Primary when on. */
export function Switch({ checked = false, onChange, disabled = false, label, style }) {
  const thumb = checked ? 24 : 16;
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled}
      onClick={() => onChange && onChange(!checked)}
      style={{
        position: "relative",
        width: "52px",
        height: "32px",
        borderRadius: "var(--radius-pill)",
        border: checked ? "2px solid transparent" : "2px solid var(--color-outline)",
        background: checked ? "var(--color-primary)" : "var(--color-surface-container-highest)",
        cursor: disabled ? "default" : "pointer",
        opacity: disabled ? 0.5 : 1,
        padding: 0,
        transition: "background var(--duration-quick) var(--ease-standard)",
        ...style,
      }}
    >
      <span
        style={{
          position: "absolute",
          top: "50%",
          left: checked ? `${48 - thumb - 4}px` : "6px",
          transform: "translateY(-50%)",
          width: `${thumb}px`,
          height: `${thumb}px`,
          borderRadius: "var(--radius-pill)",
          background: checked ? "var(--color-on-primary)" : "var(--color-outline)",
          transition: "all var(--duration-quick) var(--ease-standard)",
        }}
      ></span>
    </button>
  );
}
