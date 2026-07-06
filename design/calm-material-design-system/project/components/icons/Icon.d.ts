/** Material Symbols Rounded glyph, tinted via currentColor or `color`. */
export interface IconProps {
  /** Material Symbols name, e.g. "settings", "photo_camera" */
  name: string;
  /** px size. Default 24 */
  size?: number;
  /** Filled style (active states). Default false */
  fill?: boolean;
  /** CSS color; defaults to inherited text color */
  color?: string;
  style?: React.CSSProperties;
}
