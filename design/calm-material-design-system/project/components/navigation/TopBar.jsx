import React from "react";
import { IconButton } from "../actions/IconButton.jsx";

/** Top app bar — surface background (blends into page), elevation 0, left title 18/600. */
export function TopBar({ title, leading, onLeading, actions = [], style }) {
  return (
    <header
      style={{
        display: "flex",
        alignItems: "center",
        gap: "var(--space-8)",
        height: "64px",
        padding: "0 var(--space-8)",
        background: "var(--color-surface)",
        color: "var(--color-on-surface)",
        boxShadow: "none",
        ...style,
      }}
    >
      {leading ? <IconButton icon={leading} label="Navigation" onClick={onLeading} /> : null}
      <h1 style={{ flex: 1, font: "var(--type-title)", margin: 0, paddingLeft: leading ? 0 : "var(--space-8)", color: "var(--text-primary)" }}>
        {title}
      </h1>
      {actions.map((a, i) => (
        <IconButton key={i} icon={a.icon} label={a.label} onClick={a.onClick} />
      ))}
    </header>
  );
}
