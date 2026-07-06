/** Modal confirmation/info dialog with text-button actions. */
export interface DialogProps {
  open: boolean;
  /** Called on scrim click */
  onClose?: () => void;
  title?: React.ReactNode;
  /** Text buttons, right-aligned */
  actions?: { label: string; onClick?: () => void }[];
  style?: React.CSSProperties;
  children?: React.ReactNode;
}
