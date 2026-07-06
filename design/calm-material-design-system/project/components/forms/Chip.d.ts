/** Compact filter/assist chip for toggles and quick filters. */
export interface ChipProps {
  /** Optional leading Material Symbols icon (replaced by a check when selected) */
  icon?: string;
  selected?: boolean;
  disabled?: boolean;
  onClick?: () => void;
  style?: React.CSSProperties;
  children?: React.ReactNode;
}
