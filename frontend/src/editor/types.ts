export interface EditorTextChangedDetail {
  text: string;
  characters: number;
  words: number;
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

export interface DocumentImportElements {
  input: HTMLInputElement;
  labels: string;
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

export interface QuickActionResponse {
  text: string;
}

export interface RewriteDiffView {
  hasChanges: boolean;
  segments: RewriteDiffSegment[];
}

export type RewriteDiffHunkStatus = "pending" | "accepted" | "rejected";

export interface RewriteDiffHunk {
  key: string;
  removedText: string;
  addedText: string;
}

export type RewriteDiffSegment =
  | { kind: "text"; value: string }
  | { kind: "change"; hunk: RewriteDiffHunk };

export interface CorrectionStateChangedDetail {
  state: "idle" | "loading" | "success" | "error";
  message: string;
  count: number;
}

export interface WorkspaceBusyChangedDetail {
  busy: boolean;
  view: "editor" | "diff-review";
}
