Modal dialog — radius 16, `surface-container-high` fill, 32% scrim, text-button actions. No shadow.

```jsx
<Dialog open={confirming} onClose={cancel} title="Delete photo?"
  actions={[{label: "Cancel", onClick: cancel}, {label: "Delete", onClick: doDelete}]}>
  This can't be undone.
</Dialog>
```

Positioned `absolute` — give the app frame `position: relative` (or wrap at body level).
