import assert from "node:assert/strict";
import test from "node:test";

import { resolveRewriteBubbleState } from "./rewrite-focus";

test("resolveRewriteBubbleState uses word mode for an explicit word selection", () => {
  assert.deepEqual(resolveRewriteBubbleState("Alpha schnell.", 6, 13), {
    mode: "word",
    word: {
      start: 6,
      end: 13,
      text: "schnell",
      context: "Alpha schnell.",
    },
    sentence: {
      start: 0,
      end: 14,
      text: "Alpha schnell.",
    },
  });
});

test("resolveRewriteBubbleState keeps word mode for selected incomplete sentence fragments", () => {
  assert.deepEqual(resolveRewriteBubbleState("Alpha schnell", 6, 13), {
    mode: "word",
    word: {
      start: 6,
      end: 13,
      text: "schnell",
      context: "Alpha schnell",
    },
    sentence: null,
  });
});

test("resolveRewriteBubbleState uses sentence mode for a selection inside one sentence", () => {
  assert.deepEqual(resolveRewriteBubbleState("Alpha Satz.", 0, 11), {
    mode: "sentence",
    sentence: {
      start: 0,
      end: 11,
      text: "Alpha Satz.",
    },
  });
});

test("resolveRewriteBubbleState hides the bubble for a collapsed caret", () => {
  assert.deepEqual(resolveRewriteBubbleState("Alpha schnell.", 9, 9), {
    mode: "hidden",
  });
});

test("resolveRewriteBubbleState hides the bubble for whitespace selections", () => {
  assert.deepEqual(resolveRewriteBubbleState("Alpha  schnell.", 5, 7), {
    mode: "hidden",
  });
});

test("resolveRewriteBubbleState hides the bubble for selections spanning sentences", () => {
  assert.deepEqual(resolveRewriteBubbleState("Alpha Satz. Beta Satz.", 6, 18), {
    mode: "hidden",
  });
});
