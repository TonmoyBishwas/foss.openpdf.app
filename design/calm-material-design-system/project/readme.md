# Calm Material — Design System

A portable design language extracted from **LensBite**, a Flutter Android app (meal-scanning / nutrition). The owner liked the app's look and asked Claude to distill it into a reusable system; this project is the refined, expanded version of that distillation.

**Source provided:** `uploads/DESIGN_SYSTEM.md` — a token + principles reference written from the LensBite Flutter app. No codebase, Figma, screenshots, or logo files were provided; everything here derives from that one document. The LensBite screens in `ui_kits/lensbite/` are therefore **illustrative compositions**, not recreations.

## The idea in one paragraph

One desaturated navy seed (`#2C3E50`) generates the entire palette via the Material 3 tonal algorithm. Surfaces are **flat** — depth comes from tonal surface steps, never drop shadows. Type is a two-font system: **Doto** (dot matrix, 700) for headlines and big numeric readouts only, **Hanken Grotesk** for all other UI text (titles 600, body 400). Hierarchy in text comes from an opacity ladder (100 / 60 / 38%) of one color, not separate greys. Corners are soft (12px controls, 16px containers), spacing snaps to an 8-point grid, and light + dark themes are both first-class. The result reads like a calm utility app with a dot-matrix signature.

## Content fundamentals

The source app is utility-first; copy follows suit:

- **Tone:** quiet, factual, helpful. No exclamation points, no hype, no emoji.
- **Casing:** sentence case everywhere — titles, buttons, labels ("Scan a meal", "Clear scan history", never "SCAN NOW").
- **Person:** speaks to the user as "you", refers to itself not at all. Instructions are imperative ("Fill the frame with your plate").
- **Brevity:** labels are 1–3 words; supporting text is one short sentence; destructive confirmations state the consequence plainly ("This can't be undone.").
- **Numbers** do the talking: kcal, grams, timestamps — set large in headline type, units small and secondary.

## Visual foundations

- **Color:** seed `#2C3E50` → M3 scheme. Light: `surface #FCFCFD`, cards `#E7E9EC`, hairlines `#C5C9CE`, text `#1A1C1E`. Dark: lightened primary `#7FA8C9` on tonal near-black `#121417` (never `#000`). Error `#BA1A1A`/`#FFB4AB` is the only non-derived accent. Components reference semantic roles, never raw hex.
- **Type:** two fonts with strict roles. Display: **Doto** (dot matrix, Google Fonts) at 700, for 32/24 headlines and large numbers ONLY — never body, titles, labels, or anything under 24px. UI: **Hanken Grotesk** for everything else — 18 titles (600), 16/14 body (400, 1.5 line-height), 16 labels (500), 12 captions. Display sizes use 0 tracking (dots need air). *(Revised 2026-07: previously system-font-only; the owner chose the Doto twist from a font exploration.)*
- **Text hierarchy:** on-surface at 100 / 60 / 38% opacity (`--text-primary/secondary/tertiary`).
- **Depth:** elevation 0 everywhere. Cards = `surface-container-highest` tonal step; optional 1px `outline-variant` hairline for extra separation. No drop shadows, no blur, no glassmorphism, no gradients, no background imagery or textures.
- **Radii:** 12px controls (buttons, fields, chips) · 16px containers (cards, sheets, dialogs) · pill for avatars/toggles/badges. Never 0.
- **Spacing:** 8pt grid — 4/8/12/16/24/32/48. Screen edge 16 (24 spacious), card gap 16, inside-card 16, button padding 24×16. Single-column, content-first; whitespace does the work.
- **Hover:** 8% on-surface state layer. **Press:** 10–12% layer, no shrink/scale. **Disabled:** 12% fill, 38% text.
- **Motion:** quiet — 150ms (state changes) / 250ms (dialogs, expansion) on M3 standard easing `cubic-bezier(0.2,0,0,1)`. Fades and gentle position eases; no bounces, springs, or parallax.
- **Layout chrome:** app bar uses the page's own `surface` color (blends in), title left-aligned; bottom nav is a flat `surface-container` bar with a pill active indicator.
- **Imagery:** none in the source system. If photos appear (meal scans), contain them in 16px-radius cards, untreated (no duotones/overlays).

## Iconography

- **Set:** [Material Symbols Rounded](https://fonts.google.com/icons) — variable icon font, loaded from the Google Fonts CDN via `tokens/icons.css`. Rounded style matches the soft corner language. *(The source doc names no icon set; the Flutter app used Material icons — Rounded is the nearest match. Substitution flagged.)*
- **Specs:** 24px default, weight 400, outline style; `FILL 1` for active/selected states.
- **Tinting:** `--color-primary` for active/brand, `--color-on-surface` for neutral, tertiary opacity for placeholders.
- **Never:** emoji, hand-drawn SVGs, mixed icon sets, unicode-glyphs-as-icons.
- **Logo:** none exists. Render "LensBite" / "Calm Material" in plain type (see `guidelines/brand-wordmark.html`).

## Improvements over the source doc

Marked as **intentional additions** (all derived from the seed / M3 conventions, values are approximations — regenerate from the seed for pixel-exact tones):

- Full tonal surface ladder (`container-lowest → highest`) instead of just two surfaces.
- `primary-container` / `secondary` / `secondary-container` roles (needed by chips, tonal buttons, nav pills).
- `error-container` pair; explicit `outline` (vs `outline-variant`).
- State-layer, disabled, motion, and tap-target (44px) tokens.
- Components not spec'd in §7 but required by the app archetype: `Chip`, `Switch`, `Dialog`, `NavigationBar`, `Icon`.

## Index

- `styles.css` — global entry; imports everything under `tokens/`.
- `tokens/` — `colors.css` (light `:root` + `[data-theme="dark"]`), `typography.css`, `spacing.css`, `shape.css`, `motion.css`, `icons.css`.
- `guidelines/` — foundation specimen cards (colors, type, spacing, radius, depth, icons, motion, wordmark).
- `components/` — reusable React primitives:
  - `actions/` — **Button**, **IconButton**
  - `forms/` — **TextField**, **Chip**, **Switch**
  - `containment/` — **Card**, **ListTile**, **Dialog**
  - `navigation/` — **TopBar**, **NavigationBar**
  - `icons/` — **Icon**
- `ui_kits/lensbite/` — illustrative sample app (Home / Scan / Settings, light + dark).
- `uploads/DESIGN_SYSTEM.md` — the original source document.
- `SKILL.md` — agent-skill entry point.

## Usage

Link `styles.css`, use semantic tokens (`var(--color-primary)`, `var(--surface-card)`, `var(--radius-card)`…), never raw hex. Dark mode: set `data-theme="dark"` on any ancestor. Components mount from the compiled bundle: `const { Button, Card } = window.CalmMaterialDesignSystem_a3bee2`.
