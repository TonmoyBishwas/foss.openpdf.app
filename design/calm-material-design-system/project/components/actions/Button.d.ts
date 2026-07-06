/**
 * Calm Material button. Filled = primary action (one per view), tonal =
 * secondary emphasis, text = low emphasis. Flat (elevation 0), radius 12.
 * @startingPoint section="Components" subtitle="Filled, tonal and text buttons" viewport="700x180"
 */
export interface ButtonProps {
  /** Visual emphasis. Default "filled" */
  variant?: "filled" | "tonal" | "text";
  /** Optional leading Material Symbols icon name */
  icon?: string;
  disabled?: boolean;
  onClick?: () => void;
  type?: "button" | "submit";
  style?: React.CSSProperties;
  children?: React.ReactNode;
}
