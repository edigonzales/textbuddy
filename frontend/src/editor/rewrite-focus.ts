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
