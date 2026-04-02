import assert from "node:assert/strict";
import test from "node:test";

import { resolveRewriteBubbleState } from "./rewrite-focus";

test("resolveRewriteBubbleState prefers word mode inside a completed sentence", () => {
  assert.deepEqual(resolveRewriteBubbleState("Alpha schnell.", 9, 9), {
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

test("resolveRewriteBubbleState keeps word mode for incomplete sentence fragments", () => {
  assert.deepEqual(resolveRewriteBubbleState("Alpha schnell", 9, 9), {
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

test("resolveRewriteBubbleState falls back to sentence mode without word focus", () => {
  assert.deepEqual(resolveRewriteBubbleState("Alpha Satz.", 11, 11), {
    mode: "sentence",
    sentence: {
      start: 0,
      end: 11,
      text: "Alpha Satz.",
    },
  });
});

test("resolveRewriteBubbleState hides the bubble outside any valid context", () => {
  assert.deepEqual(resolveRewriteBubbleState("   ", 1, 1), {
    mode: "hidden",
  });
});
