List row — leading 24px icon, 16px title, 14px subtitle at 60% opacity, trailing slot. Group tiles inside a `Card` with reduced padding.

```jsx
<Card padding="var(--space-8)">
  <ListTile leading="notifications" title="Notifications" subtitle="Daily summary at 9:00" trailing={<Switch checked />} />
  <ListTile leading="language" title="Language" subtitle="English" trailing="chevron_right" onClick={open} />
</Card>
```
