import assert from "node:assert/strict";
import test from "node:test";

import {
  appendUniqueAdvisorValidationEvent,
  createAdvisorValidationKey,
} from "./advisor-validation-state";
import type { AdvisorValidationEventPayload } from "./types";

function createEvent(overrides: Partial<AdvisorValidationEventPayload> = {}): AdvisorValidationEventPayload {
  return {
    stableKey: "doc-a::rule-1::downloaden",
    documentName: "doc-a",
    documentTitle: "Dokument A",
    ruleId: "rule-1",
    ruleTitle: "Regel 1",
    page: 4,
    pageLabel: "Seite 4",
    message: "Hinweis",
    matchedText: "downloaden",
    excerpt: "Bitte downloaden Sie die Datei.",
    suggestion: "Nutze herunterladen.",
    referenceUrl: "/api/advisor/doc/doc-a#page=4",
    ...overrides,
  };
}

test("createAdvisorValidationKey falls back to document, rule and matched text", () => {
  assert.equal(
    createAdvisorValidationKey(
      createEvent({
        stableKey: "   ",
      }),
    ),
    "doc-a::rule-1::downloaden",
  );
});

test("appendUniqueAdvisorValidationEvent keeps the first occurrence of the same stable key", () => {
  const first = createEvent();
  const duplicate = createEvent({
    excerpt: "Ein spaeter empfangenes Duplikat.",
  });
  const unique = createEvent({
    stableKey: "doc-b::rule-9::per-sofort",
    documentName: "doc-b",
    documentTitle: "Dokument B",
    ruleId: "rule-9",
    matchedText: "per sofort",
  });

  const withFirst = appendUniqueAdvisorValidationEvent([], first);
  const withDuplicate = appendUniqueAdvisorValidationEvent(withFirst.events, duplicate);
  const withUnique = appendUniqueAdvisorValidationEvent(withDuplicate.events, unique);

  assert.equal(withFirst.added, true);
  assert.equal(withDuplicate.added, false);
  assert.equal(withUnique.added, true);
  assert.equal(withUnique.events.length, 2);
  assert.equal(withUnique.events[0].excerpt, "Bitte downloaden Sie die Datei.");
  assert.equal(withUnique.events[1].documentName, "doc-b");
});
