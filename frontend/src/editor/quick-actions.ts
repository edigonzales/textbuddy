import type { Editor } from "@tiptap/core";

import { apiFetch } from "./api-fetch";
import { isApiLocked } from "./auth";
import { setEditorPlainText } from "./editor-content";
import { extractErrorMessage } from "./http-error";
import { getPlainText } from "./plain-text";
import { normalizeRequestedLanguage } from "./request-language";
import { createRewriteDiff } from "./rewrite-diff";
import type {
  QuickActionElements,
  QuickActionResponse,
  RewriteDiffToken,
} from "./types";
import { t } from "./ui-i18n";

const IDLE_MESSAGE = "";
const UNDONE_MESSAGE = t("quickAction.status.undone");
const AUTH_REQUIRED_MESSAGE = t("quickAction.status.authRequired");
const CUSTOM_PROMPT_MAX_LENGTH = 2_000;
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
  runningMessage: string;
  successMessage: string;
  errorMessage: string;
  buildRequestBody?: (text: string) => QuickActionRequestBody;
}

interface CompletedRewriteState {
  original: string;
  rewritten: string;
}

interface ActiveRequestState {
  original: string;
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

export function mountQuickActions(
  editor: Editor,
  root: HTMLElement,
  elements: QuickActionElements,
): void {
  let activeRequest: ActiveRequestState | null = null;
  let completedRewrite: CompletedRewriteState | null = null;
  let suppressExternalReset = false;
  let selectedAction: QuickActionKey = "plain-language";

  const quickActions: Record<QuickActionKey, QuickActionDefinition> = {
    "plain-language": {
      button: elements.plainLanguageButton,
      endpoint: "/api/quick-actions/plain-language",
      runningMessage: t("quickAction.running.plainLanguage"),
      successMessage: t("quickAction.success.plainLanguage"),
      errorMessage: t("quickAction.error.plainLanguage"),
    },
    "bullet-points": {
      button: elements.bulletPointsButton,
      endpoint: "/api/quick-actions/bullet-points",
      runningMessage: t("quickAction.running.bulletPoints"),
      successMessage: t("quickAction.success.bulletPoints"),
      errorMessage: t("quickAction.error.bulletPoints"),
    },
    proofread: {
      button: elements.proofreadButton,
      endpoint: "/api/quick-actions/proofread",
      runningMessage: t("quickAction.running.proofread"),
      successMessage: t("quickAction.success.proofread"),
      errorMessage: t("quickAction.error.proofread"),
    },
    summarize: {
      button: elements.summarizeButton,
      endpoint: "/api/quick-actions/summarize",
      runningMessage: t("quickAction.running.summarize"),
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
      endpoint: "/api/quick-actions/formality",
      runningMessage: t("quickAction.running.formality"),
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
      endpoint: "/api/quick-actions/social-media",
      runningMessage: t("quickAction.running.socialMedia"),
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
      endpoint: "/api/quick-actions/medium",
      runningMessage: t("quickAction.running.medium"),
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
      endpoint: "/api/quick-actions/character-speech",
      runningMessage: t("quickAction.running.characterSpeech"),
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
      endpoint: "/api/quick-actions/custom",
      runningMessage: t("quickAction.running.custom"),
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
    state: "idle" | "running" | "success" | "error",
    message: string,
  ): void {
    elements.panel.dataset.quickActionState = state;
    elements.panel.setAttribute("aria-busy", state === "running" ? "true" : "false");
    elements.status.setAttribute("role", state === "error" ? "alert" : "status");
    elements.status.setAttribute("aria-live", state === "error" ? "assertive" : "polite");
    elements.status.setAttribute("aria-atomic", "true");
    elements.status.textContent = message;
  }

  function setRunningState(running: boolean): void {
    root.dataset.quickActionRunning = running ? "true" : "false";
    editor.setEditable(!running);
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
    const running = activeRequest !== null;
    const disableSelectors = running || apiLocked;

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
      running ||
      apiLocked ||
      !hasText ||
      (selectedActionNeedsPrompt && !hasValidCustomPrompt(elements.customPromptInput.value));

    elements.runButton.disabled = runDisabled;
    elements.runButton.setAttribute("aria-disabled", runDisabled ? "true" : "false");
    elements.runButton.textContent = running ? t("quickAction.status.running") : t("quickAction.run");
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
    setRunningState(false);
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
    activeRequest = {
      original: originalText,
      controller,
    };
    setRunningState(true);
    setPanelState("running", action.runningMessage);

    try {
      const response = await apiFetch(action.endpoint, {
        method: "POST",
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
        },
        body: JSON.stringify(requestBody),
        signal: controller.signal,
      });

      if (!response.ok) {
        throw new Error(await extractErrorMessage(response, action.errorMessage));
      }

      const payload = (await response.json()) as QuickActionResponse;

      if (activeRequest?.controller !== controller) {
        return;
      }

      const finalText = payload.text ?? originalText;

      setRunningState(false);
      applyEditorText(finalText);
      completedRewrite = { original: activeRequest.original, rewritten: finalText };
      showDiff(completedRewrite.original, completedRewrite.rewritten);
      activeRequest = null;
      setPanelState("success", action.successMessage);
      syncActionAvailability();
    } catch (error) {
      if (isAbortError(error) || activeRequest?.controller !== controller) {
        return;
      }

      const snapshot = activeRequest;

      setRunningState(false);
      applyEditorText(snapshot.original);
      activeRequest = null;
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
