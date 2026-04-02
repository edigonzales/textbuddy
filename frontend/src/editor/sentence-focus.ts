export interface SentenceFocus {
  start: number;
  end: number;
  text: string;
}

function clampOffset(offset: number, textLength: number): number {
  return Math.max(0, Math.min(offset, textLength));
}

function isWhitespace(character: string): boolean {
  return /\s/u.test(character);
}

function isSentenceTerminal(character: string): boolean {
  return character === "." || character === "!" || character === "?";
}

function isCompleteSentence(text: string, start: number, end: number): boolean {
  if (end <= start) {
    return false;
  }

  return isSentenceTerminal(text[end - 1] ?? "");
}

export function collectSentenceFocusRanges(text: string): SentenceFocus[] {
  const ranges: SentenceFocus[] = [];
  let index = 0;

  while (index < text.length) {
    while (index < text.length && isWhitespace(text[index] ?? "")) {
      index += 1;
    }

    if (index >= text.length) {
      break;
    }

    const start = index;
    let end = index;

    while (end < text.length) {
      const current = text[end] ?? "";

      if (current === "\n") {
        break;
      }

      end += 1;

      if (isSentenceTerminal(current)) {
        break;
      }
    }

    while (end > start && isWhitespace(text[end - 1] ?? "")) {
      end -= 1;
    }

    if (isCompleteSentence(text, start, end)) {
      ranges.push({
        start,
        end,
        text: text.slice(start, end),
      });
    }

    index = end;
  }

  return ranges;
}

export function findFocusedSentence(
  text: string,
  selectionStart: number,
  selectionEnd: number,
): SentenceFocus | null {
  const sentences = collectSentenceFocusRanges(text);

  if (sentences.length === 0) {
    return null;
  }

  const start = clampOffset(selectionStart, text.length);
  const end = clampOffset(selectionEnd, text.length);

  if (start !== end) {
    return (
      sentences.find((sentence) => start >= sentence.start && end <= sentence.end) ?? null
    );
  }

  const candidateIndexes: number[] = [];

  if (start < text.length && !isWhitespace(text[start] ?? "")) {
    candidateIndexes.push(start);
  }

  if (start > 0 && !isWhitespace(text[start - 1] ?? "")) {
    candidateIndexes.push(start - 1);
  }

  for (const candidateIndex of candidateIndexes) {
    const focusedSentence = sentences.find(
      (sentence) => candidateIndex >= sentence.start && candidateIndex < sentence.end,
    );

    if (focusedSentence) {
      return focusedSentence;
    }
  }

  return null;
}
