import type {
  QuickActionSseChunkPayload,
  QuickActionSseCompletePayload,
  QuickActionSseErrorPayload,
} from "./types";

interface QuickActionSseCallbacks {
  body: unknown;
  signal?: AbortSignal;
  onChunk: (payload: QuickActionSseChunkPayload) => void;
  onComplete: (payload: QuickActionSseCompletePayload) => void;
  onError: (payload: QuickActionSseErrorPayload) => void;
}

function parseEventBlock(block: string): { event: string; data: string } | null {
  const trimmedBlock = block.trim();

  if (!trimmedBlock) {
    return null;
  }

  let eventName = "";
  const dataLines: string[] = [];

  trimmedBlock.split("\n").forEach((line) => {
    if (line.startsWith("event:")) {
      eventName = line.slice("event:".length).trim();
      return;
    }

    if (line.startsWith("data:")) {
      dataLines.push(line.slice("data:".length).trim());
    }
  });

  if (!eventName || dataLines.length === 0) {
    return null;
  }

  return {
    event: eventName,
    data: dataLines.join("\n"),
  };
}

export async function postQuickActionSse(
  url: string,
  callbacks: QuickActionSseCallbacks,
): Promise<void> {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      Accept: "text/event-stream",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(callbacks.body),
    signal: callbacks.signal,
  });

  if (!response.ok) {
    throw new Error(`SSE request failed with status ${response.status}`);
  }

  if (!response.body) {
    throw new Error("SSE response body is missing");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let finished = false;

  while (!finished) {
    const { done, value } = await reader.read();

    buffer += decoder.decode(value ?? new Uint8Array(), {
      stream: !done,
    });

    const normalizedBuffer = buffer.replace(/\r\n/g, "\n");
    const eventBlocks = normalizedBuffer.split("\n\n");

    buffer = eventBlocks.pop() ?? "";

    eventBlocks.forEach((block) => {
      const event = parseEventBlock(block);

      if (!event) {
        return;
      }

      if (event.event === "chunk") {
        callbacks.onChunk(JSON.parse(event.data) as QuickActionSseChunkPayload);
        return;
      }

      if (event.event === "complete") {
        callbacks.onComplete(JSON.parse(event.data) as QuickActionSseCompletePayload);
        finished = true;
        return;
      }

      if (event.event === "error") {
        callbacks.onError(JSON.parse(event.data) as QuickActionSseErrorPayload);
        finished = true;
      }
    });

    if (done) {
      finished = true;
    }
  }
}
