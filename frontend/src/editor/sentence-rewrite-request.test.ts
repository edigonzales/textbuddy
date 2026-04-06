import assert from "node:assert/strict";
import test from "node:test";

import {
  buildSentenceRewritePayload,
  resolveSentenceRewriteContext,
} from "./sentence-rewrite-request";

test("resolveSentenceRewriteContext returns the surrounding paragraph", () => {
  const text = "Einleitung.\n\nErster Satz. Zweiter Satz.\nDritter Satz.\n\nSchluss.";
  const sentence = "Zweiter Satz.";
  const start = text.indexOf(sentence);
  const end = start + sentence.length;

  assert.equal(
    resolveSentenceRewriteContext(text, start, end),
    "Erster Satz. Zweiter Satz.\nDritter Satz.",
  );
});

test("buildSentenceRewritePayload includes optional context when available", () => {
  assert.deepEqual(
    buildSentenceRewritePayload("Satz.", "Absatz."),
    {
      sentence: "Satz.",
      context: "Absatz.",
    },
  );
});

test("buildSentenceRewritePayload omits blank context for backwards compatibility", () => {
  assert.deepEqual(
    buildSentenceRewritePayload("Satz.", "   "),
    {
      sentence: "Satz.",
    },
  );
});
