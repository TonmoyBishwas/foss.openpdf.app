# Design Language Reference — "Calm Material"

> A portable, framework-agnostic design system extracted from the **LensBite** app.
> It is intentionally **not tied to Flutter** — use it for Kotlin/Compose, Android XML,
> Java, SwiftUI, React Native, or web. The goal is one consistent visual identity across
> every app you build.

---

## 0. How to use this file (for an LLM or a developer)

When starting a new app, paste this file in and say:
> "Apply the **Calm Material** design language defined in this document. Use the token
> values below. Implement it natively for `<platform>` using the platform notes in the
> appendix. Do not invent new colors, fonts, or radii — derive everything from these tokens."

The identity is defined by **principles + tokens**, not by any one toolkit. As long as the
tokens and rules below are honored, the app will look like it belongs to the same family.

---

## 1. Design principles (the "why")

1. **One seed, a whole palette.** A single brand color generates the entire color scheme
   via the Material 3 tonal algorithm. This is the biggest reason the app reads as a
   "native Google" app — everything is harmonically related.
2. **Flat, not floating.** No drop shadows. Depth and grouping are shown with **tonal
   surface steps** (a slightly different background shade), never with elevation/shadows.
3. **Calm, low-saturation palette.** A desaturated navy/slate as the hero color; neutral
   greys for everything else. Color is used sparingly and purposefully.
4. **System font, always.** Use the platform's native UI font (Roboto on Android, SF Pro
   on iOS). **Never bundle a custom display font.** This is core to the native feel.
5. **Generous rounding.** Soft, friendly corners (12–16dp). Nothing sharp.
6. **Semibold, not bold.** Headings use weight 600, not 700 — confident but not heavy.
7. **Quiet hierarchy via opacity.** Secondary/tertiary text is the primary text color at
   reduced opacity, not a separate grey.
8. **Light & dark are first-class.** Both themes ship from day one; dark mode lightens the
   brand color and uses tonal near-black (never pure `#000000`).
9. **Roomy, grid-aligned spacing.** Everything snaps to an 8pt grid; screens breathe.

---

## 2. Color tokens

