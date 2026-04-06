import assert from "node:assert/strict";
import test from "node:test";

import {
  calculateTextStatistics,
  countSyllablesInWord,
  describeFleschScore,
  splitWords,
} from "./text-statistics";

test("splitWords extracts unicode words", () => {
  assert.deepEqual(splitWords("Überprüfung, Wörter und Straße."), [
    "Überprüfung",
    "Wörter",
    "und",
    "Straße",
  ]);
});

test("countSyllablesInWord counts vowel groups including umlauts", () => {
  assert.equal(countSyllablesInWord("Überprüfung"), 4);
  assert.equal(countSyllablesInWord("Rhythmus"), 2);
});

test("calculateTextStatistics computes counters and Flesch score", () => {
  const stats = calculateTextStatistics("Mal Tal. Ball Fall.");

  assert.equal(stats.characters, 19);
  assert.equal(stats.words, 4);
  assert.equal(stats.syllables, 4);
  assert.equal(stats.sentences, 2);
  assert.equal(stats.averageSentenceLength, 2);
  assert.equal(stats.averageSyllablesPerWord, 1);
  assert.equal(stats.fleschScore, 119.5);
});

test("calculateTextStatistics returns zeros for blank text", () => {
  const stats = calculateTextStatistics("   \n");

  assert.equal(stats.words, 0);
  assert.equal(stats.syllables, 0);
  assert.equal(stats.sentences, 0);
  assert.equal(stats.averageSentenceLength, 0);
  assert.equal(stats.averageSyllablesPerWord, 0);
});

test("describeFleschScore maps ranges to labels", () => {
  assert.equal(describeFleschScore(90, true), "Sehr leicht verständlich");
  assert.equal(describeFleschScore(65, true), "Leicht verständlich");
  assert.equal(describeFleschScore(45, true), "Mittel verständlich");
  assert.equal(describeFleschScore(25, true), "Anspruchsvoll");
  assert.equal(describeFleschScore(10, true), "Sehr anspruchsvoll");
  assert.equal(describeFleschScore(10, false), "Keine Bewertung");
});
