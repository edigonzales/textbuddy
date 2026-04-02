export interface EditorTextChangedDetail {
  text: string;
  characters: number;
  words: number;
}

export interface EditorSelectionChangedDetail {
  from: number;
  to: number;
  empty: boolean;
}

export interface EditorElements {
  root: HTMLElement;
  canvas: HTMLElement;
  surface: HTMLElement;
  mirror: HTMLTextAreaElement;
  characterCount: HTMLElement;
  wordCount: HTMLElement;
  undoButton: HTMLButtonElement;
  redoButton: HTMLButtonElement;
}

export interface RewriteBubbleElements {
  bubble: HTMLElement;
  focus: HTMLElement;
  primaryAction: HTMLButtonElement;
  secondaryAction: HTMLButtonElement;
  overlay: HTMLElement;
  status: HTMLElement;
  options: HTMLElement;
}

export interface CorrectionElements {
  panel: HTMLElement;
  status: HTMLElement;
  list: HTMLElement;
  languageSelect: HTMLSelectElement;
  dictionaryForm: HTMLFormElement;
  dictionaryInput: HTMLInputElement;
  dictionaryList: HTMLElement;
  dictionaryEmpty: HTMLElement;
}

export interface CorrectionRange {
  offset: number;
  length: number;
}

export interface TextCorrectionBlock {
  offset: number;
  length: number;
  message: string;
  shortMessage: string;
  ruleId: string;
  replacements: string[];
}

export interface TextCorrectionResponse {
  original: string;
  blocks: TextCorrectionBlock[];
}

export interface SentenceRewriteResponse {
  original: string;
  alternatives: string[];
}

export interface WordSynonymResponse {
  synonyms: string[];
}
