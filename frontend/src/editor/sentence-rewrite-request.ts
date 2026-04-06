export interface SentenceRewritePayload {
  sentence: string;
  context?: string;
}

function normalize(value: string | null | undefined): string {
  return (value ?? "").trim();
}

function isBlankLine(line: string): boolean {
  return line.trim().length === 0;
}

export function resolveSentenceRewriteContext(
  text: string,
  sentenceStart: number,
  sentenceEnd: number,
): string {
  const before = text.slice(0, Math.max(0, sentenceStart));
  const after = text.slice(Math.max(sentenceEnd, 0));
  const lines = text.split(/\n/u);

  let startOffset = 0;
  let sentenceLineIndex = 0;

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index] ?? "";
    const nextOffset = startOffset + line.length + (index < lines.length - 1 ? 1 : 0);

    if (sentenceStart >= startOffset && sentenceStart <= nextOffset) {
      sentenceLineIndex = index;
      break;
    }

    startOffset = nextOffset;
  }

  let paragraphStart = sentenceLineIndex;
  let paragraphEnd = sentenceLineIndex;

  while (paragraphStart > 0 && !isBlankLine(lines[paragraphStart - 1] ?? "")) {
    paragraphStart -= 1;
  }

  while (paragraphEnd < lines.length - 1 && !isBlankLine(lines[paragraphEnd + 1] ?? "")) {
    paragraphEnd += 1;
  }

  const paragraph = lines.slice(paragraphStart, paragraphEnd + 1).join("\n");
  const normalized = normalize(paragraph);

  if (normalized.length > 0) {
    return normalized;
  }

  const fallback = normalize(before + text.slice(sentenceStart, sentenceEnd) + after);
  return fallback;
}

export function buildSentenceRewritePayload(
  sentence: string,
  context: string | null | undefined,
): SentenceRewritePayload {
  const normalizedSentence = normalize(sentence);
  const normalizedContext = normalize(context);

  if (normalizedContext.length === 0) {
    return {
      sentence: normalizedSentence,
    };
  }

  return {
    sentence: normalizedSentence,
    context: normalizedContext,
  };
}
