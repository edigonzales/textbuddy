import assert from "node:assert/strict";
import test from "node:test";

import { textbuddyDocxFilename } from "./docx-download";

test("creates a stable dated Textbuddy filename", () => {
  assert.equal(textbuddyDocxFilename(new Date("2026-08-22T08:00:00Z")), "textbuddy-2026-08-22.docx");
});
