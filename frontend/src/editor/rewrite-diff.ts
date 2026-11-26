import { diffWordsWithSpace } from "diff";

import type { RewriteDiffHunk, RewriteDiffHunkStatus, RewriteDiffSegment } from "./types";

export function buildRewriteDiffSegments(
  previousText: string,
  nextText: string,
): RewriteDiffSegment[] | undefined {
  const parts = diffWordsWithSpace(previousText, nextText, {
    maxEditLength: 2_000,
    timeout: 200,
  });
  if (!parts) return undefined;

  const segments: RewriteDiffSegment[] = [];
  for (const part of parts) {
    if (!part.added && !part.removed) {
      if (part.value) segments.push({ kind: "text", value: part.value });
      continue;
    }

    let previous = segments.at(-1);
    if (previous?.kind !== "change") {
      previous = {
        kind: "change",
        hunk: { key: `hunk-${segments.length}`, removedText: "", addedText: "" },
      };
      segments.push(previous);
    }
    if (part.removed) previous.hunk.removedText += part.value;
    else previous.hunk.addedText += part.value;
  }
  return segments;
}

export function resolveRewriteDiff(
  segments: readonly RewriteDiffSegment[],
  statuses: Readonly<Record<string, RewriteDiffHunkStatus>>,
): string {
  return segments
    .map((segment) => {
      if (segment.kind === "text") return segment.value;
      return statuses[segment.hunk.key] === "rejected"
        ? segment.hunk.removedText
        : segment.hunk.addedText;
    })
    .join("");
}

export function rewriteDiffHunks(segments: readonly RewriteDiffSegment[]): RewriteDiffHunk[] {
  return segments.flatMap((segment) => segment.kind === "change" ? [segment.hunk] : []);
}
