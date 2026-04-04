import type { Editor } from "@tiptap/core";

import { setEditorPlainText } from "./editor-content";
import { getPlainText } from "./plain-text";
import { postQuickActionSse } from "./quick-action-sse";
import { createRewriteDiff } from "./rewrite-diff";
import type {
  QuickActionElements,
  QuickActionSseChunkPayload,
  QuickActionSseCompletePayload,
  QuickActionSseErrorPayload,
  RewriteDiffToken,
} from "./types";

const IDLE_MESSAGE =
  "Bereit fuer Plain Language, Bullet Points, Proofread, Summarize, Formality, Social Media, Medium, Character Speech und Custom.";
const UNDONE_MESSAGE = "Rewrite wurde rueckgaengig gemacht.";
const CUSTOM_PROMPT_MAX_LENGTH = 400;
const DISALLOWED_CUSTOM_PROMPT_CHARACTERS = /[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/;

type QuickActionKey =
  | "plain-language"
  | "bullet-points"
  | "proofread"
  | "summarize"
  | "formality"
  | "social-media"
  | "medium"
  | "character-speech"
  | "custom";

interface QuickActionRequestBody {
  text: string;
  language: string;
  option?: string;
  prompt?: string;
}

interface QuickActionDefinition {
  button: HTMLButtonElement;
  endpoint: string;
  streamingMessage: string;
  successMessage: string;
  errorMessage: string;
  buildRequestBody?: (text: string) => QuickActionRequestBody;
}

interface CompletedRewriteState {
  original: string;
  rewritten: string;
}

interface ActiveStreamState {
  original: string;
  streamed: string;
  controller: AbortController;
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException
    ? error.name === "AbortError"
    : error instanceof Error && error.name === "AbortError";
}

function renderDiffTokens(container: HTMLElement, tokens: readonly RewriteDiffToken[]): void {
  container.replaceChildren(
    ...tokens.map((token) => {
      const span = document.createElement("span");

      span.className = "rewrite-diff-token";
      span.dataset.diffStatus = token.status;
      span.textContent = token.text;

      return span;
    }),
  );
}

function normalizeCustomPrompt(value: string): string {
  return value
    .replace(/\r\n/g, "\n")
    .replace(/\r/g, "\n")
    .split("\n")
    .map((line) => line.trim())
    .join("\n")
    .trim();
}

function hasValidCustomPrompt(value: string): boolean {
  const normalized = normalizeCustomPrompt(value);

  return (
    normalized.length > 0 &&
    normalized.length <= CUSTOM_PROMPT_MAX_LENGTH &&
    !DISALLOWED_CUSTOM_PROMPT_CHARACTERS.test(normalized)
  );
}

export function mountQuickActionStream(
  editor: Editor,
  root: HTMLElement,
  elements: QuickActionElements,
): void {
  let activeStream: ActiveStreamState | null = null;
  let completedRewrite: CompletedRewriteState | null = null;
  let suppressExternalReset = false;
  const quickActions: Record<QuickActionKey, QuickActionDefinition> = {
    "plain-language": {
      button: elements.plainLanguageButton,
      endpoint: "/api/quick-actions/plain-language/stream",
      streamingMessage: "Plain Language streamt gerade...",
      successMessage: "Plain Language abgeschlossen.",
      errorMessage: "Plain Language konnte gerade nicht umgeschrieben werden.",
    },
    "bullet-points": {
      button: elements.bulletPointsButton,
      endpoint: "/api/quick-actions/bullet-points/stream",
      streamingMessage: "Bullet Points streamen gerade...",
      successMessage: "Bullet Points abgeschlossen.",
      errorMessage: "Bullet Points konnten gerade nicht erstellt werden.",
    },
    proofread: {
      button: elements.proofreadButton,
      endpoint: "/api/quick-actions/proofread/stream",
      streamingMessage: "Proofread streamt gerade...",
      successMessage: "Proofread abgeschlossen.",
      errorMessage: "Proofread konnte gerade nicht abgeschlossen werden.",
    },
    summarize: {
      button: elements.summarizeButton,
      endpoint: "/api/quick-actions/summarize/stream",
      streamingMessage: "Summarize streamt gerade...",
      successMessage: "Summarize abgeschlossen.",
      errorMessage: "Summarize konnte gerade nicht abgeschlossen werden.",
      buildRequestBody: (text) => ({
        text,
        language: "auto",
        option: elements.summarizeOptionSelect.value,
      }),
    },
    formality: {
      button: elements.formalityButton,
      endpoint: "/api/quick-actions/formality/stream",
      streamingMessage: "Formality streamt gerade...",
      successMessage: "Formality abgeschlossen.",
      errorMessage: "Formality konnte gerade nicht abgeschlossen werden.",
      buildRequestBody: (text) => ({
        text,
        language: "auto",
        option: elements.formalityOptionSelect.value,
      }),
    },
    "social-media": {
      button: elements.socialMediaButton,
      endpoint: "/api/quick-actions/social-media/stream",
      streamingMessage: "Social Media streamt gerade...",
      successMessage: "Social Media abgeschlossen.",
      errorMessage: "Social Media konnte gerade nicht abgeschlossen werden.",
      buildRequestBody: (text) => ({
        text,
        language: "auto",
        option: elements.socialMediaOptionSelect.value,
      }),
    },
    medium: {
      button: elements.mediumButton,
      endpoint: "/api/quick-actions/medium/stream",
      streamingMessage: "Medium streamt gerade...",
      successMessage: "Medium abgeschlossen.",
      errorMessage: "Medium konnte gerade nicht abgeschlossen werden.",
      buildRequestBody: (text) => ({
        text,
        language: "auto",
        option: elements.mediumOptionSelect.value,
      }),
    },
    "character-speech": {
      button: elements.characterSpeechButton,
      endpoint: "/api/quick-actions/character-speech/stream",
      streamingMessage: "Character Speech streamt gerade...",
      successMessage: "Character Speech abgeschlossen.",
      errorMessage: "Character Speech konnte gerade nicht abgeschlossen werden.",
      buildRequestBody: (text) => ({
        text,
        language: "auto",
        option: elements.characterSpeechOptionSelect.value,
      }),
    },
    custom: {
      button: elements.customButton,
      endpoint: "/api/quick-actions/custom/stream",
      streamingMessage: "Custom streamt gerade...",
      successMessage: "Custom abgeschlossen.",
      errorMessage: "Custom konnte gerade nicht abgeschlossen werden.",
      buildRequestBody: (text) => ({
        text,
        language: "auto",
        prompt: normalizeCustomPrompt(elements.customPromptInput.value),
      }),
    },
  };

  function setPanelState(
    state: "idle" | "streaming" | "success" | "error",
    message: string,
  ): void {
    elements.panel.dataset.quickActionState = state;
    elements.status.textContent = message;
  }

  function setStreamingState(streaming: boolean): void {
    root.dataset.quickActionStreaming = streaming ? "true" : "false";
    editor.setEditable(!streaming);
    syncActionAvailability();
  }

  function syncActionAvailability(): void {
    const disabled = activeStream !== null || getPlainText(editor).trim().length === 0;

    Object.entries(quickActions).forEach(([actionKey, action]) => {
      action.button.disabled =
        actionKey === "custom"
          ? disabled || !hasValidCustomPrompt(elements.customPromptInput.value)
          : disabled;
    });
    elements.summarizeOptionSelect.disabled = disabled;
    elements.formalityOptionSelect.disabled = disabled;
    elements.socialMediaOptionSelect.disabled = disabled;
    elements.mediumOptionSelect.disabled = disabled;
    elements.characterSpeechOptionSelect.disabled = disabled;
    elements.customPromptInput.disabled = activeStream !== null;
  }

  function applyEditorText(text: string): void {
    suppressExternalReset = true;

    try {
      setEditorPlainText(editor, text, {
        emitUpdate: true,
        addToHistory: false,
      });
    } finally {
      suppressExternalReset = false;
    }
  }

  function clearDiff(): void {
    completedRewrite = null;
    elements.diffPanel.hidden = true;
    elements.diffBefore.replaceChildren();
    elements.diffAfter.replaceChildren();
  }

  function showDiff(previousText: string, nextText: string): void {
    const diff = createRewriteDiff(previousText, nextText);

    renderDiffTokens(elements.diffBefore, diff.before);
    renderDiffTokens(elements.diffAfter, diff.after);
    elements.diffPanel.hidden = false;
  }

  function resetToIdle(message: string): void {
    setStreamingState(false);
    setPanelState("idle", message);
  }

  async function runQuickAction(actionKey: QuickActionKey): Promise<void> {
    const originalText = getPlainText(editor);
    const action = quickActions[actionKey];

    if (!originalText.trim()) {
      syncActionAvailability();
      return;
    }

    const controller = new AbortController();
    const requestBody = action.buildRequestBody
      ? action.buildRequestBody(originalText)
      : {
          text: originalText,
          language: "auto",
        };

    clearDiff();
    activeStream = {
      original: originalText,
      streamed: "",
      controller,
    };
    setStreamingState(true);
    setPanelState("streaming", action.streamingMessage);

    try {
      await postQuickActionSse(action.endpoint, {
        body: requestBody,
        signal: controller.signal,
        onChunk: (payload: QuickActionSseChunkPayload) => {
          if (activeStream?.controller !== controller) {
            return;
          }

          activeStream.streamed += payload.text ?? "";
          applyEditorText(activeStream.streamed);
        },
        onComplete: (payload: QuickActionSseCompletePayload) => {
          if (activeStream?.controller !== controller) {
            return;
          }

          const finalText = payload.text ?? activeStream.streamed;

          setStreamingState(false);
          applyEditorText(finalText);
          completedRewrite = {
            original: activeStream.original,
            rewritten: finalText,
          };
          showDiff(completedRewrite.original, completedRewrite.rewritten);
          activeStream = null;
          setPanelState("success", action.successMessage);
          syncActionAvailability();
        },
        onError: (payload: QuickActionSseErrorPayload) => {
          if (activeStream?.controller !== controller) {
            return;
          }

          setStreamingState(false);
          applyEditorText(activeStream.original);
          activeStream = null;
          clearDiff();
          setPanelState("error", payload.message || action.errorMessage);
          syncActionAvailability();
        },
      });
    } catch (error) {
      if (isAbortError(error) || activeStream?.controller !== controller) {
        return;
      }

      const snapshot = activeStream;

      setStreamingState(false);
      applyEditorText(snapshot.original);
      activeStream = null;
      clearDiff();
      setPanelState("error", action.errorMessage);
      syncActionAvailability();
    }
  }

  elements.plainLanguageButton.addEventListener("click", () => {
    void runQuickAction("plain-language");
  });

  elements.bulletPointsButton.addEventListener("click", () => {
    void runQuickAction("bullet-points");
  });

  elements.proofreadButton.addEventListener("click", () => {
    void runQuickAction("proofread");
  });

  elements.summarizeButton.addEventListener("click", () => {
    void runQuickAction("summarize");
  });

  elements.formalityButton.addEventListener("click", () => {
    void runQuickAction("formality");
  });

  elements.socialMediaButton.addEventListener("click", () => {
    void runQuickAction("social-media");
  });

  elements.mediumButton.addEventListener("click", () => {
    void runQuickAction("medium");
  });

  elements.characterSpeechButton.addEventListener("click", () => {
    void runQuickAction("character-speech");
  });

  elements.customButton.addEventListener("click", () => {
    void runQuickAction("custom");
  });

  elements.customPromptInput.addEventListener("input", () => {
    syncActionAvailability();
  });

  elements.diffUndoButton.addEventListener("click", () => {
    if (!completedRewrite) {
      return;
    }

    applyEditorText(completedRewrite.original);
    clearDiff();
    resetToIdle(UNDONE_MESSAGE);
  });

  root.addEventListener("editor:text-changed", () => {
    if (suppressExternalReset) {
      return;
    }

    if (completedRewrite) {
      clearDiff();
      setPanelState("idle", IDLE_MESSAGE);
    }

    syncActionAvailability();
  });

  syncActionAvailability();
  resetToIdle(IDLE_MESSAGE);
}
