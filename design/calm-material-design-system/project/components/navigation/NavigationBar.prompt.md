Bottom navigation for 3–5 app destinations — flat `surface-container` bar; active item gets a `secondary-container` pill and a filled glyph.

```jsx
<NavigationBar
  items={[{icon: "home", label: "Home"}, {icon: "photo_camera", label: "Scan"}, {icon: "settings", label: "Settings"}]}
  activeIndex={tab} onChange={setTab} />
```
