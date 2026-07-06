import React from "react";
import { Icon } from "../icons/Icon.jsx";

/** Calm Material text field — radius 12, hairline outline, primary focus. */
export function TextField({ variant = "outlined", label, value, onChange, placeholder, supportingText, error = false, leadingIcon, type = "text", disabled = false, style }) {
  const [focus, setFocus] = React.useState(false);
  const borderColor = error
    ? "var(--color-error)"
    : focus
      ? "var(--color-primary)"
      : "var(--color-outline-variant)";

  return (
    <label style={{ display: "flex", flexDirection: "column", gap: "var(--space-4)", font: "var(--type-body-md)", opacity: disabled ? 0.5 : 1, ...style }}>
      {label ? (
        <span style={{ fontWeight: "var(--weight-label)", fontSize: "14px", color: focus && !error ? "var(--color-primary)" : "var(--text-secondary)" }}>
          {label}
        </span>
      ) : null}
      <span
        style={{
          display: "flex",
          alignItems: "center",
          gap: "var(--space-12)",
          padding: "var(--space-12) var(--space-16)",
          borderRadius: "var(--radius-control)",
          border: `1px solid ${variant === "outlined" || focus || error ? borderColor : "transparent"}`,
          boxShadow: focus ? `inset 0 0 0 1px ${borderColor}` : "none",
          background: variant === "filled" ? "var(--color-surface-container-high)" : "transparent",
          transition: "border-color var(--duration-quick) var(--ease-standard)",
        }}
      >
        {leadingIcon ? <Icon name={leadingIcon} color="var(--text-secondary)" /> : null}
        <input
          type={type}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          disabled={disabled}
          onFocus={() => setFocus(true)}
          onBlur={() => setFocus(false)}
          style={{
            flex: 1,
            minWidth: 0,
            border: "none",
            outline: "none",
            background: "transparent",
            font: "var(--type-body-lg)",
            color: "var(--text-primary)",
          }}
        />
      </span>
      {supportingText ? (
        <span style={{ font: "var(--type-caption)", color: error ? "var(--color-error)" : "var(--text-tertiary)", paddingLeft: "var(--space-16)" }}>
          {supportingText}
        </span>
      ) : null}
    </label>
  );
}
