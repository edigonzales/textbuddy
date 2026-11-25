import assert from "node:assert/strict";
import test from "node:test";

import JSZip from "jszip";

import { createDocxBlob, textbuddyDocxFilename } from "./docx-download";

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

test("exports literal text in separate Word paragraphs", async () => {
  const blob = await createDocxBlob("# Titel\n\n* Punkt & Ergänzung");
  const zip = await JSZip.loadAsync(await blob.arrayBuffer());
  const xml = await zip.file("word/document.xml")?.async("string");
  assert.ok(xml);
  assert.equal(xml.match(/<w:p[\s>\/]/g)?.length, 3);
  assert.match(xml, /<w:t[^>]*># Titel<\/w:t>/);
  assert.match(xml, /<w:t[^>]*>\* Punkt &amp; Ergänzung<\/w:t>/);
});
