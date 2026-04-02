import assert from "node:assert/strict";
import test from "node:test";

import { findFocusedWord } from "./word-focus";

test("findFocusedWord resolves the word at the caret and keeps sentence context", () => {
  assert.deepEqual(findFocusedWord("Alpha schnell Beta.", 9, 9), {
    start: 6,
    end: 13,
    text: "schnell",
    context: "Alpha schnell Beta.",
  });
});

test("findFocusedWord resolves a caret directly behind the word", () => {
  assert.deepEqual(findFocusedWord("Alpha schnell.", 13, 13), {
    start: 6,
    end: 13,
    text: "schnell",
    context: "Alpha schnell.",
  });
});

test("findFocusedWord returns null for punctuation focus", () => {
  assert.equal(findFocusedWord("Alpha schnell.", 14, 14), null);
});

test("findFocusedWord keeps a selection inside one word valid", () => {
  assert.deepEqual(findFocusedWord("Alpha schnell Beta.", 7, 11), {
    start: 6,
    end: 13,
    text: "schnell",
    context: "Alpha schnell Beta.",
  });
});

test("findFocusedWord returns null for selections spanning multiple words", () => {
  assert.equal(findFocusedWord("Alpha schnell Beta.", 4, 10), null);
});
