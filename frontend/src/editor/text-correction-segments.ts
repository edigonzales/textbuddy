const IMMEDIATE_TRIGGER_PATTERN = /[.\n]/u;

export interface TextCorrectionSegment {
  start: number;
  end: number;
  text: string;
}

export interface TextCorrectionSegmentDiff {
  previousSegments: TextCorrectionSegment[];
  nextSegments: TextCorrectionSegment[];
  unchangedPrefixCount: number;
  unchangedSuffixCount: number;
  changedNextSegments: TextCorrectionSegment[];
}

export function segmentTextForCorrection(text: string): TextCorrectionSegment[] {
  if (!text) {
    return [];
  }

  const segments: TextCorrectionSegment[] = [];
  let start = 0;

  for (let index = 0; index < text.length; index += 1) {
    const character = text[index];

    if (character !== "." && character !== "\n") {
      continue;
    }

    segments.push({
      start,
      end: index + 1,
      text: text.slice(start, index + 1),
    });
    start = index + 1;
  }

  if (start < text.length) {
    segments.push({
      start,
      end: text.length,
      text: text.slice(start),
    });
  }

  return segments;
}

export function diffTextCorrectionSegments(
  previousSegments: readonly TextCorrectionSegment[],
  nextSegments: readonly TextCorrectionSegment[],
): TextCorrectionSegmentDiff {
  let unchangedPrefixCount = 0;

  while (
    unchangedPrefixCount < previousSegments.length &&
    unchangedPrefixCount < nextSegments.length &&
    previousSegments[unchangedPrefixCount]?.text === nextSegments[unchangedPrefixCount]?.text
  ) {
    unchangedPrefixCount += 1;
  }

  let unchangedSuffixCount = 0;

  while (
    unchangedSuffixCount < previousSegments.length - unchangedPrefixCount &&
    unchangedSuffixCount < nextSegments.length - unchangedPrefixCount &&
    previousSegments[previousSegments.length - 1 - unchangedSuffixCount]?.text ===
      nextSegments[nextSegments.length - 1 - unchangedSuffixCount]?.text
  ) {
    unchangedSuffixCount += 1;
  }

  return {
    previousSegments: [...previousSegments],
    nextSegments: [...nextSegments],
    unchangedPrefixCount,
    unchangedSuffixCount,
    changedNextSegments: nextSegments.slice(
      unchangedPrefixCount,
      nextSegments.length - unchangedSuffixCount,
    ),
  };
}

function extractInsertedText(previousText: string, nextText: string): string {
  if (!nextText || previousText === nextText) {
    return "";
  }

  let prefixLength = 0;

  while (
    prefixLength < previousText.length &&
    prefixLength < nextText.length &&
    previousText[prefixLength] === nextText[prefixLength]
  ) {
    prefixLength += 1;
  }

  let previousSuffixIndex = previousText.length - 1;
  let nextSuffixIndex = nextText.length - 1;

  while (
    previousSuffixIndex >= prefixLength &&
    nextSuffixIndex >= prefixLength &&
    previousText[previousSuffixIndex] === nextText[nextSuffixIndex]
  ) {
    previousSuffixIndex -= 1;
    nextSuffixIndex -= 1;
  }

  return nextText.slice(prefixLength, nextSuffixIndex + 1);
}

export function shouldTriggerCorrectionImmediately(
  previousText: string,
  nextText: string,
): boolean {
  return IMMEDIATE_TRIGGER_PATTERN.test(extractInsertedText(previousText, nextText));
}
