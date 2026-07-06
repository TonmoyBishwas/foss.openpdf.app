import React from "react";
import { Button } from "../actions/Button.jsx";

/** Modal dialog — radius 16, surface-container-high, flat; quiet fade/scale in. */
export function Dialog({ open, onClose, title, children, actions, style }) {
  if (!open) return null;
  return (
    <div
      onClick={onClose}
      style={{
        position: "absolute",
        inset: 0,
        background: "rgba(0,0,0,0.32)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: "var(--space-24)",
        zIndex: 100,
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        style={{
          background: "var(--color-surface-container-high)",
          borderRadius: "var(--radius-card)",
          padding: "var(--space-24)",
          minWidth: "280px",
          maxWidth: "400px",
          boxShadow: "none",
          display: "flex",
          flexDirection: "column",
          gap: "var(--space-16)",
          ...style,
        }}
      >
        {title ? <h2 style={{ font: "var(--type-title)", color: "var(--text-primary)", margin: 0 }}>{title}</h2> : null}
        <div style={{ font: "var(--type-body-md)", color: "var(--text-secondary)" }}>{children}</div>
        {actions ? (
          <div style={{ display: "flex", justifyContent: "flex-end", gap: "var(--space-8)" }}>
            {actions.map((a, i) => (
              <Button key={i} variant="text" onClick={a.onClick} style={{ padding: "var(--space-8) var(--space-12)" }}>
                {a.label}
              </Button>
            ))}
          </div>
        ) : null}
      </div>
    </div>
  );
}
