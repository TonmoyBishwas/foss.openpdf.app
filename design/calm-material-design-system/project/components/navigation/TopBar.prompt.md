Top app bar that blends into the page: `surface` background, elevation 0, left-aligned 18/600 title, `on-surface` icons.

```jsx
<TopBar title="Settings" leading="arrow_back" onLeading={goBack}
        actions={[{icon: "search", label: "Search"}]} />
```
