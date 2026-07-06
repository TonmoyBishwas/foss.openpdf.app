/** Top app bar that blends into the page — surface bg, no elevation, left-aligned title. */
export interface TopBarProps {
  title: React.ReactNode;
  /** Leading Material Symbols icon, e.g. "arrow_back" or "menu" */
  leading?: string;
  onLeading?: () => void;
  /** Trailing icon buttons */
  actions?: { icon: string; label?: string; onClick?: () => void }[];
  style?: React.CSSProperties;
}
