import assert from "node:assert/strict";
import test from "node:test";

import {
  collectSentenceFocusRanges,
  findFocusedSentence,
} from "./sentence-focus";

test("collectSentenceFocusRanges splits text into trimmed sentence ranges", () => {
  assert.deepEqual(collectSentenceFocusRanges("Alpha Satz.  Beta Satz?\nGamma"), [
    {
      start: 0,
      end: 11,
      text: "Alpha Satz.",
    },
    {
      start: 13,
      end: 23,
      text: "Beta Satz?",
    },
  ]);
});

test("findFocusedSentence resolves the sentence at the caret", () => {
  const text = "Alpha Satz. Beta Satz.";

  assert.deepEqual(findFocusedSentence(text, 16, 16), {
    start: 12,
    end: 22,
    text: "Beta Satz.",
  });
});

test("findFocusedSentence returns null for selections spanning multiple sentences", () => {
  assert.equal(findFocusedSentence("Alpha Satz. Beta Satz.", 2, 15), null);
});

test("findFocusedSentence returns null for incomplete sentence fragments", () => {
  assert.equal(findFocusedSentence("Ich bin", 4, 4), null);
});

test("findFocusedSentence resolves a completed sentence with terminal punctuation", () => {
  assert.deepEqual(findFocusedSentence("Ich bin!", 7, 7), {
    start: 0,
    end: 8,
    text: "Ich bin!",
  });
});

test("findFocusedSentence returns null for trailing whitespace after a completed sentence", () => {
  assert.equal(findFocusedSentence("Ich bin. ", 9, 9), null);
});

test("findFocusedSentence keeps selections inside a completed sentence valid", () => {
  assert.deepEqual(findFocusedSentence("Alpha Satz. Beta Satz?", 12, 20), {
    start: 12,
    end: 22,
    text: "Beta Satz?",
  });
});
