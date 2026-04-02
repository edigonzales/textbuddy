import assert from "node:assert/strict";
import test from "node:test";

import {
  filterCorrectionBlocksByDictionary,
  normalizeDictionaryWord,
} from "./local-dictionary";
import type { TextCorrectionBlock } from "./types";

test("normalizeDictionaryWord trims and lowercases values", () => {
  assert.equal(normalizeDictionaryWord("  Teh  "), "teh");
});

test("filterCorrectionBlocksByDictionary hides known word matches and keeps other rules", () => {
  const original = "This is teh text  with space.";
  const blocks: TextCorrectionBlock[] = [
    {
      offset: 8,
      length: 3,
      message: "Possible spelling mistake found.",
      shortMessage: "Spelling",
      ruleId: "SPELLING",
      replacements: ["the"],
    },
    {
      offset: 16,
      length: 2,
      message: "Unnecessary whitespace found.",
      shortMessage: "Whitespace",
      ruleId: "WHITESPACE",
      replacements: [" "],
    },
  ];

  assert.deepEqual(
    filterCorrectionBlocksByDictionary(original, blocks, new Set(["teh"])),
    [
      {
        offset: 16,
        length: 2,
        message: "Unnecessary whitespace found.",
        shortMessage: "Whitespace",
        ruleId: "WHITESPACE",
        replacements: [" "],
      },
    ],
  );
});
