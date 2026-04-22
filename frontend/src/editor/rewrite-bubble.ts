import type { Editor } from "@tiptap/core";

import { isApiLocked } from "./auth";
import {
  documentPositionToPlainTextOffset,
  plainTextRangeToDocumentRange,
} from "./correction-mark-extension";
import { extractErrorMessage } from "./http-error";
import { getPlainText } from "./plain-text";
import {
  resolveRewriteBubbleState,
  type RewriteBubbleState,
} from "./rewrite-focus";
import {
  buildSentenceRewritePayload,
  resolveSentenceRewriteContext,
} from "./sentence-rewrite-request";
import type {
  EditorElements,
  RewriteBubbleElements,
  SentenceRewriteResponse,
  WordSynonymResponse,
} from "./types";
import { t } from "./ui-i18n";
import type { SentenceFocus } from "./sentence-focus";
import type { WordFocus } from "./word-focus";

const WORD_LOADING_MESSAGE = t("rewrite.status.word.loading");
const WORD_EMPTY_MESSAGE = t("rewrite.status.word.empty");
const WORD_READY_MESSAGE = t("rewrite.status.word.ready");
const WORD_ERROR_MESSAGE = t("rewrite.status.word.error");

const SENTENCE_LOADING_MESSAGE = t("rewrite.status.sentence.loading");
const SENTENCE_EMPTY_MESSAGE = t("rewrite.status.sentence.empty");
const SENTENCE_READY_MESSAGE = t("rewrite.status.sentence.ready");
const SENTENCE_ERROR_MESSAGE = t("rewrite.status.sentence.error");

interface ActiveSentence extends SentenceFocus {
  docFrom: number;
  docTo: number;
  context: string;
}

interface ActiveWord extends WordFocus {
  docFrom: number;
  docTo: number;
}

type ActiveRewriteContext =
  | { mode: "word"; word: ActiveWord; sentence: ActiveSentence | null }
  | { mode: "sentence"; sentence: ActiveSentence };

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException
    ? error.name === "AbortError"
    : error instanceof Error && error.name === "AbortError";
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function shorten(text: string, maxLength: number): string {
  const normalized = text.trim();

  if (normalized.length <= maxLength) {
    return normalized;
  }

  return `${normalized.slice(0, Math.max(0, maxLength - 1)).trimEnd()}...`;
}

function createWordKey(word: WordFocus): string {
  return `${word.start}:${word.end}:${word.text}:${word.context}`;
}

function createSentenceKey(sentence: SentenceFocus): string {
  return `${sentence.start}:${sentence.end}:${sentence.text}`;
}

function createContextKey(context: ActiveRewriteContext): string {
  if (context.mode === "word") {
    return `word:${createWordKey(context.word)}:${context.sentence ? createSentenceKey(context.sentence) : "no-sentence"}`;
  }

  return `sentence:${createSentenceKey(context.sentence)}`;
}

function toActiveSentence(
  editor: Editor,
  plainText: string,
  sentence: SentenceFocus | null,
): ActiveSentence | null {
  if (!sentence) {
    return null;
  }

  const documentRange = plainTextRangeToDocumentRange(
    editor.state.doc,
    sentence.start,
    sentence.end - sentence.start,
  );

  if (!documentRange) {
    return null;
  }

  return {
    ...sentence,
    docFrom: documentRange.from,
    docTo: documentRange.to,
    context: resolveSentenceRewriteContext(plainText, sentence.start, sentence.end),
  };
}

function toActiveWord(editor: Editor, word: WordFocus): ActiveWord | null {
  const documentRange = plainTextRangeToDocumentRange(
    editor.state.doc,
    word.start,
    word.end - word.start,
  );

  if (!documentRange) {
    return null;
  }

  return {
    ...word,
    docFrom: documentRange.from,
    docTo: documentRange.to,
  };
}

