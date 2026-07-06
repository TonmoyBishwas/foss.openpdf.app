Icon-only round button (44px hit target, 24px glyph) for app bars and tiles.

```jsx
<IconButton icon="arrow_back" label="Back" onClick={goBack} />
<IconButton icon="favorite" variant="tonal" selected label="Like" />
```

- `variant`: `standard` (bare, on-surface tint) · `filled` · `tonal`
- `selected` on standard tints `primary` and fills the glyph.
