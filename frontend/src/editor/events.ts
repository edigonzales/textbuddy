import type {
  EditorSelectionChangedDetail,
  EditorTextChangedDetail,
} from "./types";

export function dispatchTextChanged(root: HTMLElement, detail: EditorTextChangedDetail): void {
  root.dispatchEvent(
    new CustomEvent<EditorTextChangedDetail>("editor:text-changed", {
      bubbles: true,
      detail,
    }),
  );
}

export function dispatchSelectionChanged(
  root: HTMLElement,
  detail: EditorSelectionChangedDetail,
): void {
  root.dispatchEvent(
    new CustomEvent<EditorSelectionChangedDetail>("editor:selection-changed", {
      bubbles: true,
      detail,
    }),
  );
}
