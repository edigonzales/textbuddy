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
  return /[.\n]/u.test(extractInsertedText(previousText, nextText));
}
