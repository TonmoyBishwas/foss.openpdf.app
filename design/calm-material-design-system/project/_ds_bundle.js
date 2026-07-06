/* @ds-bundle: {"format":4,"namespace":"CalmMaterialDesignSystem_a3bee2","components":[{"name":"Button","sourcePath":"components/actions/Button.jsx"},{"name":"IconButton","sourcePath":"components/actions/IconButton.jsx"},{"name":"Card","sourcePath":"components/containment/Card.jsx"},{"name":"Dialog","sourcePath":"components/containment/Dialog.jsx"},{"name":"ListTile","sourcePath":"components/containment/ListTile.jsx"},{"name":"Chip","sourcePath":"components/forms/Chip.jsx"},{"name":"Switch","sourcePath":"components/forms/Switch.jsx"},{"name":"TextField","sourcePath":"components/forms/TextField.jsx"},{"name":"Icon","sourcePath":"components/icons/Icon.jsx"},{"name":"NavigationBar","sourcePath":"components/navigation/NavigationBar.jsx"},{"name":"TopBar","sourcePath":"components/navigation/TopBar.jsx"}],"sourceHashes":{"components/actions/Button.jsx":"b516055bd1fd","components/actions/IconButton.jsx":"b64cbbc9d3c7","components/containment/Card.jsx":"a0351b081aed","components/containment/Dialog.jsx":"ce8b92927318","components/containment/ListTile.jsx":"9225f018c696","components/forms/Chip.jsx":"6bf5b6e34ee8","components/forms/Switch.jsx":"24d24451686c","components/forms/TextField.jsx":"f0f2d8c5a2b8","components/icons/Icon.jsx":"1e63e4859e26","components/navigation/NavigationBar.jsx":"b9c88ac7db93","components/navigation/TopBar.jsx":"a0686a52b552","ui_kits/lensbite/Screens.jsx":"bd686fce1c59"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.CalmMaterialDesignSystem_a3bee2 = window.CalmMaterialDesignSystem_a3bee2 || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/containment/Card.jsx
try { (() => {
/** Grouped container: surface-container-highest fill, radius 16, no shadow. */
function Card({
  children,
  variant = "filled",
  padding = "var(--card-pad)",
  onClick,
  style
}) {
  const [hover, setHover] = React.useState(false);
  const interactive = Boolean(onClick);
  const base = variant === "outlined" ? "var(--color-surface)" : "var(--surface-card)";
  return /*#__PURE__*/React.createElement("div", {
    onClick: onClick,
    onMouseEnter: () => interactive && setHover(true),
    onMouseLeave: () => setHover(false),
    role: interactive ? "button" : undefined,
    tabIndex: interactive ? 0 : undefined,
    style: {
      background: hover ? `color-mix(in srgb, var(--color-on-surface) 8%, ${base})` : base,
      border: variant === "outlined" ? "1px solid var(--border-hairline)" : "none",
      borderRadius: "var(--radius-card)",
      padding,
      boxShadow: "none",
      cursor: interactive ? "pointer" : undefined,
      transition: "background var(--duration-quick) var(--ease-standard)",
      ...style
    }
  }, children);
}
Object.assign(__ds_scope, { Card });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/containment/Card.jsx", error: String((e && e.message) || e) }); }

// components/forms/Switch.jsx
try { (() => {
/** M3-style switch: 52×32 track, thumb grows 16→24 when on. Primary when on. */
function Switch({
  checked = false,
  onChange,
  disabled = false,
  label,
  style
}) {
  const thumb = checked ? 24 : 16;
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    role: "switch",
    "aria-checked": checked,
    "aria-label": label,
    disabled: disabled,
    onClick: () => onChange && onChange(!checked),
    style: {
      position: "relative",
      width: "52px",
      height: "32px",
      borderRadius: "var(--radius-pill)",
      border: checked ? "2px solid transparent" : "2px solid var(--color-outline)",
      background: checked ? "var(--color-primary)" : "var(--color-surface-container-highest)",
      cursor: disabled ? "default" : "pointer",
      opacity: disabled ? 0.5 : 1,
      padding: 0,
      transition: "background var(--duration-quick) var(--ease-standard)",
      ...style
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      top: "50%",
      left: checked ? `${48 - thumb - 4}px` : "6px",
      transform: "translateY(-50%)",
      width: `${thumb}px`,
      height: `${thumb}px`,
      borderRadius: "var(--radius-pill)",
      background: checked ? "var(--color-on-primary)" : "var(--color-outline)",
      transition: "all var(--duration-quick) var(--ease-standard)"
    }
  }));
}
Object.assign(__ds_scope, { Switch });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Switch.jsx", error: String((e && e.message) || e) }); }

