import React from "react";
import { Icon } from "../icons/Icon.jsx";

/** Bottom navigation — surface-container bar, active item gets a secondary-container pill. */
export function NavigationBar({ items = [], activeIndex = 0, onChange, style }) {
  return (
    <nav
      style={{
        display: "grid",
        gridTemplateColumns: `repeat(${items.length || 1}, 1fr)`,
        background: "var(--color-surface-container)",
        padding: "var(--space-12) 0 var(--space-16)",
        boxShadow: "none",
        ...style,
      }}
    >
      {items.map((item, i) => {
        const active = i === activeIndex;
        return (
          <button
            key={i}
            type="button"
            onClick={() => onChange && onChange(i)}
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: "var(--space-4)",
              border: "none",
              background: "transparent",
              cursor: "pointer",
              padding: 0,
              font: "var(--type-label)",
              fontSize: "12px",
            }}
          >
            <span
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                width: "64px",
                height: "32px",
                borderRadius: "var(--radius-pill)",
                background: active ? "var(--color-secondary-container)" : "transparent",
                color: active ? "var(--color-on-secondary-container)" : "var(--text-secondary)",
                transition: "background var(--duration-quick) var(--ease-standard)",
              }}
            >
              <Icon name={item.icon} fill={active} />
            </span>
            <span style={{ color: active ? "var(--text-primary)" : "var(--text-secondary)", fontWeight: active ? 600 : 500 }}>
              {item.label}
            </span>
          </button>
        );
      })}
    </nav>
  );
}
