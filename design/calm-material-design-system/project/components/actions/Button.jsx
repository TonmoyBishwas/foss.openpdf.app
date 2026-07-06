import React from "react";
import { Icon } from "../icons/Icon.jsx";

/** Calm Material button — flat, radius 12, label 16/500, elevation 0. */
export function Button({ variant = "filled", children, icon, disabled = false, onClick, type = "button", style }) {
  const [hover, setHover] = React.useState(false);
  const [press, setPress] = React.useState(false);

  const layer = press ? "12%" : hover ? "8%" : "0%";
  let background, color;
  if (disabled) {
    background = variant === "text" ? "transparent" : "var(--disabled-fill)";
    color = "var(--disabled-text)";
  } else if (variant === "filled") {
    background = `color-mix(in srgb, var(--color-on-primary) ${layer}, var(--color-primary))`;
    color = "var(--color-on-primary)";
  } else if (variant === "tonal") {
    background = `color-mix(in srgb, var(--color-on-secondary-container) ${layer}, var(--color-secondary-container))`;
    color = "var(--color-on-secondary-container)";
  } else {
    background = `color-mix(in srgb, var(--color-primary) ${layer}, transparent)`;
    color = "var(--color-primary)";
  }

  return (
    <button
      type={type}
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
        gap: "var(--space-8)",
        padding: "var(--button-pad-y) var(--button-pad-x)",
        border: "none",
        borderRadius: "var(--radius-control)",
        font: "var(--type-label)",
        background,
        color,
        cursor: disabled ? "default" : "pointer",
        boxShadow: "none",
        transition: "background var(--duration-quick) var(--ease-standard)",
        ...style,
      }}
    >
      {icon ? <Icon name={icon} size={20} /> : null}
      {children}
    </button>
  );
}
