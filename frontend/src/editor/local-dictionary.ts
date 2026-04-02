import type { TextCorrectionBlock } from "./types";

const DICTIONARY_DATABASE_NAME = "textbuddy-local-dictionary";
const DICTIONARY_STORE_NAME = "entries";
const DICTIONARY_RECORD_ID = "words";
const DICTIONARY_STORAGE_KEY = "textbuddy.local-dictionary";
const DICTIONARY_WORD_PATTERN = /^[\p{L}\p{M}\p{N}'’-]+$/u;

interface DictionaryRecord {
  id: string;
  words: string[];
}

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

    window.localStorage.setItem(
      DICTIONARY_STORAGE_KEY,
      JSON.stringify(sortDictionaryWords(words)),
    );
  }
}

class IndexedDbDictionaryStore implements LocalDictionaryStore {
  private readonly fallback = new LocalStorageDictionaryStore();

  async load(): Promise<string[]> {
    try {
      const database = await this.openDatabase();
      const record = await new Promise<DictionaryRecord | undefined>((resolve, reject) => {
        const transaction = database.transaction(DICTIONARY_STORE_NAME, "readonly");
        const store = transaction.objectStore(DICTIONARY_STORE_NAME);
        const request = store.get(DICTIONARY_RECORD_ID);

        request.onsuccess = () => {
          resolve(request.result as DictionaryRecord | undefined);
        };
        request.onerror = () => {
          reject(request.error ?? new Error("Dictionary lookup failed."));
        };
      });

      return sortDictionaryWords(record?.words ?? []);
    } catch {
      return this.fallback.load();
    }
  }

  async save(words: readonly string[]): Promise<void> {
    const normalizedWords = sortDictionaryWords(words);

    try {
      const database = await this.openDatabase();

      await new Promise<void>((resolve, reject) => {
        const transaction = database.transaction(DICTIONARY_STORE_NAME, "readwrite");

        transaction.oncomplete = () => {
          resolve();
        };
        transaction.onerror = () => {
          reject(transaction.error ?? new Error("Dictionary save failed."));
        };

        transaction.objectStore(DICTIONARY_STORE_NAME).put({
          id: DICTIONARY_RECORD_ID,
          words: normalizedWords,
        } satisfies DictionaryRecord);
      });
    } catch {
      await this.fallback.save(normalizedWords);
    }
  }

  private openDatabase(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      if (typeof window === "undefined" || typeof window.indexedDB === "undefined") {
        reject(new Error("IndexedDB is not available."));
        return;
      }

      const request = window.indexedDB.open(DICTIONARY_DATABASE_NAME, 1);

      request.onupgradeneeded = () => {
        const database = request.result;

        if (!database.objectStoreNames.contains(DICTIONARY_STORE_NAME)) {
          database.createObjectStore(DICTIONARY_STORE_NAME, {
            keyPath: "id",
          });
        }
      };

      request.onsuccess = () => {
        resolve(request.result);
      };
      request.onerror = () => {
        reject(request.error ?? new Error("IndexedDB open failed."));
      };
    });
  }
}

function isString(value: unknown): value is string {
  return typeof value === "string";
}

export function createLocalDictionaryStore(): LocalDictionaryStore {
  if (typeof window !== "undefined" && typeof window.indexedDB !== "undefined") {
    return new IndexedDbDictionaryStore();
  }

  return new LocalStorageDictionaryStore();
}
