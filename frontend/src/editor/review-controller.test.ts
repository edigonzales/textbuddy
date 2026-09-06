import assert from "node:assert/strict";
import test from "node:test";

import { formatReviewDifference } from "./review-controller";

test("shared review formats positive, negative and unchanged differences", () => {
  assert.equal(formatReviewDifference(1.2), "+1.2");
  assert.equal(formatReviewDifference(-1.2), "−1.2");
  assert.equal(formatReviewDifference(0), "±0.0");
});