### 2.1 The seed (the only thing you must keep)
```
BRAND SEED = #2C3E50   (desaturated navy / slate)
```
Feed this seed into a Material 3 tonal palette generator
([Material Theme Builder](https://m3.material.io/theme-builder) / Compose `dynamicColorScheme`
/ Flutter `ColorScheme.fromSeed`) to produce all roles below. The values listed are
**reference approximations** — regenerate from the seed for pixel-exact tones on platforms
that lack an M3 generator.

### 2.2 Semantic roles — LIGHT
| Role | Hex | Use |
|------|-----|-----|
| `primary` | `#2C3E50` | Buttons, active icons, links, brand accents |
| `onPrimary` | `#FFFFFF` | Text/icons on primary |
| `surface` / background | `#FCFCFD` | App background, scaffold, app bar |
| `surfaceContainerHighest` | `#E7E9EC` | **Cards, tiles, grouped containers** |
| `outlineVariant` | `#C5C9CE` | Hairline borders, dividers |
| `onSurface` (text primary) | `#1A1C1E` | Headlines & primary body text (100%) |
| text secondary | `onSurface @ 60%` | Subtitles, captions |
| text tertiary | `onSurface @ 38%` | Hints, disabled, timestamps |
| `error` | `#BA1A1A` | Error states |

### 2.3 Semantic roles — DARK
| Role | Hex | Use |
|------|-----|-----|
| `primary` | `#7FA8C9` | **Lightened** brand for contrast on dark |
| `onPrimary` | `#FFFFFF` | Text/icons on primary |
| `surface` / background | `#121417` | Tonal near-black (never pure black) |
| `surfaceContainerHighest` | `#2A2D31` | Cards, tiles, grouped containers |
| `outlineVariant` | `#42474E` | Hairline borders, dividers |
| `onSurface` (text primary) | `#E2E2E6` | Primary text (100%) |
| text secondary | `onSurface @ 60%` | Subtitles, captions |
| text tertiary | `onSurface @ 38%` | Hints, disabled |
| `error` | `#FFB4AB` | Error states |

**Rule:** Components reference **semantic roles** (`surface`, `primary`, `onSurface`…),
never raw hex. Switching light/dark then "just works."

---

## 3. Typography

**Font family:** the platform default system UI font. Do **not** ship a custom font.
**Weight convention:** headings = **600 (semibold)**; buttons/labels = **500 (medium)**;
body = **400 (regular)**.

| Token | Size (sp/pt) | Weight | Tracking | Line-height | Use |
|-------|------|--------|----------|-------------|-----|
| Headline Large | 32 | 600 | −0.5 | — | Screen hero titles, big numbers |
| Headline Medium | 24 | 600 | −0.5 | — | Section titles |
| Title / App-bar | 18 | 600 | 0 | — | App bar, dialog titles |
| Body Large | 16 | 400 | 0 | 1.5 | Primary reading text |
| Body Medium | 14 | 400 | 0 | 1.5 | Secondary text (use 60% color) |
| Label / Button | 16 | 500 | 0 | — | Button text, tabs |

Notes:
- **Negative tracking (−0.5)** on large headings tightens display text — a key detail.
- Generous **1.5 line-height** on body for readability.

---

## 4. Shape (corner radius)

| Token | Radius | Applies to |
|-------|--------|-----------|
| `radius.button` | **12dp** | Buttons, text fields, chips, small controls |
| `radius.card` | **16dp** | Cards, sheets, dialogs, grouped containers |
| `radius.pill` | full/999dp | Avatars, toggles, badges (optional) |

Never use sharp 0dp corners on interactive or container elements.

---

## 5. Elevation & depth

- **Default elevation = 0 everywhere** (app bar, buttons, cards).
- Express hierarchy through **tonal surface steps**, not shadows:
  `surface` (background) → `surfaceContainerHighest` (card sits "above" the background by
  being a slightly different shade).
- Avoid Material drop shadows. If a platform forces shadows, keep them near-invisible.

---

## 6. Spacing & layout

**8-point grid.** All margins, padding, and gaps come from this scale:
```
4 · 8 · 12 · 16 · 24 · 32 · 48
```
| Context | Value |
|---------|-------|
| Screen edge padding | 16 (compact) or 24 (spacious) |
| Gap between cards/sections | 16 |
| Inside-card padding | 16 |
| Button padding | 24 horizontal · 16 vertical |
| Icon size (default) | 24dp |

Single-column, content-first layouts. Let whitespace do the work.

---

## 7. Core component specs

> Describe behavior abstractly; implement with each platform's native widget.

**App bar / top bar**
- Background = `surface` (blends into the page — no contrasting bar color).
- Elevation 0; title **left-aligned**, 18sp/600.
- Icons tinted `onSurface`.

**Primary button (filled)**
- Fill `primary`, text/icon `onPrimary` (white).
- Radius 12; padding 24×16; elevation 0; label 16sp/500.

**Secondary / text button**
- Transparent fill; label/icon in `primary`. Same radius & padding.

**Card / list tile / grouped container**
- Fill `surfaceContainerHighest`; radius 16; **no shadow**.
- Optional 1px `outlineVariant` border if extra separation is needed.

**Icons**
- 24dp default; tinted `primary` for active/brand, `onSurface` for neutral.

**Inputs / text fields**
- Radius 12; filled or outlined with `outlineVariant`; focus state uses `primary`.

---

## 8. Dark mode rules

1. Ship both themes; follow the OS setting by default, with a manual override toggle
   (persist the user's choice).
2. **Lighten** the brand color in dark mode (`#2C3E50` → `#7FA8C9`) so it stays legible.
3. Backgrounds = tonal near-black (`#121417`), **never** `#000000`.
4. Keep the same opacity ladder for text (100 / 60 / 38%).

---

## 9. Do / Don't

✅ Do
- Generate the whole palette from the one seed.
- Use semantic role names in code.
- Keep surfaces flat; group with tonal shades.
- Use the system font and weight 600 for headings.
- Snap everything to the 8pt grid.

❌ Don't
- Hardcode hex values inside screens/components.
- Add drop shadows or heavy elevation.
- Bundle a decorative custom font for UI text.
- Use pure black/white as primary surfaces.
- Use bold (700) where semibold (600) reads cleaner.
- Introduce a second accent color without deriving it from the M3 scheme.

---

## Appendix A — Per-platform implementation notes

**Jetpack Compose (Kotlin) — recommended for native Android**
- `MaterialTheme` (Material 3). Build a `ColorScheme` from seed `#2C3E50` via Material
  Theme Builder, or use `dynamicColorScheme` on Android 12+ as an enhancement.
- `Typography` with default font, the sizes/weights from §3.
- `Shapes(small = 12.dp, medium = 16.dp)`. Set component elevation to `0.dp`.

**Android Views / XML (Java or Kotlin)**
- Parent theme `Theme.Material3.DayNight.NoActionBar`.
- Map roles: `colorPrimary`, `colorOnPrimary`, `colorSurface`, `colorSurfaceContainerHighest`,
  `colorOutlineVariant`, `colorError`.
- `shapeAppearanceSmallComponent` = 12dp, `…MediumComponent` = 16dp.
- `app:elevation="0dp"` on `MaterialCardView`, `AppBarLayout`, buttons. Use
  `values/` + `values-night/` for light/dark.

**Flutter (reference — the source app)**
- `ThemeData(useMaterial3: true, colorScheme: ColorScheme.fromSeed(seedColor: Color(0xFF2C3E50), …))`.
- `CardThemeData(elevation: 0, shape: RoundedRectangleBorder(borderRadius: 16))`,
  `ElevatedButton` radius 12, app bar elevation 0. System font (no GoogleFonts).

**SwiftUI / iOS (cross-brand parity)**
- System font (SF Pro); accent color = navy `#2C3E50` (lighten for dark).
- Use `.background(Color(surfaceContainerHighest))` grouped lists, corner radius 16,
  `.listRowSeparator` as hairlines; avoid shadows.

**Web / CSS**
- Expose tokens as CSS custom properties (`--color-primary`, `--radius-card: 16px`, …);
  swap a `[data-theme="dark"]` block. Use `font-family: system-ui, Roboto, sans-serif`.

---

## Appendix B — Token quick-reference (copy/paste)

```
seed:                #2C3E50
primary.light:       #2C3E50      primary.dark:       #7FA8C9
onPrimary:           #FFFFFF
surface.light:       #FCFCFD      surface.dark:       #121417
card.light:          #E7E9EC      card.dark:          #2A2D31
border.light:        #C5C9CE      border.dark:        #42474E
onSurface.light:     #1A1C1E      onSurface.dark:     #E2E2E6
textSecondary:       onSurface @ 60%
textTertiary:        onSurface @ 38%
error.light:         #BA1A1A      error.dark:         #FFB4AB

radius.button: 12   radius.card: 16
spacing grid:  4 / 8 / 12 / 16 / 24 / 32 / 48
font:          system default (Roboto/SF Pro) — no custom font
weights:       heading 600 · label 500 · body 400
heading tracking: -0.5   body line-height: 1.5
elevation:     0 everywhere (tonal surfaces for depth)
```
