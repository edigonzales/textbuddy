import { diffWordsWithSpace } from "diff";

import type {
  RewriteDiffHunk,
  RewriteDiffHunkStatus,
  RewriteDiffSegment,
  RewriteDiffToken,
  RewriteDiffView,
} from "./types";

function isWhitespaceOnlyChange(removedText: string, addedText: string): boolean {
  return removedText.replace(/\s/g, "") === addedText.replace(/\s/g, "");
}

function groupSegments(raw: RewriteDiffSegment[]): RewriteDiffSegment[] {
  const grouped: RewriteDiffSegment[] = [];
  let index = 0;

  while (index < raw.length) {
    const segment = raw[index];

    if (!segment || segment.kind !== "change") {
      if (segment) {
        grouped.push(segment);
      }
      index += 1;
      continue;
    }

    let removedText = segment.hunk.removedText;
    let addedText = segment.hunk.addedText;
    let cursor = index + 1;

    for (;;) {
      const next = raw[cursor];

      if (next?.kind === "change") {
        removedText += next.hunk.removedText;
        addedText += next.hunk.addedText;
        cursor += 1;
        continue;
      }

      const afterGap = raw[cursor + 1];

      if (
        next?.kind === "text" &&
        /^\s*$/.test(next.value) &&
        afterGap?.kind === "change"
      ) {
        removedText += next.value + afterGap.hunk.removedText;
        addedText += next.value + afterGap.hunk.addedText;
        cursor += 2;
        continue;
      }

      break;
    }

    grouped.push({
      kind: "change",
      hunk: {
        key: `${index}:${removedText}:${addedText}`,
        removedText,
        addedText,
        status: "pending",
      },
    });
    index = cursor;
  }

  return grouped;
}

function suppressWhitespaceOnlyChanges(segments: RewriteDiffSegment[]): RewriteDiffSegment[] {
  const result: RewriteDiffSegment[] = [];

  for (const segment of segments) {
    if (
      segment.kind === "change" &&
      isWhitespaceOnlyChange(segment.hunk.removedText, segment.hunk.addedText)
    ) {
      const value = segment.hunk.addedText;
      const previous = result.at(-1);

      if (previous?.kind === "text") {
        previous.value += value;
      } else if (value) {
        result.push({ kind: "text", value });
      }
      continue;
    }

    result.push(segment);
  }

  return result;
}

export function buildRewriteDiffSegments(
  previousText: string,
  nextText: string,
): RewriteDiffSegment[] {
  const raw: RewriteDiffSegment[] = [];
  let pendingRemoved = "";

  for (const part of diffWordsWithSpace(previousText, nextText)) {
    if (part.removed) {
      pendingRemoved += part.value;
      continue;
    }

    if (part.added) {
      raw.push({
        kind: "change",
        hunk: {
          key: `${raw.length}:${pendingRemoved}:${part.value}`,
          removedText: pendingRemoved,
          addedText: part.value,
          status: "pending",
        },
      });
      pendingRemoved = "";
      continue;
    }

    if (pendingRemoved) {
      raw.push({
        kind: "change",
        hunk: {
          key: `${raw.length}:${pendingRemoved}:`,
          removedText: pendingRemoved,
          addedText: "",
          status: "pending",
        },
      });
      pendingRemoved = "";
    }

    raw.push({ kind: "text", value: part.value });
  }

  if (pendingRemoved) {
    raw.push({
      kind: "change",
      hunk: {
        key: `${raw.length}:${pendingRemoved}:`,
        removedText: pendingRemoved,
        addedText: "",
        status: "pending",
      },
    });
  }

  return suppressWhitespaceOnlyChanges(groupSegments(raw));
}

function legacyTokens(
  segments: readonly RewriteDiffSegment[],
  side: "before" | "after",
): RewriteDiffToken[] {
  return segments.flatMap((segment) => {
    if (segment.kind === "text") {
      return [{ text: segment.value, status: "unchanged" as const }];
    }

    const text = side === "before" ? segment.hunk.removedText : segment.hunk.addedText;
    const status = side === "before" ? "removed" : "added";

    return text ? [{ text, status } as RewriteDiffToken] : [];
  });
}

export function createRewriteDiff(previousText: string, nextText: string): RewriteDiffView {
  const segments = buildRewriteDiffSegments(previousText, nextText);

  return {
    before: legacyTokens(segments, "before"),
    after: legacyTokens(segments, "after"),
    hasChanges: segments.some((segment) => segment.kind === "change"),
    segments,
  };
}

export function resolveRewriteDiff(
  segments: readonly RewriteDiffSegment[],
  statuses: Readonly<Record<string, RewriteDiffHunkStatus>>,
): string {
  return segments
    .map((segment) => {
      if (segment.kind === "text") {
        return segment.value;
      }

      return statuses[segment.hunk.key] === "rejected"
        ? segment.hunk.removedText
        : segment.hunk.addedText;
    })
    .join("");
}

export function rewriteDiffHunks(segments: readonly RewriteDiffSegment[]): RewriteDiffHunk[] {
  return segments
    .filter((segment): segment is { kind: "change"; hunk: RewriteDiffHunk } =>
      segment.kind === "change",
    )
    .map((segment) => segment.hunk);
}
