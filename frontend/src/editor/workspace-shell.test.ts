import assert from "node:assert/strict";
import test from "node:test";

import {
  dismissedAfterCorrectionCountChange,
  shouldShowCorrectionRail,
} from "./workspace-shell";

test("correction rail is visible only for findings in idle validate mode", () => {
  assert.equal(
    shouldShowCorrectionRail({
      mode: "validate",
      count: 2,
      dismissed: false,
      busy: false,
      view: "editor",
    }),
    true,
  );
  assert.equal(
    shouldShowCorrectionRail({
      mode: "transform",
      count: 2,
      dismissed: false,
      busy: false,
      view: "editor",
    }),
    false,
  );
  assert.equal(
    shouldShowCorrectionRail({
      mode: "validate",
      count: 2,
      dismissed: false,
      busy: true,
      view: "diff-review",
    }),
    false,
  );
});

test("manual dismissal survives the finding set and resets at zero to nonzero", () => {
  assert.equal(dismissedAfterCorrectionCountChange(true, 2, 2, "validate"), true);
  assert.equal(dismissedAfterCorrectionCountChange(true, 2, 0, "validate"), false);
  assert.equal(dismissedAfterCorrectionCountChange(true, 0, 3, "validate"), false);
});
