import assert from "node:assert/strict";
import test from "node:test";

import { literalTextToMarkdown, textbuddyDocxFilename } from "./docx-download";

test("creates a stable dated Textbuddy filename", () => {
  class LocalDate extends Date {
    override getFullYear(): number {
      return 2026;
    }

    override getMonth(): number {
      return 7;
    }

    override getDate(): number {
      return 22;
    }

    override getUTCFullYear(): number {
      throw new Error("UTC must not be used for the filename");
    }
  }

  assert.equal(textbuddyDocxFilename(new LocalDate(0)), "textbuddy-2026-08-22.docx");
});

test("escapes Markdown syntax for literal DOCX text", () => {
  assert.equal(
    literalTextToMarkdown("# Titel\n* Punkt\n1. Eintrag"),
    "\\# Titel  \n\\* Punkt  \n1\\. Eintrag",
  );
});
