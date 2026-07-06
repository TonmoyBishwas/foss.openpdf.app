/** List row for settings and content lists; compose inside a Card. */
export interface ListTileProps {
  /** Material Symbols name, or a custom node */
  leading?: string | React.ReactNode;
  title: React.ReactNode;
  /** Secondary line at 60% opacity */
  subtitle?: React.ReactNode;
  /** Icon name or node (e.g. <Switch/>, "chevron_right") */
  trailing?: string | React.ReactNode;
  onClick?: () => void;
  style?: React.CSSProperties;
}
