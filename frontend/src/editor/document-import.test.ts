import assert from "node:assert/strict";
import test from "node:test";

import { mapTextLanguageToOcr } from "./import-language";

test("maps editor language variants to OCR languages", () => {
  assert.equal(mapTextLanguageToOcr("de-CH"), "de");
  assert.equal(mapTextLanguageToOcr("en-GB"), "en");
  assert.equal(mapTextLanguageToOcr("fr"), "fr");
  assert.equal(mapTextLanguageToOcr("it"), "it");
  assert.equal(mapTextLanguageToOcr("auto"), "de");
});
