export interface WordFocus {
  start: number;
  end: number;
  text: string;
  context: string;
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

function isWordCharacter(character: string): boolean {
  return /[\p{L}\p{N}\p{M}'’-]/u.test(character);
}

function expandWordStart(text: string, index: number): number {
  let cursor = index;

  while (cursor > 0 && isWordCharacter(text[cursor - 1] ?? "")) {
    cursor -= 1;
  }

  return cursor;
}

function expandWordEnd(text: string, index: number): number {
  let cursor = index;

  while (cursor < text.length && isWordCharacter(text[cursor] ?? "")) {
    cursor += 1;
  }

  return cursor;
}

function findContextStart(text: string, wordStart: number): number {
  let cursor = wordStart;

  while (cursor > 0) {
    const previous = text[cursor - 1] ?? "";

    if (previous === "\n" || isSentenceTerminal(previous)) {
      break;
    }

    cursor -= 1;
  }

  while (cursor < wordStart && isWhitespace(text[cursor] ?? "")) {
    cursor += 1;
  }

  return cursor;
}

function findContextEnd(text: string, wordEnd: number): number {
  let cursor = wordEnd;

  while (cursor < text.length) {
    const current = text[cursor] ?? "";

    if (current === "\n") {
      break;
    }

    cursor += 1;

    if (isSentenceTerminal(current)) {
      break;
    }
  }

  while (cursor > wordEnd && isWhitespace(text[cursor - 1] ?? "")) {
    cursor -= 1;
  }

  return cursor;
}

function resolveWordFocus(text: string, anchorIndex: number): WordFocus | null {
  if (!isWordCharacter(text[anchorIndex] ?? "")) {
    return null;
  }

  const start = expandWordStart(text, anchorIndex);
  const end = expandWordEnd(text, anchorIndex);
  const contextStart = findContextStart(text, start);
  const contextEnd = findContextEnd(text, end);
  const word = text.slice(start, end);

  if (!word) {
    return null;
  }

  return {
    start,
    end,
    text: word,
    context: text.slice(contextStart, contextEnd),
  };
}

export function findFocusedWord(
  text: string,
  selectionStart: number,
  selectionEnd: number,
): WordFocus | null {
  if (!text) {
    return null;
  }

  const start = clampOffset(selectionStart, text.length);
  const end = clampOffset(selectionEnd, text.length);

  if (start !== end) {
    const focus = resolveWordFocus(text, start);

    if (!focus) {
      return null;
    }

    return start >= focus.start && end <= focus.end ? focus : null;
  }

  const candidateIndexes: number[] = [];

  if (start < text.length && isWordCharacter(text[start] ?? "")) {
    candidateIndexes.push(start);
  }

  if (start > 0 && isWordCharacter(text[start - 1] ?? "")) {
    candidateIndexes.push(start - 1);
  }

  for (const candidateIndex of candidateIndexes) {
    const focus = resolveWordFocus(text, candidateIndex);

    if (focus) {
      return focus;
    }
  }

  return null;
}