// components/icons/Icon.jsx
try { (() => {
/** Material Symbols Rounded glyph. Requires tokens/icons.css (loaded via styles.css). */
function Icon({
  name,
  size = 24,
  fill = false,
  color,
  style
}) {
  return /*#__PURE__*/React.createElement("span", {
    className: "cm-icon" + (fill ? " cm-icon--fill" : ""),
    "aria-hidden": "true",
    style: {
      fontSize: size,
      color: color,
      ...style
    }
  }, name);
}
Object.assign(__ds_scope, { Icon });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/icons/Icon.jsx", error: String((e && e.message) || e) }); }

// components/actions/Button.jsx
try { (() => {
/** Calm Material button — flat, radius 12, label 16/500, elevation 0. */
function Button({
  variant = "filled",
  children,
  icon,
  disabled = false,
  onClick,
  type = "button",
  style
}) {
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
  return /*#__PURE__*/React.createElement("button", {
    type: type,
    disabled: disabled,
    onClick: onClick,
    onMouseEnter: () => setHover(true),
    onMouseLeave: () => {
      setHover(false);
      setPress(false);
    },
    onMouseDown: () => setPress(true),
    onMouseUp: () => setPress(false),
    style: {
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
      ...style
    }
  }, icon ? /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 20
  }) : null, children);
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/actions/Button.jsx", error: String((e && e.message) || e) }); }

// components/actions/IconButton.jsx
try { (() => {
/** Round 44px icon button — standard (bare), filled (primary) or tonal. */
function IconButton({
  icon,
  variant = "standard",
  selected = false,
  disabled = false,
  onClick,
  label,
  style
}) {
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
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": label,
    disabled: disabled,
    onClick: onClick,
    onMouseEnter: () => setHover(true),
    onMouseLeave: () => {
      setHover(false);
      setPress(false);
    },
    onMouseDown: () => setPress(true),
    onMouseUp: () => setPress(false),
    style: {
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
      ...style
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    fill: selected
  }));
}
Object.assign(__ds_scope, { IconButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/actions/IconButton.jsx", error: String((e && e.message) || e) }); }

// components/containment/Dialog.jsx
try { (() => {
/** Modal dialog — radius 16, surface-container-high, flat; quiet fade/scale in. */
function Dialog({
  open,
  onClose,
  title,
  children,
  actions,
  style
}) {
  if (!open) return null;
  return /*#__PURE__*/React.createElement("div", {
    onClick: onClose,
    style: {
      position: "absolute",
      inset: 0,
      background: "rgba(0,0,0,0.32)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      padding: "var(--space-24)",
      zIndex: 100
    }
  }, /*#__PURE__*/React.createElement("div", {
    onClick: e => e.stopPropagation(),
    role: "dialog",
    "aria-modal": "true",
    style: {
      background: "var(--color-surface-container-high)",
      borderRadius: "var(--radius-card)",
      padding: "var(--space-24)",
      minWidth: "280px",
      maxWidth: "400px",
      boxShadow: "none",
      display: "flex",
      flexDirection: "column",
      gap: "var(--space-16)",
      ...style
    }
  }, title ? /*#__PURE__*/React.createElement("h2", {
    style: {
      font: "var(--type-title)",
      color: "var(--text-primary)",
      margin: 0
    }
  }, title) : null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--type-body-md)",
      color: "var(--text-secondary)"
    }
  }, children), actions ? /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "flex-end",
      gap: "var(--space-8)"
    }
  }, actions.map((a, i) => /*#__PURE__*/React.createElement(__ds_scope.Button, {
    key: i,
    variant: "text",
    onClick: a.onClick,
    style: {
      padding: "var(--space-8) var(--space-12)"
    }
  }, a.label))) : null));
}
Object.assign(__ds_scope, { Dialog });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/containment/Dialog.jsx", error: String((e && e.message) || e) }); }

// components/containment/ListTile.jsx
try { (() => {
/** List row: leading icon, title, optional subtitle (60%), trailing slot. */
function ListTile({
  leading,
  title,
  subtitle,
  trailing,
  onClick,
  style
}) {
  const [hover, setHover] = React.useState(false);
  const interactive = Boolean(onClick);
  return /*#__PURE__*/React.createElement("div", {
    onClick: onClick,
    onMouseEnter: () => interactive && setHover(true),
    onMouseLeave: () => setHover(false),
    role: interactive ? "button" : undefined,
    tabIndex: interactive ? 0 : undefined,
    style: {
      display: "flex",
      alignItems: "center",
      gap: "var(--space-16)",
      padding: "var(--space-12) var(--space-16)",
      minHeight: "var(--tap-target)",
      borderRadius: "var(--radius-control)",
      background: hover ? "var(--state-hover)" : "transparent",
      cursor: interactive ? "pointer" : undefined,
      transition: "background var(--duration-quick) var(--ease-standard)",
      ...style
    }
  }, typeof leading === "string" ? /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: leading,
    color: "var(--color-on-surface)"
  }) : leading, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0,
      display: "flex",
      flexDirection: "column",
      gap: "2px"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--type-body-lg)",
      lineHeight: 1.3,
      color: "var(--text-primary)"
    }
  }, title), subtitle ? /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--type-body-md)",
      lineHeight: 1.35,
      color: "var(--text-secondary)"
    }
  }, subtitle) : null), typeof trailing === "string" ? /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: trailing,
    color: "var(--text-tertiary)"
  }) : trailing);
}
Object.assign(__ds_scope, { ListTile });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/containment/ListTile.jsx", error: String((e && e.message) || e) }); }

