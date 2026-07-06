import React from "react";
import { Icon } from "../icons/Icon.jsx";

/** Filter/assist chip — 32px tall, radius 12, hairline outline; selected = secondary-container. */
export function Chip({ children, icon, selected = false, disabled = false, onClick, style }) {
  const [hover, setHover] = React.useState(false);
  const layer = hover && !disabled ? "8%" : "0%";
  const background = selected
    ? `color-mix(in srgb, var(--color-on-secondary-container) ${layer}, var(--color-secondary-container))`
    : `color-mix(in srgb, var(--color-on-surface) ${layer}, transparent)`;

  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: "var(--space-8)",
        height: "32px",
        padding: "0 var(--space-16)",
        borderRadius: "var(--radius-control)",
        border: selected ? "1px solid transparent" : "1px solid var(--color-outline-variant)",
        background,
        color: disabled ? "var(--disabled-text)" : selected ? "var(--color-on-secondary-container)" : "var(--color-on-surface)",
        font: "var(--type-label)",
        fontSize: "14px",
        cursor: disabled ? "default" : "pointer",
        transition: "background var(--duration-quick) var(--ease-standard)",
        ...style,
      }}
    >
      {selected ? <Icon name="check" size={18} /> : icon ? <Icon name={icon} size={18} /> : null}
      {children}
    </button>
  );
}
