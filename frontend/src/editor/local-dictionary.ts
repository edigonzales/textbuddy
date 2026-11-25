import type { TextCorrectionBlock } from "./types";

const DICTIONARY_STORAGE_KEY = "textbuddy.local-dictionary";
const DICTIONARY_WORD_PATTERN = /^[\p{L}\p{M}\p{N}'’-]+$/u;

export function normalizeDictionaryWord(word: string): string {
  return word.trim().toLocaleLowerCase();
}

export function isDictionaryWord(word: string): boolean {
  const normalized = normalizeDictionaryWord(word);
  return normalized.length > 0 && DICTIONARY_WORD_PATTERN.test(normalized);
}

export function filterCorrectionBlocksByDictionary(
  original: string,
  blocks: readonly TextCorrectionBlock[],
  dictionaryWords: ReadonlySet<string>,
): TextCorrectionBlock[] {
  return blocks.filter((block) => {
    const fragment = original.slice(block.offset, block.offset + block.length);
    return !dictionaryWords.has(normalizeDictionaryWord(fragment));
  });
}

function sortDictionaryWords(words: readonly string[]): string[] {
  return Array.from(
    new Set(words.map(normalizeDictionaryWord).filter((word) => isDictionaryWord(word))),
  ).sort((left, right) => left.localeCompare(right));
}

export function loadDictionaryWords(): string[] {
  try {
    const parsed: unknown = JSON.parse(window.localStorage.getItem(DICTIONARY_STORAGE_KEY) ?? "null");
    return Array.isArray(parsed) ? sortDictionaryWords(parsed.filter((value) => typeof value === "string")) : [];
  } catch {
    return [];
  }
}

export function saveDictionaryWords(words: readonly string[]): void {
  try {
    window.localStorage.setItem(DICTIONARY_STORAGE_KEY, JSON.stringify(sortDictionaryWords(words)));
  } catch {
    // Storage may be unavailable in restricted browser contexts.
  }
}
