import type {
  AdvisorValidationElements,
  CorrectionElements,
  DocumentImportElements,
  EditorElements,
  QuickActionElements,
  RewriteBubbleElements,
} from "./types";

function queryRequired<T extends Element>(parent: ParentNode, selector: string): T | null {
  return parent.querySelector<T>(selector);
}

export function findEditorElements(): EditorElements | null {
  const root = document.querySelector<HTMLElement>("#editor-island-root");

  if (!root) {
    return null;
  }

  const canvas = queryRequired<HTMLElement>(root, "[data-editor-canvas]");
  const surface = queryRequired<HTMLElement>(root, "[data-editor-surface]");
  const mirror = queryRequired<HTMLTextAreaElement>(root, "[data-editor-mirror]");
  const characterCount = queryRequired<HTMLElement>(root, "[data-editor-count='characters']");
  const wordCount = queryRequired<HTMLElement>(root, "[data-editor-count='words']");
  const undoButton = queryRequired<HTMLButtonElement>(root, "[data-editor-action='undo']");
  const redoButton = queryRequired<HTMLButtonElement>(root, "[data-editor-action='redo']");

  if (!canvas || !surface || !mirror || !characterCount || !wordCount || !undoButton || !redoButton) {
    return null;
  }

  return {
    root,
    canvas,
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

export function findDocumentImportElements(root: HTMLElement): DocumentImportElements | null {
  const panel = queryRequired<HTMLElement>(root, "[data-document-import-panel]");

  if (!panel) {
    return null;
  }

  const status = queryRequired<HTMLElement>(panel, "[data-document-import-status]");
  const dropzone = queryRequired<HTMLElement>(panel, "[data-document-import-dropzone]");
  const button = queryRequired<HTMLButtonElement>(panel, "[data-document-import-button]");
  const input = queryRequired<HTMLInputElement>(panel, "[data-document-import-input]");

  if (!status || !dropzone || !button || !input) {
    return null;
  }

  return {
    panel,
    status,
    dropzone,
    button,
    input,
    labels: panel.dataset.documentImportLabels ?? "",
  };
}

export function findAdvisorValidationElements(): AdvisorValidationElements | null {
  const panel = document.querySelector<HTMLElement>("[data-advisor-panel]");

  if (!panel) {
    return null;
  }

  const status = queryRequired<HTMLElement>(panel, "[data-advisor-status]");
  const validateButton = queryRequired<HTMLButtonElement>(panel, "[data-advisor-validate]");
  const resultCount = queryRequired<HTMLElement>(panel, "[data-advisor-result-count]");
  const resultList = queryRequired<HTMLElement>(panel, "[data-advisor-result-list]");
  const resultEmpty = queryRequired<HTMLElement>(panel, "[data-advisor-result-empty]");
  const detailPanel = queryRequired<HTMLElement>(panel, "[data-advisor-result-detail]");
  const detailTitle = queryRequired<HTMLElement>(panel, "[data-advisor-result-detail-title]");
  const detailReference = queryRequired<HTMLElement>(panel, "[data-advisor-result-detail-reference]");
  const detailMatch = queryRequired<HTMLElement>(panel, "[data-advisor-result-detail-match]");
  const detailMessage = queryRequired<HTMLElement>(panel, "[data-advisor-result-detail-message]");
  const detailExcerpt = queryRequired<HTMLElement>(panel, "[data-advisor-result-detail-excerpt]");
  const detailSuggestion = queryRequired<HTMLElement>(panel, "[data-advisor-result-detail-suggestion]");
  const detailLink = queryRequired<HTMLAnchorElement>(panel, "[data-advisor-result-detail-link]");
  const docCheckboxes = Array.from(
    panel.querySelectorAll<HTMLInputElement>("[data-testid='advisor-doc-checkbox']"),
  );

  if (
    !status ||
    !validateButton ||
    !resultCount ||
    !resultList ||
    !resultEmpty ||
    !detailPanel ||
    !detailTitle ||
    !detailReference ||
    !detailMatch ||
    !detailMessage ||
    !detailExcerpt ||
    !detailSuggestion ||
    !detailLink ||
    docCheckboxes.length === 0
  ) {
    return null;
  }

  return {
    panel,
    status,
    validateButton,
    docCheckboxes,
    resultCount,
    resultList,
    resultEmpty,
    detailPanel,
    detailTitle,
    detailReference,
    detailMatch,
    detailMessage,
    detailExcerpt,
    detailSuggestion,
    detailLink,
  };
}

export function findRewriteBubbleElements(root: HTMLElement): RewriteBubbleElements | null {
  const bubble = queryRequired<HTMLElement>(root, "[data-rewrite-bubble]");
  const focus = queryRequired<HTMLElement>(root, "[data-rewrite-focus]");
  const primaryAction = queryRequired<HTMLButtonElement>(root, "[data-rewrite-primary-action]");
  const secondaryAction = queryRequired<HTMLButtonElement>(root, "[data-rewrite-secondary-action]");
  const overlay = queryRequired<HTMLElement>(root, "[data-rewrite-overlay]");
  const status = queryRequired<HTMLElement>(root, "[data-rewrite-status]");
  const options = queryRequired<HTMLElement>(root, "[data-rewrite-options]");

  if (!bubble || !focus || !primaryAction || !secondaryAction || !overlay || !status || !options) {
    return null;
  }

  return {
    bubble,
    focus,
    primaryAction,
    secondaryAction,
    overlay,
    status,
    options,
  };
}

export function findQuickActionElements(root: HTMLElement): QuickActionElements | null {
  const panel = queryRequired<HTMLElement>(root, "[data-quick-action-panel]");
  const status = queryRequired<HTMLElement>(root, "[data-quick-action-status]");
  const plainLanguageButton = queryRequired<HTMLButtonElement>(
    root,
    "[data-quick-action='plain-language']",
  );
  const bulletPointsButton = queryRequired<HTMLButtonElement>(
    root,
    "[data-quick-action='bullet-points']",
  );
  const proofreadButton = queryRequired<HTMLButtonElement>(
    root,
    "[data-quick-action='proofread']",
  );
  const summarizeButton = queryRequired<HTMLButtonElement>(
    root,
    "[data-quick-action='summarize']",
  );
  const summarizeOptionSelect = queryRequired<HTMLSelectElement>(
    root,
    "[data-quick-action-option='summarize']",
  );
  const formalityButton = queryRequired<HTMLButtonElement>(root, "[data-quick-action='formality']");
  const formalityOptionSelect = queryRequired<HTMLSelectElement>(
    root,
    "[data-quick-action-option='formality']",
  );
  const socialMediaButton = queryRequired<HTMLButtonElement>(
    root,
    "[data-quick-action='social-media']",
  );
  const socialMediaOptionSelect = queryRequired<HTMLSelectElement>(
    root,
    "[data-quick-action-option='social-media']",
  );
  const mediumButton = queryRequired<HTMLButtonElement>(root, "[data-quick-action='medium']");
  const mediumOptionSelect = queryRequired<HTMLSelectElement>(
    root,
    "[data-quick-action-option='medium']",
  );
  const characterSpeechButton = queryRequired<HTMLButtonElement>(
    root,
    "[data-quick-action='character-speech']",
  );
  const characterSpeechOptionSelect = queryRequired<HTMLSelectElement>(
    root,
    "[data-quick-action-option='character-speech']",
  );
  const customButton = queryRequired<HTMLButtonElement>(root, "[data-quick-action='custom']");
  const customPromptInput = queryRequired<HTMLTextAreaElement>(
    root,
    "[data-quick-action-prompt='custom']",
  );
  const diffPanel = queryRequired<HTMLElement>(root, "[data-rewrite-diff-panel]");
  const diffBefore = queryRequired<HTMLElement>(root, "[data-rewrite-diff-before]");
  const diffAfter = queryRequired<HTMLElement>(root, "[data-rewrite-diff-after]");
  const diffUndoButton = queryRequired<HTMLButtonElement>(root, "[data-rewrite-diff-undo]");

  if (
    !panel ||
    !status ||
    !plainLanguageButton ||
    !bulletPointsButton ||
    !proofreadButton ||
    !summarizeButton ||
    !summarizeOptionSelect ||
    !formalityButton ||
    !formalityOptionSelect ||
    !socialMediaButton ||
    !socialMediaOptionSelect ||
    !mediumButton ||
    !mediumOptionSelect ||
    !characterSpeechButton ||
    !characterSpeechOptionSelect ||
    !customButton ||
    !customPromptInput ||
    !diffPanel ||
    !diffBefore ||
    !diffAfter ||
    !diffUndoButton
  ) {
    return null;
  }

  return {
    panel,
    status,
    plainLanguageButton,
    bulletPointsButton,
    proofreadButton,
    summarizeButton,
    summarizeOptionSelect,
    formalityButton,
    formalityOptionSelect,
    socialMediaButton,
    socialMediaOptionSelect,
    mediumButton,
    mediumOptionSelect,
    characterSpeechButton,
    characterSpeechOptionSelect,
    customButton,
    customPromptInput,
    diffPanel,
    diffBefore,
    diffAfter,
    diffUndoButton,
  };
}
