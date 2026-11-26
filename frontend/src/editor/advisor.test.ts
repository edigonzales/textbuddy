import assert from "node:assert/strict";
import test from "node:test";

import {
  consumeAdvisorSse,
  parseAdvisorSseBlock,
  type AdvisorSseEvent,
} from "./advisor";

const encoder = new TextEncoder();

test("parses validation, progress and error SSE events", () => {
  assert.deepEqual(parseAdvisorSseBlock('event:progress\ndata:{"checked":3,"total":10}'), {
    event: "progress",
    data: { checked: 3, total: 10 },
  });
  assert.equal(parseAdvisorSseBlock("event:unknown\ndata:{}"), null);
  assert.throws(() => parseAdvisorSseBlock("event:error\ndata:not-json"), SyntaxError);
  assert.deepEqual(parseAdvisorSseBlock('event:complete\ndata:{"incomplete":true}'), {
    event: "complete", data: { incomplete: true },
  });
});

test("consumes fragmented and coalesced SSE blocks", async () => {
  const chunks = [
    'event:validation\ndata:{"replacement":"","start":1,',
    '"end":4}\n\nevent:progress\ndata:{"checked":3,"total":6}\n\n',
    'event:error\ndata:{"message":"kaputt"}\n\n',
  ];
  const stream = new ReadableStream<Uint8Array>({
    start(controller) {
      chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)));
      controller.close();
    },
  });
  const events: AdvisorSseEvent[] = [];

  await consumeAdvisorSse(stream, (event) => events.push(event));

  assert.deepEqual(events.map((event) => event.event), ["validation", "progress", "error"]);
  assert.equal(events[0]?.event === "validation" && events[0].data.replacement, "");
  assert.equal(events[1]?.event === "progress" && events[1].data.checked, 3);
});
