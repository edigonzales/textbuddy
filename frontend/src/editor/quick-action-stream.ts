import type { Editor } from "@tiptap/core";

import { isApiLocked } from "./auth";
import { setEditorPlainText } from "./editor-content";
import { getPlainText } from "./plain-text";
import { postQuickActionSse } from "./quick-action-sse";
import { normalizeRequestedLanguage } from "./request-language";
import { createRewriteDiff } from "./rewrite-diff";
import type {
  QuickActionElements,
  QuickActionSseChunkPayload,
  QuickActionSseCompletePayload,
  QuickActionSseErrorPayload,
  RewriteDiffToken,
} from "./types";
import { t } from "./ui-i18n";

const IDLE_MESSAGE = t("quickAction.status.idle");
const UNDONE_MESSAGE = t("quickAction.status.undone");
const AUTH_REQUIRED_MESSAGE = t("quickAction.status.authRequired");
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

const ACTION_LABELS: Record<QuickActionKey, string> = {
  "plain-language": t("quickAction.action.plainLanguage"),
  "bullet-points": t("quickAction.action.bulletPoints"),
  proofread: t("quickAction.action.proofread"),
  summarize: t("quickAction.action.summarize"),
  formality: t("quickAction.action.formality"),
  "social-media": t("quickAction.action.socialMedia"),
  medium: t("quickAction.action.medium"),
  "character-speech": t("quickAction.action.characterSpeech"),
  custom: t("quickAction.action.custom"),
};

