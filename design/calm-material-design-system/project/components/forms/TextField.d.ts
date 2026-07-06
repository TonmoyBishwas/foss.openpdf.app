/** Single-line text input — radius 12, hairline outline, primary focus ring. */
export interface TextFieldProps {
  /** Default "outlined". Filled uses surface-container-high fill */
  variant?: "outlined" | "filled";
  label?: string;
  value?: string;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  placeholder?: string;
  /** Helper line below; renders in error color when error=true */
  supportingText?: string;
  error?: boolean;
  /** Material Symbols name */
  leadingIcon?: string;
  type?: string;
  disabled?: boolean;
  style?: React.CSSProperties;
}
