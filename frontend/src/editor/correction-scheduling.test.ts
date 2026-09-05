import assert from "node:assert/strict";
import test from "node:test";

import {
  shouldTriggerCorrectionImmediately,
} from "./correction-scheduling";

test("shouldTriggerCorrectionImmediately reacts to inserted sentence boundaries", () => {
  assert.equal(shouldTriggerCorrectionImmediately("Alpha", "Alpha."), true);
  assert.equal(shouldTriggerCorrectionImmediately("Alpha", "Alpha\n"), true);
  assert.equal(shouldTriggerCorrectionImmediately("Alpha", "Alpha beta"), false);
});
