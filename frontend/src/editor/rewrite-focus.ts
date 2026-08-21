import {
  findFocusedSentence,
  type SentenceFocus,
} from "./sentence-focus";
import {
  findFocusedWord,
  type WordFocus,
} from "./word-focus";

export type RewriteBubbleState =
  | { mode: "hidden" }
  | { mode: "word"; word: WordFocus; sentence: SentenceFocus | null }
  | { mode: "sentence"; sentence: SentenceFocus };

export function resolveRewriteBubbleState(
  text: string,
  selectionStart: number,
  selectionEnd: number,
): RewriteBubbleState {
  const start = Math.max(0, Math.min(selectionStart, text.length));
  const end = Math.max(0, Math.min(selectionEnd, text.length));

  if (start === end || text.slice(Math.min(start, end), Math.max(start, end)).trim().length === 0) {
    return {
      mode: "hidden",
    };
  }

  const word = findFocusedWord(text, selectionStart, selectionEnd);

  if (word) {
    return {
      mode: "word",
      word,
      sentence: findFocusedSentence(text, selectionStart, selectionEnd),
    };
  }

  const sentence = findFocusedSentence(text, selectionStart, selectionEnd);

  if (sentence) {
    return {
      mode: "sentence",
      sentence,
    };
  }

  return {
    mode: "hidden",
  };
}