// components/forms/Chip.jsx
try { (() => {
/** Filter/assist chip — 32px tall, radius 12, hairline outline; selected = secondary-container. */
function Chip({
  children,
  icon,
  selected = false,
  disabled = false,
  onClick,
  style
}) {
  const [hover, setHover] = React.useState(false);
  const layer = hover && !disabled ? "8%" : "0%";
  const background = selected ? `color-mix(in srgb, var(--color-on-secondary-container) ${layer}, var(--color-secondary-container))` : `color-mix(in srgb, var(--color-on-surface) ${layer}, transparent)`;
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    disabled: disabled,
    onClick: onClick,
    onMouseEnter: () => setHover(true),
    onMouseLeave: () => setHover(false),
    style: {
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
      ...style
    }
  }, selected ? /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "check",
    size: 18
  }) : icon ? /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 18
  }) : null, children);
}
Object.assign(__ds_scope, { Chip });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Chip.jsx", error: String((e && e.message) || e) }); }

// components/forms/TextField.jsx
try { (() => {
/** Calm Material text field — radius 12, hairline outline, primary focus. */
function TextField({
  variant = "outlined",
  label,
  value,
  onChange,
  placeholder,
  supportingText,
  error = false,
  leadingIcon,
  type = "text",
  disabled = false,
  style
}) {
  const [focus, setFocus] = React.useState(false);
  const borderColor = error ? "var(--color-error)" : focus ? "var(--color-primary)" : "var(--color-outline-variant)";
  return /*#__PURE__*/React.createElement("label", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: "var(--space-4)",
      font: "var(--type-body-md)",
      opacity: disabled ? 0.5 : 1,
      ...style
    }
  }, label ? /*#__PURE__*/React.createElement("span", {
    style: {
      fontWeight: "var(--weight-label)",
      fontSize: "14px",
      color: focus && !error ? "var(--color-primary)" : "var(--text-secondary)"
    }
  }, label) : null, /*#__PURE__*/React.createElement("span", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: "var(--space-12)",
      padding: "var(--space-12) var(--space-16)",
      borderRadius: "var(--radius-control)",
      border: `1px solid ${variant === "outlined" || focus || error ? borderColor : "transparent"}`,
      boxShadow: focus ? `inset 0 0 0 1px ${borderColor}` : "none",
      background: variant === "filled" ? "var(--color-surface-container-high)" : "transparent",
      transition: "border-color var(--duration-quick) var(--ease-standard)"
    }
  }, leadingIcon ? /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: leadingIcon,
    color: "var(--text-secondary)"
  }) : null, /*#__PURE__*/React.createElement("input", {
    type: type,
    value: value,
    onChange: onChange,
    placeholder: placeholder,
    disabled: disabled,
    onFocus: () => setFocus(true),
    onBlur: () => setFocus(false),
    style: {
      flex: 1,
      minWidth: 0,
      border: "none",
      outline: "none",
      background: "transparent",
      font: "var(--type-body-lg)",
      color: "var(--text-primary)"
    }
  })), supportingText ? /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--type-caption)",
      color: error ? "var(--color-error)" : "var(--text-tertiary)",
      paddingLeft: "var(--space-16)"
    }
  }, supportingText) : null);
}
Object.assign(__ds_scope, { TextField });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/TextField.jsx", error: String((e && e.message) || e) }); }

