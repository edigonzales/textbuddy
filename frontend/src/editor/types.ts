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
  surface: HTMLElement;
  mirror: HTMLTextAreaElement;
  characterCount: HTMLElement;
  wordCount: HTMLElement;
  undoButton: HTMLButtonElement;
  redoButton: HTMLButtonElement;
}
