import type { EditorTextChangedDetail, WorkspaceBusyChangedDetail } from "./types";

export function dispatchTextChanged(root: HTMLElement, detail: EditorTextChangedDetail): void {
  root.dispatchEvent(
    new CustomEvent<EditorTextChangedDetail>("editor:text-changed", {
      bubbles: true,
      detail,
    }),
  );
}

export function dispatchWorkspaceBusy(
  root: HTMLElement,
  busy: boolean,
  view: WorkspaceBusyChangedDetail["view"],
  source = "",
): void {
  root.dataset.workspaceBusy = busy ? "true" : "false";
  root.dataset.workspaceBusySource = busy ? source : "";
  root.dispatchEvent(
    new CustomEvent<WorkspaceBusyChangedDetail>("workspace:busy-changed", {
      bubbles: true,
      detail: { busy, view },
    }),
  );
}
