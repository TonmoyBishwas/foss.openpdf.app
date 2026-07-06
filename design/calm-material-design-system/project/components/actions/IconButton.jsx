import React from "react";
import { Icon } from "../icons/Icon.jsx";

/** Round 44px icon button — standard (bare), filled (primary) or tonal. */
export function IconButton({ icon, variant = "standard", selected = false, disabled = false, onClick, label, style }) {
  const [hover, setHover] = React.useState(false);
  const [press, setPress] = React.useState(false);
  const layer = press ? "12%" : hover ? "8%" : "0%";

  let background, color;
  if (disabled) {
    background = variant === "standard" ? "transparent" : "var(--disabled-fill)";
    color = "var(--disabled-text)";
  } else if (variant === "filled") {
    background = `color-mix(in srgb, var(--color-on-primary) ${layer}, var(--color-primary))`;
    color = "var(--color-on-primary)";
  } else if (variant === "tonal") {
    background = `color-mix(in srgb, var(--color-on-secondary-container) ${layer}, var(--color-secondary-container))`;
    color = "var(--color-on-secondary-container)";
  } else {
    background = `color-mix(in srgb, var(--color-on-surface) ${layer}, transparent)`;
    color = selected ? "var(--color-primary)" : "var(--color-on-surface)";
  }

  return (
    <button
      type="button"
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => { setHover(false); setPress(false); }}
      onMouseDown={() => setPress(true)}
      onMouseUp={() => setPress(false)}
      style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        width: "var(--tap-target)",
        height: "var(--tap-target)",
        border: "none",
        borderRadius: "var(--radius-pill)",
        background,
        color,
        cursor: disabled ? "default" : "pointer",
        transition: "background var(--duration-quick) var(--ease-standard)",
        ...style,
      }}
    >
      <Icon name={icon} fill={selected} />
    </button>
  );
}
