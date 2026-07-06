import React from "react";

/** Grouped container: surface-container-highest fill, radius 16, no shadow. */
export function Card({ children, variant = "filled", padding = "var(--card-pad)", onClick, style }) {
  const [hover, setHover] = React.useState(false);
  const interactive = Boolean(onClick);
  const base = variant === "outlined" ? "var(--color-surface)" : "var(--surface-card)";
  return (
    <div
      onClick={onClick}
      onMouseEnter={() => interactive && setHover(true)}
      onMouseLeave={() => setHover(false)}
      role={interactive ? "button" : undefined}
      tabIndex={interactive ? 0 : undefined}
      style={{
        background: hover ? `color-mix(in srgb, var(--color-on-surface) 8%, ${base})` : base,
        border: variant === "outlined" ? "1px solid var(--border-hairline)" : "none",
        borderRadius: "var(--radius-card)",
        padding,
        boxShadow: "none",
        cursor: interactive ? "pointer" : undefined,
        transition: "background var(--duration-quick) var(--ease-standard)",
        ...style,
      }}
    >
      {children}
    </div>
  );
}
