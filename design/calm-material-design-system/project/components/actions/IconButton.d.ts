/** Round 44px icon-only button for app bars, tiles and toolbars. */
export interface IconButtonProps {
  /** Material Symbols icon name */
  icon: string;
  /** Default "standard" (bare glyph) */
  variant?: "standard" | "filled" | "tonal";
  /** Standard variant: tints primary + filled glyph */
  selected?: boolean;
  disabled?: boolean;
  onClick?: () => void;
  /** Accessible label — always provide */
  label?: string;
  style?: React.CSSProperties;
}
