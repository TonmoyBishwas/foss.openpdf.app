import React from "react";
import { Icon } from "../icons/Icon.jsx";

/** List row: leading icon, title, optional subtitle (60%), trailing slot. */
export function ListTile({ leading, title, subtitle, trailing, onClick, style }) {
  const [hover, setHover] = React.useState(false);
  const interactive = Boolean(onClick);
  return (
    <div
      onClick={onClick}
      onMouseEnter={() => interactive && setHover(true)}
      onMouseLeave={() => setHover(false)}
      role={interactive ? "button" : undefined}
      tabIndex={interactive ? 0 : undefined}
      style={{
        display: "flex",
        alignItems: "center",
        gap: "var(--space-16)",
        padding: "var(--space-12) var(--space-16)",
        minHeight: "var(--tap-target)",
        borderRadius: "var(--radius-control)",
        background: hover ? "var(--state-hover)" : "transparent",
        cursor: interactive ? "pointer" : undefined,
        transition: "background var(--duration-quick) var(--ease-standard)",
        ...style,
      }}
    >
      {typeof leading === "string" ? <Icon name={leading} color="var(--color-on-surface)" /> : leading}
      <div style={{ flex: 1, minWidth: 0, display: "flex", flexDirection: "column", gap: "2px" }}>
        <span style={{ font: "var(--type-body-lg)", lineHeight: 1.3, color: "var(--text-primary)" }}>{title}</span>
        {subtitle ? <span style={{ font: "var(--type-body-md)", lineHeight: 1.35, color: "var(--text-secondary)" }}>{subtitle}</span> : null}
      </div>
      {typeof trailing === "string" ? <Icon name={trailing} color="var(--text-tertiary)" /> : trailing}
    </div>
  );
}
