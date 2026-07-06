Single-line input: radius 12, `outline-variant` hairline, focus swaps to a 2px `primary` border. Label above, supporting text below.

```jsx
<TextField label="Email" placeholder="you@example.com" leadingIcon="mail" />
<TextField label="Name" error supportingText="Required" />
```

- `variant`: `outlined` (default) · `filled` (surface-container-high fill)
- `error` turns border + supporting text to the error role.
