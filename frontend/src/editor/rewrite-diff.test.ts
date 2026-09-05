import assert from "node:assert/strict";
import test from "node:test";

import {
  buildRewriteDiffSegments,
  resolveRewriteDiff,
  rewriteDiffHunks,
} from "./rewrite-diff";

test("builds independent word hunks and resolves mixed decisions", () => {
  const segments = buildRewriteDiffSegments("Das alte Haus ist klein.", "Das neue Haus ist gross.");
  const hunks = rewriteDiffHunks(segments);

  assert.equal(hunks.length, 2);
  assert.equal(
    resolveRewriteDiff(segments, {
      [hunks[0]!.key]: "accepted",
      [hunks[1]!.key]: "rejected",
    }),
    "Das neue Haus ist klein.",
  );
});

test("suppresses whitespace-only differences", () => {
  const segments = buildRewriteDiffSegments("Hallo  Welt", "Hallo Welt");
  assert.equal(rewriteDiffHunks(segments).length, 0);
});

test("uses short identifiers that do not contain document text", () => {
  const segments = buildRewriteDiffSegments("vertraulicher alter Text", "vertraulicher neuer Text");
  const [hunk] = rewriteDiffHunks(segments);

  assert.match(hunk!.key, /^hunk-\d+$/);
  assert.equal(hunk!.key.includes("vertraulich"), false);
});

test("falls back to one document hunk for large rewrites", () => {
  const previous = "a".repeat(5_001);
  const next = "b".repeat(5_001);
  const segments = buildRewriteDiffSegments(previous, next);
  const hunks = rewriteDiffHunks(segments);

  assert.equal(hunks.length, 1);
  assert.equal(hunks[0]!.removedText, previous);
  assert.equal(hunks[0]!.addedText, next);
});
