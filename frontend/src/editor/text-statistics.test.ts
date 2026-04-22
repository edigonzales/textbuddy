import assert from "node:assert/strict";
import test from "node:test";

import {
  calculateTextStatistics,
  countSyllablesInWord,
  describeFleschScoreKey,
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

test("describeFleschScoreKey maps ranges to keys", () => {
  assert.equal(describeFleschScoreKey(90, true), "stats.flesch.veryEasy");
  assert.equal(describeFleschScoreKey(65, true), "stats.flesch.easy");
  assert.equal(describeFleschScoreKey(45, true), "stats.flesch.medium");
  assert.equal(describeFleschScoreKey(25, true), "stats.flesch.demanding");
  assert.equal(describeFleschScoreKey(10, true), "stats.flesch.veryDemanding");
  assert.equal(describeFleschScoreKey(10, false), "stats.flesch.none");
});
