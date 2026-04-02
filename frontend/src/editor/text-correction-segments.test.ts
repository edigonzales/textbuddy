import assert from "node:assert/strict";
import test from "node:test";

import {
  diffTextCorrectionSegments,
  segmentTextForCorrection,
  shouldTriggerCorrectionImmediately,
} from "./text-correction-segments";

test("segmentTextForCorrection splits text by periods and newlines", () => {
  assert.deepEqual(segmentTextForCorrection("Alpha.\nBeta gamma"), [
    {
      start: 0,
      end: 6,
      text: "Alpha.",
    },
    {
      start: 6,
      end: 7,
      text: "\n",
    },
    {
      start: 7,
      end: 17,
      text: "Beta gamma",
    },
  ]);
});

test("diffTextCorrectionSegments isolates only the changed middle segment", () => {
  const previousSegments = segmentTextForCorrection("Alpha.\nBeta recieve.\nGamma.");
  const nextSegments = segmentTextForCorrection("Alpha.\nBeta receive.\nGamma.");
  const diff = diffTextCorrectionSegments(previousSegments, nextSegments);

  assert.equal(diff.unchangedPrefixCount, 2);
  assert.equal(diff.unchangedSuffixCount, 2);
  assert.deepEqual(diff.changedNextSegments, [
    {
      start: 7,
      end: 20,
      text: "Beta receive.",
    },
  ]);
});

test("shouldTriggerCorrectionImmediately reacts to inserted sentence boundaries", () => {
  assert.equal(shouldTriggerCorrectionImmediately("Alpha", "Alpha."), true);
  assert.equal(shouldTriggerCorrectionImmediately("Alpha", "Alpha\n"), true);
  assert.equal(shouldTriggerCorrectionImmediately("Alpha", "Alpha beta"), false);
});
