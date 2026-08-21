import type { TextCorrectionBlock } from "./types";

const DICTIONARY_STORAGE_KEY = "textbuddy.local-dictionary";
const DICTIONARY_WORD_PATTERN = /^[\p{L}\p{M}\p{N}'’-]+$/u;

export interface LocalDictionaryStore {
  load(): Promise<string[]>;
  save(words: readonly string[]): Promise<void>;
}

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
  if (dictionaryWords.size === 0) {
    return blocks.map((block) => ({
      ...block,
      replacements: [...block.replacements],
    }));
  }

  return blocks
    .filter((block) => {
      const fragment = original.slice(block.offset, block.offset + block.length);

      if (!isDictionaryWord(fragment)) {
        return true;
      }

      return !dictionaryWords.has(normalizeDictionaryWord(fragment));
    })
    .map((block) => ({
      ...block,
      replacements: [...block.replacements],
    }));
}

function sortDictionaryWords(words: readonly string[]): string[] {
  return Array.from(
    new Set(words.map(normalizeDictionaryWord).filter((word) => isDictionaryWord(word))),
  ).sort((left, right) => left.localeCompare(right));
}

class LocalStorageDictionaryStore implements LocalDictionaryStore {
  async load(): Promise<string[]> {
    if (typeof window === "undefined") {
      return [];
    }

    try {
      const raw = window.localStorage.getItem(DICTIONARY_STORAGE_KEY);

      if (!raw) {
        return [];
      }

      const parsed = JSON.parse(raw) as unknown;
      return Array.isArray(parsed) ? sortDictionaryWords(parsed.filter(isString)) : [];
    } catch {
      return [];
    }
  }

  async save(words: readonly string[]): Promise<void> {
    if (typeof window === "undefined") {
      return;
    }

    try {
      window.localStorage.setItem(
        DICTIONARY_STORAGE_KEY,
        JSON.stringify(sortDictionaryWords(words)),
      );
    } catch {
      // Private browsing or storage quotas may make localStorage unavailable.
    }
  }
}

function isString(value: unknown): value is string {
  return typeof value === "string";
}

export function createLocalDictionaryStore(): LocalDictionaryStore {
  return new LocalStorageDictionaryStore();
}
