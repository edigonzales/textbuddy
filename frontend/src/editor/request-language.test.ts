import assert from "node:assert/strict";
import test from "node:test";

import { normalizeRequestedLanguage } from "./request-language";

test("normalizeRequestedLanguage falls back to auto for blank values", () => {
  assert.equal(normalizeRequestedLanguage("   "), "auto");
  assert.equal(normalizeRequestedLanguage(undefined), "auto");
});

test("normalizeRequestedLanguage keeps explicit locales", () => {
  assert.equal(normalizeRequestedLanguage(" de-CH "), "de-CH");
  assert.equal(normalizeRequestedLanguage("en-GB"), "en-GB");
});
