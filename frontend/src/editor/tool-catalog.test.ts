import assert from "node:assert/strict";
import test from "node:test";

import { TEXTBUDDY_TOOL_CATALOG, isMvpToolVisible } from "./tool-catalog";

test("the MVP exposes exactly correction, plain language and summarize", () => {
  assert.deepEqual(
    TEXTBUDDY_TOOL_CATALOG.filter((tool) => tool.mvpVisible).map((tool) => tool.key),
    ["correction", "plain-language", "summarize"],
  );
  assert.equal(isMvpToolVisible("advisor"), false);
});
