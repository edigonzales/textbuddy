import type { EditorTextChangedDetail } from "./types";

export function dispatchTextChanged(root: HTMLElement, detail: EditorTextChangedDetail): void {
  root.dispatchEvent(
    new CustomEvent<EditorTextChangedDetail>("editor:text-changed", {
      bubbles: true,
      detail,
    }),
  );
}
