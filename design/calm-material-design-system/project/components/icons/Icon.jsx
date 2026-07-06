import React from "react";

/** Material Symbols Rounded glyph. Requires tokens/icons.css (loaded via styles.css). */
export function Icon({ name, size = 24, fill = false, color, style }) {
  return (
    <span
      className={"cm-icon" + (fill ? " cm-icon--fill" : "")}
      aria-hidden="true"
      style={{ fontSize: size, color: color, ...style }}
    >
      {name}
    </span>
  );
}
