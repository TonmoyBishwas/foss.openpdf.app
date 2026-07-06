/** Toggle switch for settings rows. Track turns primary when on. */
export interface SwitchProps {
  checked?: boolean;
  onChange?: (checked: boolean) => void;
  disabled?: boolean;
  /** Accessible label */
  label?: string;
  style?: React.CSSProperties;
}
