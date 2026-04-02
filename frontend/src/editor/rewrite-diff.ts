import type { RewriteDiffToken, RewriteDiffView } from "./types";

const TOKEN_PATTERN = /\s+|[^\s]+/g;

function tokenize(text: string): string[] {
  return text.match(TOKEN_PATTERN) ?? [];
}

function createTokens(
  tokens: readonly string[],
  status: RewriteDiffToken["status"],
): RewriteDiffToken[] {
  return tokens.map((text) => ({
    text,
    status,
  }));
}

export function createRewriteDiff(previousText: string, nextText: string): RewriteDiffView {
  const previousTokens = tokenize(previousText);
  const nextTokens = tokenize(nextText);
  let unchangedPrefixCount = 0;

  while (
    unchangedPrefixCount < previousTokens.length &&
    unchangedPrefixCount < nextTokens.length &&
    previousTokens[unchangedPrefixCount] === nextTokens[unchangedPrefixCount]
  ) {
    unchangedPrefixCount += 1;
  }

  let unchangedSuffixCount = 0;

  while (
    unchangedSuffixCount + unchangedPrefixCount < previousTokens.length &&
    unchangedSuffixCount + unchangedPrefixCount < nextTokens.length &&
    previousTokens[previousTokens.length - 1 - unchangedSuffixCount] ===
      nextTokens[nextTokens.length - 1 - unchangedSuffixCount]
  ) {
    unchangedSuffixCount += 1;
  }

  const previousMiddle = previousTokens.slice(
    unchangedPrefixCount,
    previousTokens.length - unchangedSuffixCount,
  );
  const nextMiddle = nextTokens.slice(unchangedPrefixCount, nextTokens.length - unchangedSuffixCount);

  return {
    before: [
      ...createTokens(previousTokens.slice(0, unchangedPrefixCount), "unchanged"),
      ...createTokens(previousMiddle, "removed"),
      ...createTokens(
        previousTokens.slice(previousTokens.length - unchangedSuffixCount),
        "unchanged",
      ),
    ],
    after: [
      ...createTokens(nextTokens.slice(0, unchangedPrefixCount), "unchanged"),
      ...createTokens(nextMiddle, "added"),
      ...createTokens(nextTokens.slice(nextTokens.length - unchangedSuffixCount), "unchanged"),
    ],
    hasChanges: previousMiddle.length > 0 || nextMiddle.length > 0,
  };
}
