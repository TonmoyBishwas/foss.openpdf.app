Flat Calm Material button: fill `primary`, radius 12, padding 24×16, label 16/500, no shadow. One filled button per view; use tonal/text for lesser actions.

```jsx
<Button onClick={save}>Save</Button>
<Button variant="tonal" icon="add">Add item</Button>
<Button variant="text">Cancel</Button>
```

- `variant`: `filled` (primary) · `tonal` (secondary-container) · `text` (transparent, primary label)
- `icon`: leading Material Symbols name. `disabled` uses the 12%/38% ladder.
