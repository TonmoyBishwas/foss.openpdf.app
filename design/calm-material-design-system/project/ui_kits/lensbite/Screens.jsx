const { TopBar, NavigationBar, Card, ListTile, Button, IconButton, Chip, Switch, TextField, Dialog, Icon } = window.CalmMaterialDesignSystem_a3bee2;

/* Illustrative LensBite screens — original app screens were not provided;
   these demonstrate the Calm Material system in composition, not a recreation. */

const T = {
  headline: { font: "var(--type-headline-lg)", letterSpacing: "var(--tracking-display)", color: "var(--text-primary)", margin: 0 },
  section: { font: "var(--type-title)", color: "var(--text-primary)", margin: 0 },
  bodySm: { font: "var(--type-body-md)", color: "var(--text-secondary)", margin: 0 },
};

function StatCard({ value, unit, label }) {
  return (
    <Card style={{ flex: 1 }}>
      <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-4)" }}>
        <span style={{ font: "var(--type-headline-md)", letterSpacing: "var(--tracking-display)", color: "var(--text-primary)" }}>
          {value}<span style={{ fontFamily: "var(--font-ui)", fontSize: "14px", fontWeight: 400, color: "var(--text-secondary)" }}> {unit}</span>
        </span>
        <span style={{ font: "var(--type-caption)", color: "var(--text-secondary)" }}>{label}</span>
      </div>
    </Card>
  );
}

function HomeScreen() {
  const [filter, setFilter] = React.useState(0);
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-16)", padding: "0 var(--screen-pad) var(--space-16)" }}>
      <h1 style={T.headline}>Good morning</h1>
      <p style={{ ...T.bodySm, marginTop: "-8px" }}>Tuesday, July 3 · 2 meals logged</p>
      <div style={{ display: "flex", gap: "var(--space-16)" }}>
        <StatCard value="1,240" unit="kcal" label="Today" />
        <StatCard value="68" unit="g" label="Protein" />
        <StatCard value="142" unit="g" label="Carbs" />
      </div>
      <Button icon="photo_camera" style={{ alignSelf: "stretch" }}>Scan a meal</Button>
      <h2 style={T.section}>Recent scans</h2>
      <div style={{ display: "flex", gap: "var(--space-8)" }}>
        {["All", "Breakfast", "Lunch", "Dinner"].map((c, i) => (
          <Chip key={c} selected={filter === i} onClick={() => setFilter(i)}>{c}</Chip>
        ))}
      </div>
      <Card padding="var(--space-8)">
        <ListTile leading="lunch_dining" title="Chicken salad bowl" subtitle="520 kcal · 12:40" trailing="chevron_right" onClick={() => {}} />
        <ListTile leading="bakery_dining" title="Oat porridge with berries" subtitle="380 kcal · 8:15" trailing="chevron_right" onClick={() => {}} />
        <ListTile leading="local_cafe" title="Flat white" subtitle="120 kcal · 8:05" trailing="chevron_right" onClick={() => {}} />
      </Card>
    </div>
  );
}

function ScanScreen() {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-16)", padding: "0 var(--screen-pad) var(--space-16)" }}>
      <div style={{
        height: "300px", borderRadius: "var(--radius-card)", background: "var(--color-surface-container-highest)",
        display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: "var(--space-12)",
        border: "1px dashed var(--color-outline-variant)",
      }}>
        <Icon name="photo_camera" size={48} color="var(--text-tertiary)" />
        <span style={T.bodySm}>Camera preview</span>
      </div>
      <Card>
        <div style={{ display: "flex", alignItems: "center", gap: "var(--space-16)" }}>
          <Icon name="tips_and_updates" color="var(--color-primary)" />
          <span style={T.bodySm}>Fill the frame with your plate. Good light helps the estimate.</span>
        </div>
      </Card>
      <div style={{ display: "flex", gap: "var(--space-12)", justifyContent: "center", alignItems: "center" }}>
        <IconButton icon="image" variant="tonal" label="Gallery" />
        <Button icon="camera">Capture</Button>
        <IconButton icon="flash_on" variant="tonal" label="Flash" />
      </div>
    </div>
  );
}

function SettingsScreen({ dark, setDark }) {
  const [notif, setNotif] = React.useState(true);
  const [confirming, setConfirming] = React.useState(false);
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-16)", padding: "0 var(--screen-pad) var(--space-16)" }}>
      <h2 style={T.section}>Appearance</h2>
      <Card padding="var(--space-8)">
        <ListTile leading="dark_mode" title="Dark theme" subtitle="Follows system by default" trailing={<Switch checked={dark} onChange={setDark} label="Dark theme" />} />
      </Card>
      <h2 style={T.section}>Preferences</h2>
      <Card padding="var(--space-8)">
        <ListTile leading="notifications" title="Notifications" subtitle="Daily summary at 9:00" trailing={<Switch checked={notif} onChange={setNotif} label="Notifications" />} />
        <ListTile leading="language" title="Language" subtitle="English" trailing="chevron_right" onClick={() => {}} />
        <ListTile leading="straighten" title="Units" subtitle="Metric" trailing="chevron_right" onClick={() => {}} />
      </Card>
      <h2 style={T.section}>Data</h2>
      <Card padding="var(--space-8)">
        <ListTile leading="download" title="Export history" trailing="chevron_right" onClick={() => {}} />
        <ListTile leading="delete" title="Clear scan history" onClick={() => setConfirming(true)} />
      </Card>
      <Dialog open={confirming} onClose={() => setConfirming(false)} title="Clear scan history?"
        actions={[{ label: "Cancel", onClick: () => setConfirming(false) }, { label: "Clear", onClick: () => setConfirming(false) }]}>
        All logged meals will be removed. This can't be undone.
      </Dialog>
    </div>
  );
}

function App() {
  const [tab, setTab] = React.useState(0);
  const [dark, setDark] = React.useState(false);
  const titles = ["LensBite", "Scan", "Settings"];
  return (
    <div data-theme={dark ? "dark" : undefined} style={{
      width: "412px", height: "752px", background: "var(--color-surface)",
      borderRadius: "24px", overflow: "hidden", display: "flex", flexDirection: "column",
      border: "1px solid rgba(0,0,0,0.12)", position: "relative", colorScheme: dark ? "dark" : "light",
    }}>
      <TopBar
        title={titles[tab]}
        actions={tab === 0 ? [{ icon: "search", label: "Search" }] : []}
        style={{ flexShrink: 0 }}
      />
      <main style={{ flex: 1, overflowY: "auto" }}>
        {tab === 0 ? <HomeScreen /> : tab === 1 ? <ScanScreen /> : <SettingsScreen dark={dark} setDark={setDark} />}
      </main>
      <NavigationBar
        items={[{ icon: "home", label: "Home" }, { icon: "photo_camera", label: "Scan" }, { icon: "settings", label: "Settings" }]}
        activeIndex={tab}
        onChange={setTab}
        style={{ flexShrink: 0 }}
      />
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<App />);