// components/navigation/NavigationBar.jsx
try { (() => {
/** Bottom navigation — surface-container bar, active item gets a secondary-container pill. */
function NavigationBar({
  items = [],
  activeIndex = 0,
  onChange,
  style
}) {
  return /*#__PURE__*/React.createElement("nav", {
    style: {
      display: "grid",
      gridTemplateColumns: `repeat(${items.length || 1}, 1fr)`,
      background: "var(--color-surface-container)",
      padding: "var(--space-12) 0 var(--space-16)",
      boxShadow: "none",
      ...style
    }
  }, items.map((item, i) => {
    const active = i === activeIndex;
    return /*#__PURE__*/React.createElement("button", {
      key: i,
      type: "button",
      onClick: () => onChange && onChange(i),
      style: {
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: "var(--space-4)",
        border: "none",
        background: "transparent",
        cursor: "pointer",
        padding: 0,
        font: "var(--type-label)",
        fontSize: "12px"
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        width: "64px",
        height: "32px",
        borderRadius: "var(--radius-pill)",
        background: active ? "var(--color-secondary-container)" : "transparent",
        color: active ? "var(--color-on-secondary-container)" : "var(--text-secondary)",
        transition: "background var(--duration-quick) var(--ease-standard)"
      }
    }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
      name: item.icon,
      fill: active
    })), /*#__PURE__*/React.createElement("span", {
      style: {
        color: active ? "var(--text-primary)" : "var(--text-secondary)",
        fontWeight: active ? 600 : 500
      }
    }, item.label));
  }));
}
Object.assign(__ds_scope, { NavigationBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/NavigationBar.jsx", error: String((e && e.message) || e) }); }

// components/navigation/TopBar.jsx
try { (() => {
/** Top app bar — surface background (blends into page), elevation 0, left title 18/600. */
function TopBar({
  title,
  leading,
  onLeading,
  actions = [],
  style
}) {
  return /*#__PURE__*/React.createElement("header", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: "var(--space-8)",
      height: "64px",
      padding: "0 var(--space-8)",
      background: "var(--color-surface)",
      color: "var(--color-on-surface)",
      boxShadow: "none",
      ...style
    }
  }, leading ? /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    icon: leading,
    label: "Navigation",
    onClick: onLeading
  }) : null, /*#__PURE__*/React.createElement("h1", {
    style: {
      flex: 1,
      font: "var(--type-title)",
      margin: 0,
      paddingLeft: leading ? 0 : "var(--space-8)",
      color: "var(--text-primary)"
    }
  }, title), actions.map((a, i) => /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    key: i,
    icon: a.icon,
    label: a.label,
    onClick: a.onClick
  })));
}
Object.assign(__ds_scope, { TopBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/TopBar.jsx", error: String((e && e.message) || e) }); }

// ui_kits/lensbite/Screens.jsx
try { (() => {
const {
  TopBar,
  NavigationBar,
  Card,
  ListTile,
  Button,
  IconButton,
  Chip,
  Switch,
  TextField,
  Dialog,
  Icon
} = window.CalmMaterialDesignSystem_a3bee2;

/* Illustrative LensBite screens — original app screens were not provided;
   these demonstrate the Calm Material system in composition, not a recreation. */

const T = {
  headline: {
    font: "var(--type-headline-lg)",
    letterSpacing: "var(--tracking-display)",
    color: "var(--text-primary)",
    margin: 0
  },
  section: {
    font: "var(--type-title)",
    color: "var(--text-primary)",
    margin: 0
  },
  bodySm: {
    font: "var(--type-body-md)",
    color: "var(--text-secondary)",
    margin: 0
  }
};
function StatCard({
  value,
  unit,
  label
}) {
  return /*#__PURE__*/React.createElement(Card, {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: "var(--space-4)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--type-headline-md)",
      letterSpacing: "var(--tracking-display)",
      color: "var(--text-primary)"
    }
  }, value, /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: "var(--font-ui)",
      fontSize: "14px",
      fontWeight: 400,
      color: "var(--text-secondary)"
    }
  }, " ", unit)), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--type-caption)",
      color: "var(--text-secondary)"
    }
  }, label)));
}
function HomeScreen() {
  const [filter, setFilter] = React.useState(0);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: "var(--space-16)",
      padding: "0 var(--screen-pad) var(--space-16)"
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: T.headline
  }, "Good morning"), /*#__PURE__*/React.createElement("p", {
    style: {
      ...T.bodySm,
      marginTop: "-8px"
    }
  }, "Tuesday, July 3 \xB7 2 meals logged"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: "var(--space-16)"
    }
  }, /*#__PURE__*/React.createElement(StatCard, {
    value: "1,240",
    unit: "kcal",
    label: "Today"
  }), /*#__PURE__*/React.createElement(StatCard, {
    value: "68",
    unit: "g",
    label: "Protein"
  }), /*#__PURE__*/React.createElement(StatCard, {
    value: "142",
    unit: "g",
    label: "Carbs"
  })), /*#__PURE__*/React.createElement(Button, {
    icon: "photo_camera",
    style: {
      alignSelf: "stretch"
    }
  }, "Scan a meal"), /*#__PURE__*/React.createElement("h2", {
    style: T.section
  }, "Recent scans"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: "var(--space-8)"
    }
  }, ["All", "Breakfast", "Lunch", "Dinner"].map((c, i) => /*#__PURE__*/React.createElement(Chip, {
    key: c,
    selected: filter === i,
    onClick: () => setFilter(i)
  }, c))), /*#__PURE__*/React.createElement(Card, {
    padding: "var(--space-8)"
  }, /*#__PURE__*/React.createElement(ListTile, {
    leading: "lunch_dining",
    title: "Chicken salad bowl",
    subtitle: "520 kcal \xB7 12:40",
    trailing: "chevron_right",
    onClick: () => {}
  }), /*#__PURE__*/React.createElement(ListTile, {
    leading: "bakery_dining",
    title: "Oat porridge with berries",
    subtitle: "380 kcal \xB7 8:15",
    trailing: "chevron_right",
    onClick: () => {}
  }), /*#__PURE__*/React.createElement(ListTile, {
    leading: "local_cafe",
    title: "Flat white",
    subtitle: "120 kcal \xB7 8:05",
    trailing: "chevron_right",
    onClick: () => {}
  })));
}
function ScanScreen() {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: "var(--space-16)",
      padding: "0 var(--screen-pad) var(--space-16)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      height: "300px",
      borderRadius: "var(--radius-card)",
      background: "var(--color-surface-container-highest)",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      gap: "var(--space-12)",
      border: "1px dashed var(--color-outline-variant)"
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "photo_camera",
    size: 48,
    color: "var(--text-tertiary)"
  }), /*#__PURE__*/React.createElement("span", {
    style: T.bodySm
  }, "Camera preview")), /*#__PURE__*/React.createElement(Card, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: "var(--space-16)"
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "tips_and_updates",
    color: "var(--color-primary)"
  }), /*#__PURE__*/React.createElement("span", {
    style: T.bodySm
  }, "Fill the frame with your plate. Good light helps the estimate."))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: "var(--space-12)",
      justifyContent: "center",
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement(IconButton, {
    icon: "image",
    variant: "tonal",
    label: "Gallery"
  }), /*#__PURE__*/React.createElement(Button, {
    icon: "camera"
  }, "Capture"), /*#__PURE__*/React.createElement(IconButton, {
    icon: "flash_on",
    variant: "tonal",
    label: "Flash"
  })));
}
function SettingsScreen({
  dark,
  setDark
}) {
  const [notif, setNotif] = React.useState(true);
  const [confirming, setConfirming] = React.useState(false);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: "var(--space-16)",
      padding: "0 var(--screen-pad) var(--space-16)"
    }
  }, /*#__PURE__*/React.createElement("h2", {
    style: T.section
  }, "Appearance"), /*#__PURE__*/React.createElement(Card, {
    padding: "var(--space-8)"
  }, /*#__PURE__*/React.createElement(ListTile, {
    leading: "dark_mode",
    title: "Dark theme",
    subtitle: "Follows system by default",
    trailing: /*#__PURE__*/React.createElement(Switch, {
      checked: dark,
      onChange: setDark,
      label: "Dark theme"
    })
  })), /*#__PURE__*/React.createElement("h2", {
    style: T.section
  }, "Preferences"), /*#__PURE__*/React.createElement(Card, {
    padding: "var(--space-8)"
  }, /*#__PURE__*/React.createElement(ListTile, {
    leading: "notifications",
    title: "Notifications",
    subtitle: "Daily summary at 9:00",
    trailing: /*#__PURE__*/React.createElement(Switch, {
      checked: notif,
      onChange: setNotif,
      label: "Notifications"
    })
  }), /*#__PURE__*/React.createElement(ListTile, {
    leading: "language",
    title: "Language",
    subtitle: "English",
    trailing: "chevron_right",
    onClick: () => {}
  }), /*#__PURE__*/React.createElement(ListTile, {
    leading: "straighten",
    title: "Units",
    subtitle: "Metric",
    trailing: "chevron_right",
    onClick: () => {}
  })), /*#__PURE__*/React.createElement("h2", {
    style: T.section
  }, "Data"), /*#__PURE__*/React.createElement(Card, {
    padding: "var(--space-8)"
  }, /*#__PURE__*/React.createElement(ListTile, {
    leading: "download",
    title: "Export history",
    trailing: "chevron_right",
    onClick: () => {}
  }), /*#__PURE__*/React.createElement(ListTile, {
    leading: "delete",
    title: "Clear scan history",
    onClick: () => setConfirming(true)
  })), /*#__PURE__*/React.createElement(Dialog, {
    open: confirming,
    onClose: () => setConfirming(false),
    title: "Clear scan history?",
    actions: [{
      label: "Cancel",
      onClick: () => setConfirming(false)
    }, {
      label: "Clear",
      onClick: () => setConfirming(false)
    }]
  }, "All logged meals will be removed. This can't be undone."));
}
function App() {
  const [tab, setTab] = React.useState(0);
  const [dark, setDark] = React.useState(false);
  const titles = ["LensBite", "Scan", "Settings"];
  return /*#__PURE__*/React.createElement("div", {
    "data-theme": dark ? "dark" : undefined,
    style: {
      width: "412px",
      height: "752px",
      background: "var(--color-surface)",
      borderRadius: "24px",
      overflow: "hidden",
      display: "flex",
      flexDirection: "column",
      border: "1px solid rgba(0,0,0,0.12)",
      position: "relative",
      colorScheme: dark ? "dark" : "light"
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: titles[tab],
    actions: tab === 0 ? [{
      icon: "search",
      label: "Search"
    }] : [],
    style: {
      flexShrink: 0
    }
  }), /*#__PURE__*/React.createElement("main", {
    style: {
      flex: 1,
      overflowY: "auto"
    }
  }, tab === 0 ? /*#__PURE__*/React.createElement(HomeScreen, null) : tab === 1 ? /*#__PURE__*/React.createElement(ScanScreen, null) : /*#__PURE__*/React.createElement(SettingsScreen, {
    dark: dark,
    setDark: setDark
  })), /*#__PURE__*/React.createElement(NavigationBar, {
    items: [{
      icon: "home",
      label: "Home"
    }, {
      icon: "photo_camera",
      label: "Scan"
    }, {
      icon: "settings",
      label: "Settings"
    }],
    activeIndex: tab,
    onChange: setTab,
    style: {
      flexShrink: 0
    }
  }));
}
ReactDOM.createRoot(document.getElementById("root")).render(/*#__PURE__*/React.createElement(App, null));
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/lensbite/Screens.jsx", error: String((e && e.message) || e) }); }

__ds_ns.Button = __ds_scope.Button;

__ds_ns.IconButton = __ds_scope.IconButton;

__ds_ns.Card = __ds_scope.Card;

__ds_ns.Dialog = __ds_scope.Dialog;

__ds_ns.ListTile = __ds_scope.ListTile;

__ds_ns.Chip = __ds_scope.Chip;

__ds_ns.Switch = __ds_scope.Switch;

__ds_ns.TextField = __ds_scope.TextField;

__ds_ns.Icon = __ds_scope.Icon;

__ds_ns.NavigationBar = __ds_scope.NavigationBar;

__ds_ns.TopBar = __ds_scope.TopBar;

})();