function getSelectedLanguage(elements: QuickActionElements): string {
  return normalizeRequestedLanguage(elements.languageSelect.value);
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
  let selectedAction: QuickActionKey = "plain-language";

  const quickActions: Record<QuickActionKey, QuickActionDefinition> = {
    "plain-language": {
      button: elements.plainLanguageButton,
      endpoint: "/api/quick-actions/plain-language/stream",
      streamingMessage: t("quickAction.streaming.plainLanguage"),
      successMessage: t("quickAction.success.plainLanguage"),
      errorMessage: t("quickAction.error.plainLanguage"),
    },
    "bullet-points": {
      button: elements.bulletPointsButton,
      endpoint: "/api/quick-actions/bullet-points/stream",
      streamingMessage: t("quickAction.streaming.bulletPoints"),
      successMessage: t("quickAction.success.bulletPoints"),
      errorMessage: t("quickAction.error.bulletPoints"),
    },
    proofread: {
      button: elements.proofreadButton,
      endpoint: "/api/quick-actions/proofread/stream",
      streamingMessage: t("quickAction.streaming.proofread"),
      successMessage: t("quickAction.success.proofread"),
      errorMessage: t("quickAction.error.proofread"),
    },
    summarize: {
      button: elements.summarizeButton,
      endpoint: "/api/quick-actions/summarize/stream",
      streamingMessage: t("quickAction.streaming.summarize"),
      successMessage: t("quickAction.success.summarize"),
      errorMessage: t("quickAction.error.summarize"),
      buildRequestBody: (text) => ({
        text,
        language: getSelectedLanguage(elements),
        option: elements.summarizeOptionSelect.value,
      }),
    },
    formality: {
      button: elements.formalityButton,
      endpoint: "/api/quick-actions/formality/stream",
      streamingMessage: t("quickAction.streaming.formality"),
      successMessage: t("quickAction.success.formality"),
      errorMessage: t("quickAction.error.formality"),
      buildRequestBody: (text) => ({
        text,
        language: getSelectedLanguage(elements),
        option: elements.formalityOptionSelect.value,
      }),
    },
    "social-media": {
      button: elements.socialMediaButton,
      endpoint: "/api/quick-actions/social-media/stream",
      streamingMessage: t("quickAction.streaming.socialMedia"),
      successMessage: t("quickAction.success.socialMedia"),
      errorMessage: t("quickAction.error.socialMedia"),
      buildRequestBody: (text) => ({
        text,
        language: getSelectedLanguage(elements),
        option: elements.socialMediaOptionSelect.value,
      }),
    },
    medium: {
      button: elements.mediumButton,
      endpoint: "/api/quick-actions/medium/stream",
      streamingMessage: t("quickAction.streaming.medium"),
      successMessage: t("quickAction.success.medium"),
      errorMessage: t("quickAction.error.medium"),
      buildRequestBody: (text) => ({
        text,
        language: getSelectedLanguage(elements),
        option: elements.mediumOptionSelect.value,
      }),
    },
    "character-speech": {
      button: elements.characterSpeechButton,
      endpoint: "/api/quick-actions/character-speech/stream",
      streamingMessage: t("quickAction.streaming.characterSpeech"),
      successMessage: t("quickAction.success.characterSpeech"),
      errorMessage: t("quickAction.error.characterSpeech"),
      buildRequestBody: (text) => ({
        text,
        language: getSelectedLanguage(elements),
        option: elements.characterSpeechOptionSelect.value,
      }),
    },
    custom: {
      button: elements.customButton,
      endpoint: "/api/quick-actions/custom/stream",
      streamingMessage: t("quickAction.streaming.custom"),
      successMessage: t("quickAction.success.custom"),
      errorMessage: t("quickAction.error.custom"),
      buildRequestBody: (text) => ({
        text,
        language: getSelectedLanguage(elements),
        prompt: normalizeCustomPrompt(elements.customPromptInput.value),
      }),
    },
  };

  function setPanelState(
    state: "idle" | "streaming" | "success" | "error",
    message: string,
  ): void {
    elements.panel.dataset.quickActionState = state;
    elements.panel.setAttribute("aria-busy", state === "streaming" ? "true" : "false");
    elements.status.setAttribute("role", state === "error" ? "alert" : "status");
    elements.status.setAttribute("aria-live", state === "error" ? "assertive" : "polite");
    elements.status.setAttribute("aria-atomic", "true");
    elements.status.textContent = message;
  }

  function setStreamingState(streaming: boolean): void {
    root.dataset.quickActionStreaming = streaming ? "true" : "false";
    editor.setEditable(!streaming);
    syncActionAvailability();
  }

  function setSelectedAction(actionKey: QuickActionKey): void {
    selectedAction = actionKey;
    elements.panel.dataset.quickActionSelectedAction = actionKey;
    elements.activeLabel.textContent = ACTION_LABELS[actionKey];

    Object.entries(quickActions).forEach(([entryKey, definition]) => {
      const isActive = entryKey === actionKey;

      definition.button.dataset.activeAction = isActive ? "true" : "false";
      definition.button.setAttribute("aria-pressed", isActive ? "true" : "false");
    });

    const configPanels = Array.from(
      elements.panel.querySelectorAll<HTMLElement>("[data-quick-action-config]"),
    );
    configPanels.forEach((panel) => {
      const panelAction = panel.dataset.quickActionConfig as QuickActionKey | undefined;
      panel.hidden = panelAction !== selectedAction;
    });

    syncActionAvailability();
  }

  function syncActionAvailability(): void {
    const apiLocked = isApiLocked(root);
    const hasText = getPlainText(editor).trim().length > 0;
    const streaming = activeStream !== null;
    const disableSelectors = streaming || apiLocked;

    Object.values(quickActions).forEach((action) => {
      action.button.disabled = disableSelectors;
      action.button.setAttribute("aria-disabled", action.button.disabled ? "true" : "false");
    });

    elements.summarizeOptionSelect.disabled = disableSelectors || selectedAction !== "summarize";
    elements.summarizeOptionSelect.setAttribute(
      "aria-disabled",
      elements.summarizeOptionSelect.disabled ? "true" : "false",
    );
    elements.formalityOptionSelect.disabled = disableSelectors || selectedAction !== "formality";
    elements.formalityOptionSelect.setAttribute(
      "aria-disabled",
      elements.formalityOptionSelect.disabled ? "true" : "false",
    );
    elements.socialMediaOptionSelect.disabled = disableSelectors || selectedAction !== "social-media";
    elements.socialMediaOptionSelect.setAttribute(
      "aria-disabled",
      elements.socialMediaOptionSelect.disabled ? "true" : "false",
    );
    elements.mediumOptionSelect.disabled = disableSelectors || selectedAction !== "medium";
    elements.mediumOptionSelect.setAttribute(
      "aria-disabled",
      elements.mediumOptionSelect.disabled ? "true" : "false",
    );
    elements.characterSpeechOptionSelect.disabled =
      disableSelectors || selectedAction !== "character-speech";
    elements.characterSpeechOptionSelect.setAttribute(
      "aria-disabled",
      elements.characterSpeechOptionSelect.disabled ? "true" : "false",
    );
    elements.customPromptInput.disabled = disableSelectors || selectedAction !== "custom";
    elements.customPromptInput.setAttribute(
      "aria-disabled",
      elements.customPromptInput.disabled ? "true" : "false",
    );

    const selectedActionNeedsPrompt = selectedAction === "custom";
    const runDisabled =
      streaming ||
      apiLocked ||
      !hasText ||
      (selectedActionNeedsPrompt && !hasValidCustomPrompt(elements.customPromptInput.value));

    elements.runButton.disabled = runDisabled;
    elements.runButton.setAttribute("aria-disabled", runDisabled ? "true" : "false");
    elements.runButton.textContent = streaming
      ? t("quickAction.status.running")
      : `${ACTION_LABELS[selectedAction]} ${t("quickAction.runSuffix")}`;
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

    if (isApiLocked(root)) {
      setPanelState("error", AUTH_REQUIRED_MESSAGE);
      syncActionAvailability();
      return;
    }

    if (!originalText.trim()) {
      syncActionAvailability();
      return;
    }

    if (selectedAction === "custom" && !hasValidCustomPrompt(elements.customPromptInput.value)) {
      setPanelState("error", t("quickAction.error.customPromptRequired"));
      syncActionAvailability();
      return;
    }

    const controller = new AbortController();
    const requestBody = action.buildRequestBody
      ? action.buildRequestBody(originalText)
      : {
          text: originalText,
          language: getSelectedLanguage(elements),
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
      setPanelState(
        "error",
        error instanceof Error && error.message.trim().length > 0
          ? error.message
          : t("quickAction.error.generic"),
      );
      syncActionAvailability();
    }
  }

  (Object.keys(quickActions) as QuickActionKey[]).forEach((actionKey) => {
    quickActions[actionKey].button.addEventListener("click", () => {
      setSelectedAction(actionKey);
    });
  });

  elements.runButton.addEventListener("click", () => {
    void runQuickAction(selectedAction);
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
      setPanelState("idle", isApiLocked(root) ? AUTH_REQUIRED_MESSAGE : IDLE_MESSAGE);
    }

    syncActionAvailability();
  });

  setSelectedAction("plain-language");
  syncActionAvailability();
  resetToIdle(isApiLocked(root) ? AUTH_REQUIRED_MESSAGE : IDLE_MESSAGE);
}
