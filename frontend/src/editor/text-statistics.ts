export interface TextStatistics {
  characters: number;
  words: number;
  syllables: number;
  sentences: number;
  averageSentenceLength: number;
  averageSyllablesPerWord: number;
  fleschScore: number;
}

const WORD_REGEX = /\p{L}+(?:['’-]\p{L}+)*/gu;
const WORD_STRIP_REGEX = /[^\p{L}'’-]+/gu;
const VOWEL_GROUP_REGEX = /[aeiouyäöüy]+/gu;

function normalizeText(value: string): string {
  return value.replace(/\r\n/g, "\n").replace(/\r/g, "\n");
}

export function splitWords(text: string): string[] {
  const normalized = normalizeText(text);
  const words = normalized.match(WORD_REGEX);

  return words ? words : [];
}

export function countSyllablesInWord(word: string): number {
  const normalized = word
    .toLowerCase()
    .normalize("NFC")
    .replace(WORD_STRIP_REGEX, "")
    .trim();

  if (normalized.length === 0) {
    return 0;
  }

  const groups = normalized.match(VOWEL_GROUP_REGEX);

  if (!groups || groups.length === 0) {
    return 1;
  }

  return groups.length;
}

function countSentences(text: string, wordCount: number): number {
  const normalized = normalizeText(text).trim();

  if (normalized.length === 0) {
    return 0;
  }

  const segments = normalized
    .split(/(?<=[.!?])\s+|\n+/u)
    .map((segment) => segment.trim())
    .filter((segment) => segment.length > 0);

  if (segments.length === 0) {
    return wordCount > 0 ? 1 : 0;
  }

  return segments.length;
}

function round(value: number, digits: number): number {
  const factor = 10 ** digits;
  return Math.round(value * factor) / factor;
}

export function calculateTextStatistics(text: string): TextStatistics {
  const normalized = normalizeText(text);
  const words = splitWords(normalized);
  const wordCount = words.length;
  const syllableCount = words.reduce((sum, word) => sum + countSyllablesInWord(word), 0);
  const sentenceCount = countSentences(normalized, wordCount);
  const averageSentenceLength = sentenceCount > 0 ? wordCount / sentenceCount : 0;
  const averageSyllablesPerWord = wordCount > 0 ? syllableCount / wordCount : 0;
  const fleschScore = 180 - averageSentenceLength - 58.5 * averageSyllablesPerWord;

  return {
    characters: normalized.length,
    words: wordCount,
    syllables: syllableCount,
    sentences: sentenceCount,
    averageSentenceLength: round(averageSentenceLength, 2),
    averageSyllablesPerWord: round(averageSyllablesPerWord, 2),
    fleschScore: round(fleschScore, 1),
  };
}

export function describeFleschScore(score: number, hasWords: boolean): string {
  if (!hasWords) {
    return "Keine Bewertung";
  }

  if (score >= 80) {
    return "Sehr leicht verständlich";
  }

  if (score >= 60) {
    return "Leicht verständlich";
  }

  if (score >= 40) {
    return "Mittel verständlich";
  }

  if (score >= 20) {
    return "Anspruchsvoll";
  }

  return "Sehr anspruchsvoll";
}
