/**
 * Flat grouped container — tonal fill, radius 16, never a drop shadow.
 * @startingPoint section="Components" subtitle="Flat tonal card, radius 16" viewport="700x220"
 */
export interface CardProps {
  /** Default "filled" (surface-container-highest). Outlined = surface + hairline */
  variant?: "filled" | "outlined";
  /** CSS padding. Default var(--card-pad) = 16px */
  padding?: string;
  /** Makes the card interactive with a hover state layer */
  onClick?: () => void;
  style?: React.CSSProperties;
  children?: React.ReactNode;
}
