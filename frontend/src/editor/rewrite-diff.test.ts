import assert from "node:assert/strict";
import test from "node:test";

import {
  buildRewriteDiffSegments,
  resolveRewriteDiff,
  rewriteDiffHunks,
} from "./rewrite-diff";

test("builds independent word hunks and resolves mixed decisions", () => {
  const segments = buildRewriteDiffSegments("Das alte Haus ist klein.", "Das neue Haus ist gross.");
  assert.ok(segments);
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

test("keeps every whitespace-only edit as a reviewable change", () => {
  const before = "Ein  Satz mit CHF 20.\n\nNächster Absatz.";
  const after = "Ein Satz mit CHF\u00a020.\nNächster Absatz.";
  const segments = buildRewriteDiffSegments(before, after);
  assert.ok(segments);
  const hunks = rewriteDiffHunks(segments);
  assert.equal(hunks.length, 3);
  assert.equal(resolveRewriteDiff(segments, {}), after);
  assert.equal(resolveRewriteDiff(segments, Object.fromEntries(hunks.map(({ key }) => [key, "rejected"]))), before);
});

test("uses short identifiers that do not contain document text", () => {
  const segments = buildRewriteDiffSegments("vertraulicher alter Text", "vertraulicher neuer Text");
  assert.ok(segments);
  const [hunk] = rewriteDiffHunks(segments);

  assert.match(hunk!.key, /^hunk-\d+$/);
  assert.equal(hunk!.key.includes("vertraulich"), false);
});

test("keeps independent edits in a multi-page text", () => {
  const before = Array.from({ length: 300 }, (_, index) => `Absatz ${index}: Der Kanton prüft die Vorlage.\n`).join("");
  const after = before.replace("Absatz 8:", "Abschnitt 8:").replace("Absatz 280:", "Abschnitt 280:");
  const segments = buildRewriteDiffSegments(before, after);
  assert.ok(segments);
  const hunks = rewriteDiffHunks(segments);
  assert.equal(hunks.length, 2);
  assert.equal(resolveRewriteDiff(segments, { [hunks[1]!.key]: "rejected" }),
    before.replace("Absatz 8:", "Abschnitt 8:"));
});

test("reports an exhausted diff budget without a full-document fallback", () => {
  assert.equal(buildRewriteDiffSegments("a ".repeat(1_100), "b ".repeat(1_100)), undefined);
});