function resolveActiveContext(editor: Editor): ActiveRewriteContext | null {
  const plainText = getPlainText(editor);

  if (!plainText.trim()) {
    return null;
  }

  const start = documentPositionToPlainTextOffset(editor.state.doc, editor.state.selection.from);
  const end = documentPositionToPlainTextOffset(editor.state.doc, editor.state.selection.to);

  if (start === null || end === null) {
    return null;
  }

  const nextState = resolveRewriteBubbleState(plainText, start, end);

  if (nextState.mode === "hidden") {
    return null;
  }

  if (nextState.mode === "word") {
    const word = toActiveWord(editor, nextState.word);

    if (!word) {
      return null;
    }

    return {
      mode: "word",
      word,
      sentence: toActiveSentence(editor, plainText, nextState.sentence),
    };
  }

  const sentence = toActiveSentence(editor, plainText, nextState.sentence);

  if (!sentence) {
    return null;
  }

  return {
    mode: "sentence",
    sentence,
  };
}

export function mountRewriteBubble(
  editor: Editor,
  root: HTMLElement,
  editorElements: EditorElements,
  elements: RewriteBubbleElements,
): void {
  let activeContext: ActiveRewriteContext | null = null;
  let latestRequestId = 0;
  let inFlightRequest: AbortController | null = null;
  let dismissedSelection: { from: number; to: number } | null = null;

  function applyModeAppearance(mode: RewriteBubbleState["mode"]): void {
    elements.bubble.dataset.rewriteMode = mode;
  }

  function resetOverlay(): void {
    elements.overlay.hidden = true;
    elements.status.textContent = "";
    elements.options.replaceChildren();
    elements.primaryAction.disabled = false;
    elements.secondaryAction.disabled = false;
    elements.bubble.dataset.rewriteState = "idle";
  }

  function abortInFlightRequest(): void {
    inFlightRequest?.abort();
    inFlightRequest = null;
  }

  function hideBubble(): void {
    abortInFlightRequest();
    activeContext = null;
    resetOverlay();
    applyModeAppearance("hidden");
    elements.focus.textContent = "";
    elements.primaryAction.hidden = false;
    elements.secondaryAction.hidden = true;
    elements.bubble.hidden = true;
    elements.bubble.setAttribute("aria-hidden", "true");
  }

  function setOverlayState(
    state: "loading" | "loaded" | "empty" | "error",
    message: string,
  ): void {
    elements.bubble.dataset.rewriteState = state;
    elements.overlay.hidden = false;
    elements.status.setAttribute("role", state === "error" ? "alert" : "status");
    elements.status.setAttribute("aria-live", state === "error" ? "assertive" : "polite");
    elements.status.setAttribute("aria-atomic", "true");
    elements.status.textContent = message;
  }

  function positionBubble(): void {
    if (!activeContext) {
      return;
    }

    const canvasRect = editorElements.canvas.getBoundingClientRect();
    const anchor = editor.view.coordsAtPos(editor.state.selection.from);
    const bubbleWidth = elements.bubble.offsetWidth || 320;
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

  function syncBubbleChrome(context: ActiveRewriteContext): void {
    if (context.mode === "word") {
      applyModeAppearance("word");
      elements.focus.textContent = t("rewrite.focus.word", {
        text: context.word.text,
      });
      elements.primaryAction.textContent = t("rewrite.wordAction");
      elements.primaryAction.dataset.actionKind = "word";
      elements.secondaryAction.textContent = t("rewrite.sentenceAction");
      elements.secondaryAction.dataset.actionKind = "sentence";
      elements.secondaryAction.hidden = context.sentence === null;
      return;
    }

    applyModeAppearance("sentence");
    elements.focus.textContent = t("rewrite.focus.sentence", {
      text: shorten(context.sentence.text, 48),
    });
    elements.primaryAction.textContent = t("rewrite.sentenceAction");
    elements.primaryAction.dataset.actionKind = "sentence";
    elements.secondaryAction.hidden = true;
  }

  function syncActiveContext(): void {
    if (isApiLocked(root)) {
      hideBubble();
      return;
    }

    const currentSelection = {
      from: editor.state.selection.from,
      to: editor.state.selection.to,
    };

    if (
      dismissedSelection &&
      dismissedSelection.from === currentSelection.from &&
      dismissedSelection.to === currentSelection.to
    ) {
      hideBubble();
      return;
    }

    dismissedSelection = null;
    const nextContext = resolveActiveContext(editor);

    if (!nextContext) {
      hideBubble();
      return;
    }

    const previousKey = activeContext ? createContextKey(activeContext) : "";
    const nextKey = createContextKey(nextContext);

    activeContext = nextContext;
    elements.bubble.hidden = false;
    elements.bubble.setAttribute("aria-hidden", "false");
    syncBubbleChrome(nextContext);

    if (previousKey !== nextKey) {
      abortInFlightRequest();
      resetOverlay();
      syncBubbleChrome(nextContext);
    }

    positionBubble();
  }

  function applyWordSynonym(word: ActiveWord, synonym: string): void {
    resetOverlay();
    editor
      .chain()
      .focus()
      .insertContentAt({ from: word.docFrom, to: word.docTo }, synonym)
      .run();
  }

  function applySentenceAlternative(sentence: ActiveSentence, alternative: string): void {
    resetOverlay();
    editor
      .chain()
      .focus()
      .insertContentAt({ from: sentence.docFrom, to: sentence.docTo }, alternative)
      .run();
  }

  function createOptionButton(label: string, testId: string, onClick: () => void): HTMLButtonElement {
    const button = document.createElement("button");

    button.type = "button";
    button.className = "rewrite-option";
    button.dataset.testid = testId;
    button.textContent = label;
    button.addEventListener("click", onClick);

    return button;
  }

  async function requestWordSynonyms(context: ActiveRewriteContext & { mode: "word" }): Promise<void> {
    const contextKey = createContextKey(context);
    const requestId = latestRequestId + 1;
    const controller = new AbortController();

    latestRequestId = requestId;
    abortInFlightRequest();
    inFlightRequest = controller;
    elements.options.replaceChildren();
    elements.primaryAction.disabled = true;
    elements.secondaryAction.disabled = true;
    setOverlayState("loading", WORD_LOADING_MESSAGE);
    positionBubble();

    try {
      const response = await fetch("/api/word-synonym", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          word: context.word.text,
          context: context.word.context,
        }),
        signal: controller.signal,
      });

      if (!response.ok) {
        throw new Error(
          await extractErrorMessage(
            response,
            `Word synonym request failed with status ${response.status}`,
          ),
        );
      }

      const payload = (await response.json()) as WordSynonymResponse;

      if (
        requestId !== latestRequestId ||
        !activeContext ||
        createContextKey(activeContext) !== contextKey
      ) {
        return;
      }

      const synonyms = (payload.synonyms ?? [])
        .map((candidate) => candidate.trim())
        .filter((candidate) => candidate.length > 0)
        .filter((candidate) => candidate.toLowerCase() !== context.word.text.toLowerCase())
        .filter((candidate, index, list) => {
          const key = candidate.toLowerCase();
          return list.findIndex((entry) => entry.toLowerCase() === key) === index;
        })
        .slice(0, 5);

      elements.options.replaceChildren(
        ...synonyms.map((synonym) =>
          createOptionButton(synonym, "rewrite-option", () => {
            applyWordSynonym(context.word, synonym);
          }),
        ),
      );

      if (synonyms.length === 0) {
        setOverlayState("empty", WORD_EMPTY_MESSAGE);
        positionBubble();
        return;
      }

      setOverlayState("loaded", WORD_READY_MESSAGE);
      if (
        document.activeElement === elements.primaryAction ||
        document.activeElement === elements.secondaryAction
      ) {
        elements.options.querySelector<HTMLButtonElement>("button")?.focus();
      }
      positionBubble();
    } catch (error) {
      if (isAbortError(error) || requestId !== latestRequestId) {
        return;
      }

      setOverlayState(
        "error",
        error instanceof Error && error.message.trim().length > 0
          ? error.message
          : WORD_ERROR_MESSAGE,
      );
      positionBubble();
    } finally {
      if (inFlightRequest === controller) {
        inFlightRequest = null;
      }

      if (activeContext) {
        syncBubbleChrome(activeContext);
      }

      elements.primaryAction.disabled = false;
      elements.secondaryAction.disabled = false;
    }
  }

  async function requestSentenceAlternatives(sentence: ActiveSentence, contextKey: string): Promise<void> {
    const requestId = latestRequestId + 1;
    const controller = new AbortController();

    latestRequestId = requestId;
    abortInFlightRequest();
    inFlightRequest = controller;
    elements.options.replaceChildren();
    elements.primaryAction.disabled = true;
    elements.secondaryAction.disabled = true;
    setOverlayState("loading", SENTENCE_LOADING_MESSAGE);
    positionBubble();

    try {
      const response = await fetch("/api/sentence-rewrite", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(buildSentenceRewritePayload(sentence.text, sentence.context)),
        signal: controller.signal,
      });

      if (!response.ok) {
        throw new Error(
          await extractErrorMessage(
            response,
            `Sentence rewrite request failed with status ${response.status}`,
          ),
        );
      }

      const payload = (await response.json()) as SentenceRewriteResponse;

      if (
        requestId !== latestRequestId ||
        !activeContext ||
        createContextKey(activeContext) !== contextKey
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
        ...alternatives.map((alternative) =>
          createOptionButton(alternative, "rewrite-option", () => {
            applySentenceAlternative(sentence, alternative);
          }),
        ),
      );

      if (alternatives.length === 0) {
        setOverlayState("empty", SENTENCE_EMPTY_MESSAGE);
        positionBubble();
        return;
      }

      setOverlayState("loaded", SENTENCE_READY_MESSAGE);
      if (
        document.activeElement === elements.primaryAction ||
        document.activeElement === elements.secondaryAction
      ) {
        elements.options.querySelector<HTMLButtonElement>("button")?.focus();
      }
      positionBubble();
    } catch (error) {
      if (isAbortError(error) || requestId !== latestRequestId) {
        return;
      }

      setOverlayState(
        "error",
        error instanceof Error && error.message.trim().length > 0
          ? error.message
          : SENTENCE_ERROR_MESSAGE,
      );
      positionBubble();
    } finally {
      if (inFlightRequest === controller) {
        inFlightRequest = null;
      }

      if (activeContext) {
        syncBubbleChrome(activeContext);
      }

      elements.primaryAction.disabled = false;
      elements.secondaryAction.disabled = false;
    }
  }

  elements.primaryAction.addEventListener("click", () => {
    if (!activeContext) {
      return;
    }

    if (activeContext.mode === "word") {
      void requestWordSynonyms(activeContext);
      return;
    }

    void requestSentenceAlternatives(activeContext.sentence, createContextKey(activeContext));
  });

  elements.secondaryAction.addEventListener("click", () => {
    if (!activeContext || activeContext.mode !== "word" || !activeContext.sentence) {
      return;
    }

    void requestSentenceAlternatives(activeContext.sentence, createContextKey(activeContext));
  });

  root.addEventListener("editor:selection-changed", () => {
    syncActiveContext();
  });

  root.addEventListener("editor:text-changed", () => {
    syncActiveContext();
  });

  document.addEventListener("pointerdown", (event) => {
    if (root.contains(event.target as Node)) {
      return;
    }

    hideBubble();
  });

  window.addEventListener(
    "keydown",
    (event) => {
      if (event.key !== "Escape" || elements.bubble.hidden) {
        return;
      }

      event.preventDefault();
      dismissedSelection = {
        from: editor.state.selection.from,
        to: editor.state.selection.to,
      };
      hideBubble();
    },
    true,
  );

  window.addEventListener("resize", () => {
    if (!elements.bubble.hidden) {
      positionBubble();
    }
  });

  resetOverlay();
  hideBubble();
}
