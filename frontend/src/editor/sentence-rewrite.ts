import type { Editor } from "@tiptap/core";

import {
  documentPositionToPlainTextOffset,
  plainTextRangeToDocumentRange,
} from "./correction-mark-extension";
import { getPlainText } from "./plain-text";
import {
  findFocusedSentence,
  type SentenceFocus,
} from "./sentence-focus";
import type {
  EditorElements,
  SentenceRewriteElements,
  SentenceRewriteResponse,
} from "./types";

const LOADING_MESSAGE = "Alternativen werden geladen...";
const EMPTY_MESSAGE = "Keine Alternativen gefunden.";
const READY_MESSAGE = "Alternative auswaehlen:";
const ERROR_MESSAGE = "Alternativen konnten gerade nicht geladen werden.";

interface ActiveSentence extends SentenceFocus {
  docFrom: number;
  docTo: number;
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException
    ? error.name === "AbortError"
    : error instanceof Error && error.name === "AbortError";
}

function createSentenceKey(sentence: SentenceFocus): string {
  return `${sentence.start}:${sentence.end}:${sentence.text}`;
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

export function mountSentenceRewriteBridge(
  editor: Editor,
  root: HTMLElement,
  editorElements: EditorElements,
  elements: SentenceRewriteElements,
): void {
  let activeSentence: ActiveSentence | null = null;
  let latestRequestId = 0;
  let inFlightRequest: AbortController | null = null;

  function resetOverlay(): void {
    elements.overlay.hidden = true;
    elements.status.textContent = "";
    elements.options.replaceChildren();
    elements.trigger.disabled = false;
    elements.bubble.dataset.rewriteState = "idle";
  }

  function abortInFlightRequest(): void {
    inFlightRequest?.abort();
    inFlightRequest = null;
  }

  function hideBubble(): void {
    abortInFlightRequest();
    activeSentence = null;
    resetOverlay();
    elements.bubble.hidden = true;
  }

  function setOverlayState(
    state: "loading" | "loaded" | "empty" | "error",
    message: string,
  ): void {
    elements.bubble.dataset.rewriteState = state;
    elements.overlay.hidden = false;
    elements.status.textContent = message;
  }

  function positionBubble(): void {
    if (!activeSentence) {
      return;
    }

    const canvasRect = editorElements.canvas.getBoundingClientRect();
    const anchor = editor.view.coordsAtPos(editor.state.selection.from);
    const bubbleWidth = elements.bubble.offsetWidth || 280;
    const bubbleHeight = elements.bubble.offsetHeight || 56;
    const minLeft = Math.min(bubbleWidth / 2 + 12, canvasRect.width / 2);
    const maxLeft = Math.max(minLeft, canvasRect.width - bubbleWidth / 2 - 12);
    let left = clamp(anchor.left - canvasRect.left, minLeft, maxLeft);
    let top = anchor.bottom - canvasRect.top + 12;
    let placement: "bottom" | "top" = "bottom";

    if (top + bubbleHeight > canvasRect.height - 8) {
      top = Math.max(12, anchor.top - canvasRect.top - bubbleHeight - 12);
      placement = "top";
    }

    if (canvasRect.width <= bubbleWidth + 24) {
      left = canvasRect.width / 2;
    }

    elements.bubble.dataset.placement = placement;
    elements.bubble.style.left = `${left}px`;
    elements.bubble.style.top = `${top}px`;
  }

  function resolveFocusedSentence(): ActiveSentence | null {
    const plainText = getPlainText(editor);

    if (!plainText.trim()) {
      return null;
    }

    const start = documentPositionToPlainTextOffset(editor.state.doc, editor.state.selection.from);
    const end = documentPositionToPlainTextOffset(editor.state.doc, editor.state.selection.to);

    if (start === null || end === null) {
      return null;
    }

    const focusedSentence = findFocusedSentence(plainText, start, end);

    if (!focusedSentence) {
      return null;
    }

    const documentRange = plainTextRangeToDocumentRange(
      editor.state.doc,
      focusedSentence.start,
      focusedSentence.end - focusedSentence.start,
    );

    if (!documentRange) {
      return null;
    }

    return {
      ...focusedSentence,
      docFrom: documentRange.from,
      docTo: documentRange.to,
    };
  }

  function syncFocusedSentence(): void {
    const nextSentence = resolveFocusedSentence();

    if (!nextSentence) {
      hideBubble();
      return;
    }

    const previousKey = activeSentence ? createSentenceKey(activeSentence) : "";
    const nextKey = createSentenceKey(nextSentence);

    activeSentence = nextSentence;
    elements.bubble.hidden = false;

    if (previousKey !== nextKey) {
      abortInFlightRequest();
      resetOverlay();
    }

    positionBubble();
  }

  function applyAlternative(sentence: ActiveSentence, alternative: string): void {
    resetOverlay();
    editor
      .chain()
      .focus()
      .insertContentAt({ from: sentence.docFrom, to: sentence.docTo }, alternative)
      .run();
  }

  function createAlternativeButton(
    sentence: ActiveSentence,
    alternative: string,
  ): HTMLButtonElement {
    const button = document.createElement("button");

    button.type = "button";
    button.className = "sentence-rewrite-option";
    button.dataset.testid = "sentence-rewrite-option";
    button.textContent = alternative;
    button.addEventListener("click", () => {
      applyAlternative(sentence, alternative);
    });

    return button;
  }

  async function requestAlternatives(sentence: ActiveSentence): Promise<void> {
    const sentenceKey = createSentenceKey(sentence);
    const requestId = latestRequestId + 1;
    const controller = new AbortController();

    latestRequestId = requestId;
    abortInFlightRequest();
    inFlightRequest = controller;
    elements.options.replaceChildren();
    elements.trigger.disabled = true;
    setOverlayState("loading", LOADING_MESSAGE);
    positionBubble();

    try {
      const response = await fetch("/api/sentence-rewrite", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          sentence: sentence.text,
        }),
        signal: controller.signal,
      });

      if (!response.ok) {
        throw new Error(`Sentence rewrite request failed with status ${response.status}`);
      }

      const payload = (await response.json()) as SentenceRewriteResponse;

      if (
        requestId !== latestRequestId ||
        !activeSentence ||
        createSentenceKey(activeSentence) !== sentenceKey
      ) {
        return;
      }

      const alternatives = (payload.alternatives ?? [])
        .map((candidate) => candidate.trim())
        .filter((candidate) => candidate.length > 0)
        .filter((candidate) => candidate !== sentence.text)
        .filter((candidate, index, list) => list.indexOf(candidate) === index)
        .slice(0, 3);

      elements.options.replaceChildren(
        ...alternatives.map((alternative) => createAlternativeButton(sentence, alternative)),
      );
      elements.trigger.disabled = false;

      if (alternatives.length === 0) {
        setOverlayState("empty", EMPTY_MESSAGE);
        positionBubble();
        return;
      }

      setOverlayState("loaded", READY_MESSAGE);
      positionBubble();
    } catch (error) {
      if (isAbortError(error) || requestId !== latestRequestId) {
        return;
      }

      elements.trigger.disabled = false;
      setOverlayState("error", ERROR_MESSAGE);
      positionBubble();
    } finally {
      if (inFlightRequest === controller) {
        inFlightRequest = null;
      }
    }
  }

  elements.trigger.addEventListener("click", () => {
    if (!activeSentence) {
      return;
    }

    void requestAlternatives(activeSentence);
  });

  root.addEventListener("editor:selection-changed", () => {
    syncFocusedSentence();
  });

  root.addEventListener("editor:text-changed", () => {
    syncFocusedSentence();
  });

  document.addEventListener("pointerdown", (event) => {
    if (root.contains(event.target as Node)) {
      return;
    }

    hideBubble();
  });

  window.addEventListener("resize", () => {
    if (!elements.bubble.hidden) {
      positionBubble();
    }
  });

  resetOverlay();
  elements.bubble.hidden = true;
}
