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
