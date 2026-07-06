/** Bottom navigation bar (3–5 destinations) with pill active indicator. */
export interface NavigationBarProps {
  items: { icon: string; label: string }[];
  activeIndex?: number;
  onChange?: (index: number) => void;
  style?: React.CSSProperties;
}
