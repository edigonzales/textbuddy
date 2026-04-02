import type { CorrectionElements, EditorElements } from "./types";

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

export function findCorrectionElements(): CorrectionElements | null {
  const panel = document.querySelector<HTMLElement>("[data-correction-panel]");

  if (!panel) {
    return null;
  }

  const status = queryRequired<HTMLElement>(panel, "[data-correction-status]");
  const list = queryRequired<HTMLElement>(panel, "[data-correction-list]");
  const languageSelect = queryRequired<HTMLSelectElement>(panel, "[data-correction-language]");
  const dictionaryForm = queryRequired<HTMLFormElement>(panel, "[data-dictionary-form]");
  const dictionaryInput = queryRequired<HTMLInputElement>(panel, "[data-dictionary-input]");
  const dictionaryList = queryRequired<HTMLElement>(panel, "[data-dictionary-list]");
  const dictionaryEmpty = queryRequired<HTMLElement>(panel, "[data-dictionary-empty]");

  if (
    !status ||
    !list ||
    !languageSelect ||
    !dictionaryForm ||
    !dictionaryInput ||
    !dictionaryList ||
    !dictionaryEmpty
  ) {
    return null;
  }

  return {
    panel,
    status,
    list,
    languageSelect,
    dictionaryForm,
    dictionaryInput,
    dictionaryList,
    dictionaryEmpty,
  };
}
