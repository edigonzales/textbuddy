import type { CorrectionElements, DocumentImportElements, EditorElements } from "./types";

function query<T extends Element>(parent: ParentNode, selector: string): T | null {
  return parent.querySelector<T>(selector);
}

export function findEditorElements(): EditorElements | null {
  const root = document.querySelector<HTMLElement>("#editor-island-root");
  if (!root) {
    return null;
  }

  const elements = {
    root,
    canvas: query<HTMLElement>(root, "[data-editor-canvas]"),
    surface: query<HTMLElement>(root, "[data-editor-surface]"),
    mirror: query<HTMLTextAreaElement>(root, "[data-editor-mirror]"),
    characterCount: query<HTMLElement>(root, "[data-editor-count='characters']"),
    wordCount: query<HTMLElement>(root, "[data-editor-count='words']"),
    undoButton: query<HTMLButtonElement>(root, "[data-editor-action='undo']"),
    redoButton: query<HTMLButtonElement>(root, "[data-editor-action='redo']"),
  };

  return Object.values(elements).every(Boolean) ? elements as EditorElements : null;
}

export function findCorrectionElements(): CorrectionElements | null {
  const panel = document.querySelector<HTMLElement>("[data-correction-panel]");
  if (!panel) {
    return null;
  }

  const elements = {
    panel,
    status: query<HTMLElement>(panel, "[data-correction-status]"),
    list: query<HTMLElement>(panel, "[data-correction-list]"),
    languageSelect: document.querySelector<HTMLSelectElement>("[data-workspace-language]"),
    dictionaryForm: query<HTMLFormElement>(panel, "[data-dictionary-form]"),
    dictionaryInput: query<HTMLInputElement>(panel, "[data-dictionary-input]"),
    dictionaryList: query<HTMLElement>(panel, "[data-dictionary-list]"),
    dictionaryEmpty: query<HTMLElement>(panel, "[data-dictionary-empty]"),
  };

  return Object.values(elements).every(Boolean) ? elements as CorrectionElements : null;
}

export function findDocumentImportElements(root: HTMLElement): DocumentImportElements | null {
  const input = query<HTMLInputElement>(root, "[data-document-import-input]");
  return input
    ? { input, labels: input.dataset.documentImportLabels ?? "" }
    : null;
}
