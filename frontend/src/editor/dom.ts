import type { EditorElements } from "./types";

function queryRequired<T extends Element>(parent: ParentNode, selector: string): T | null {
  return parent.querySelector<T>(selector);
}

export function findEditorElements(): EditorElements | null {
  const root = document.querySelector<HTMLElement>("#editor-island-root");

  if (!root) {
    return null;
  }

  const surface = queryRequired<HTMLElement>(root, "[data-editor-surface]");
  const mirror = queryRequired<HTMLTextAreaElement>(root, "[data-editor-mirror]");
  const characterCount = queryRequired<HTMLElement>(root, "[data-editor-count='characters']");
  const wordCount = queryRequired<HTMLElement>(root, "[data-editor-count='words']");
  const undoButton = queryRequired<HTMLButtonElement>(root, "[data-editor-action='undo']");
  const redoButton = queryRequired<HTMLButtonElement>(root, "[data-editor-action='redo']");

  if (!surface || !mirror || !characterCount || !wordCount || !undoButton || !redoButton) {
    return null;
  }

  return {
    root,
    surface,
    mirror,
    characterCount,
    wordCount,
    undoButton,
    redoButton,
  };
}
