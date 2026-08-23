import type { Editor } from "@tiptap/core";
import { closeHistory } from "@tiptap/pm/history";

import { apiFetch } from "./api-fetch";
import { isApiLocked } from "./auth";
import { setEditorPlainText } from "./editor-content";
import { extractErrorMessage } from "./http-error";
import { getPlainText } from "./plain-text";
import { normalizeRequestedLanguage } from "./request-language";
import {
  createRewriteDiff,
  resolveRewriteDiff,
  rewriteDiffHunks,
} from "./rewrite-diff";
import type {
  QuickActionElements,
  QuickActionResponse,
  RewriteDiffHunk,
  RewriteDiffHunkStatus,
  RewriteDiffSegment,
  WorkspaceBusyChangedDetail,
} from "./types";
import { t } from "./ui-i18n";

const IDLE_MESSAGE = "";
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

interface ReviewState {
  action: QuickActionKey;
  original: string;
  rewritten: string;
  segments: RewriteDiffSegment[];
  statuses: Record<string, RewriteDiffHunkStatus>;
}

interface ActiveRequestState {
  action: QuickActionKey;
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

type ReviewIconName = "check" | "x";

function appendSvgElement(
  svg: SVGSVGElement,
  name: string,
  attributes: Record<string, string>,
): void {
  const element = document.createElementNS("http://www.w3.org/2000/svg", name);
  Object.entries(attributes).forEach(([attribute, value]) => {
    element.setAttribute(attribute, value);
  });
  svg.append(element);
}

function createReviewIcon(name: ReviewIconName): SVGSVGElement {
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.setAttribute("class", "diff-decision-icon");
  svg.setAttribute("width", "16");
  svg.setAttribute("height", "16");
  svg.setAttribute("viewBox", "0 0 16 16");
  svg.setAttribute("fill", "none");
  svg.setAttribute("aria-hidden", "true");
  svg.setAttribute("focusable", "false");

  if (name === "check") {
    appendSvgElement(svg, "path", {
      d: "m2.75 8.25 3.25 3.25 7.25-7.5",
      stroke: "currentColor",
      "stroke-linecap": "round",
      "stroke-linejoin": "round",
      "stroke-width": "1.75",
    });
  } else {
    appendSvgElement(svg, "path", {
      d: "m3.25 3.25 9.5 9.5m0-9.5-9.5 9.5",
      stroke: "currentColor",
      "stroke-linecap": "round",
      "stroke-width": "1.75",
    });
  }

  return svg;
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException
    ? error.name === "AbortError"
    : error instanceof Error && error.name === "AbortError";
}

function createTextSpan(text: string, className = ""): HTMLSpanElement {
  const span = document.createElement("span");
  span.textContent = text;
  span.className = className;
  return span;
}

function createDecisionButton(
  label: string,
  decision: "accepted" | "rejected",
  hunk: RewriteDiffHunk,
  onDecision: (key: string, status: RewriteDiffHunkStatus) => void,
): HTMLButtonElement {
  const button = document.createElement("button");
  button.type = "button";
  button.className = `diff-decision diff-decision-${decision}`;
  button.dataset.diffDecision = decision;
  button.dataset.diffHunkKey = hunk.key;
  button.setAttribute("aria-label", label);
  button.title = label;
  button.append(createReviewIcon(decision === "accepted" ? "check" : "x"));
  button.addEventListener("click", () => onDecision(hunk.key, decision));
  return button;
}

export function mountQuickActions(
  editor: Editor,
  root: HTMLElement,
  elements: QuickActionElements,
): void {
  let activeRequest: ActiveRequestState | null = null;
  let review: ReviewState | null = null;
  let selectedAction: QuickActionKey = "plain-language";
  let viewMode: "inline" | "split" = "inline";
  let suppressExternalReset = false;

  const editorPanel = root.querySelector<HTMLElement>("[data-editor-shell]");
  const editorView = root.querySelector<HTMLElement>("[data-editor-view]");
  const reviewView = root.querySelector<HTMLElement>("[data-review-view]");
  const reviewTitle = root.querySelector<HTMLElement>("[data-review-title]");
  const reviewProgress = root.querySelector<HTMLElement>("[data-review-progress]");
  const reviewInline = root.querySelector<HTMLElement>("[data-review-inline]");
  const reviewSplit = root.querySelector<HTMLElement>("[data-review-split]");
  const reviewSplitBefore = root.querySelector<HTMLElement>("[data-review-split-before]");
  const reviewSplitAfter = root.querySelector<HTMLElement>("[data-review-split-after]");
  const reviewNoChanges = root.querySelector<HTMLElement>("[data-review-no-changes]");
  const acceptAllButton = root.querySelector<HTMLButtonElement>("[data-review-accept-all]");
  const rejectAllButton = root.querySelector<HTMLButtonElement>("[data-review-reject-all]");
  const retryButton = root.querySelector<HTMLButtonElement>("[data-review-retry]");
  const inlineButton = root.querySelector<HTMLButtonElement>("[data-review-mode='inline']");
  const splitButton = root.querySelector<HTMLButtonElement>("[data-review-mode='split']");
  const workspaceStatus = document.querySelector<HTMLElement>("[data-workspace-status]");
  const directPlainLanguage = document.querySelector<HTMLButtonElement>(
    "[data-mvp-quick-action='plain-language']",
  );
  const directSummaryOption = document.querySelector<HTMLSelectElement>(
    "[data-mvp-summary-option]",
  );

  const getSelectedLanguage = (): string =>
    normalizeRequestedLanguage(elements.languageSelect.value);

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
        language: getSelectedLanguage(),
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
        language: getSelectedLanguage(),
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
        language: getSelectedLanguage(),
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
        language: getSelectedLanguage(),
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
        language: getSelectedLanguage(),
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
        language: getSelectedLanguage(),
        prompt: normalizeCustomPrompt(elements.customPromptInput.value),
      }),
    },
  };

  function dispatchBusy(busy: boolean, view: WorkspaceBusyChangedDetail["view"]): void {
    root.dispatchEvent(
      new CustomEvent<WorkspaceBusyChangedDetail>("workspace:busy-changed", {
        bubbles: true,
        detail: { busy, view },
      }),
    );
  }

  function setStatus(state: "idle" | "running" | "success" | "error", message: string): void {
    elements.panel.dataset.quickActionState = state;
    elements.status.textContent = message;
    elements.status.setAttribute("role", state === "error" ? "alert" : "status");

    if (workspaceStatus) {
      workspaceStatus.dataset.state = state;
      workspaceStatus.textContent = message;
      workspaceStatus.hidden = message.length === 0;
      workspaceStatus.setAttribute("role", state === "error" ? "alert" : "status");
    }
  }

  function setWorkspaceView(view: "editor" | "diff-review"): void {
    root.dataset.workspaceView = view;

    if (editorPanel) {
      editorPanel.dataset.workspaceView = view;
    }
    if (editorView) {
      editorView.hidden = view !== "editor";
    }
    if (reviewView) {
      reviewView.hidden = view !== "diff-review";
    }
    dispatchBusy(view === "diff-review", view);
  }

  function setRunning(running: boolean): void {
    root.dataset.quickActionRunning = running ? "true" : "false";
    editor.setEditable(!running && !review);
    dispatchBusy(running || review !== null, review ? "diff-review" : "editor");
    syncAvailability();
  }

  function setSelectedAction(actionKey: QuickActionKey): void {
    selectedAction = actionKey;
    elements.panel.dataset.quickActionSelectedAction = actionKey;
    elements.activeLabel.textContent = ACTION_LABELS[actionKey];

    Object.entries(quickActions).forEach(([key, definition]) => {
      const active = key === actionKey;
      definition.button.dataset.activeAction = active ? "true" : "false";
      definition.button.setAttribute("aria-pressed", active ? "true" : "false");
    });

    elements.panel.querySelectorAll<HTMLElement>("[data-quick-action-config]").forEach((panel) => {
      panel.hidden = panel.dataset.quickActionConfig !== actionKey;
    });
    syncAvailability();
  }

  function syncAvailability(): void {
    const locked = isApiLocked(root);
    const hasText = getPlainText(editor).trim().length > 0;
    const unavailable = locked || activeRequest !== null || review !== null;

    Object.values(quickActions).forEach((action) => {
      action.button.disabled = unavailable;
    });
    [directPlainLanguage].forEach((button) => {
      if (button) {
        button.disabled = unavailable || !hasText;
      }
    });
    if (directSummaryOption) {
      directSummaryOption.disabled = unavailable || !hasText;
    }

    elements.runButton.disabled =
      unavailable ||
      !hasText ||
      (selectedAction === "custom" && !hasValidCustomPrompt(elements.customPromptInput.value));
    elements.runButton.textContent = activeRequest
      ? t("quickAction.status.running")
      : t("quickAction.run");
  }

  function onDecision(key: string, status: RewriteDiffHunkStatus): void {
    if (!review) {
      return;
    }

    review.statuses[key] = status;
    renderReview();

    const pending = rewriteDiffHunks(review.segments).some(
      (hunk) => !review || !review.statuses[hunk.key] || review.statuses[hunk.key] === "pending",
    );

    if (!pending) {
      commitReview();
    }
  }

  function appendInlineHunk(container: HTMLElement, hunk: RewriteDiffHunk): void {
    const status = review?.statuses[hunk.key] ?? "pending";
    const wrapper = document.createElement("span");
    wrapper.className = "diff-hunk-inline";
    wrapper.dataset.diffStatus = status;

    if (status === "accepted") {
      wrapper.append(createTextSpan(hunk.addedText, "diff-added"));
    } else if (status === "rejected") {
      wrapper.append(createTextSpan(hunk.removedText, "diff-rejected"));
    } else {
      if (hunk.removedText) {
        wrapper.append(createTextSpan(hunk.removedText, "diff-removed"));
      }
      if (hunk.removedText && hunk.addedText) {
        wrapper.append(createTextSpan(" → ", "diff-arrow"));
      }
      if (hunk.addedText) {
        wrapper.append(createTextSpan(hunk.addedText, "diff-added"));
      }
      const actions = document.createElement("span");
      actions.className = "diff-hunk-actions";
      actions.append(
        createDecisionButton(t("review.accept"), "accepted", hunk, onDecision),
        createDecisionButton(t("review.reject"), "rejected", hunk, onDecision),
      );
      wrapper.append(actions);
    }
    container.append(wrapper);
  }

  function appendSplitSegment(
    before: HTMLElement,
    after: HTMLElement,
    segment: RewriteDiffSegment,
  ): void {
    if (segment.kind === "text") {
      before.append(createTextSpan(segment.value));
      after.append(createTextSpan(segment.value));
      return;
    }

    const status = review?.statuses[segment.hunk.key] ?? "pending";
    before.append(
      createTextSpan(
        segment.hunk.removedText,
        status === "accepted" ? "diff-rejected" : status === "pending" ? "diff-removed" : "",
      ),
    );
    after.append(
      createTextSpan(
        segment.hunk.addedText,
        status === "rejected" ? "diff-rejected" : "diff-added",
      ),
    );

    if (status === "pending") {
      const beforeActions = document.createElement("span");
      const afterActions = document.createElement("span");
      beforeActions.className = "diff-hunk-actions";
      afterActions.className = "diff-hunk-actions";
      beforeActions.append(
        createDecisionButton(t("review.reject"), "rejected", segment.hunk, onDecision),
      );
      afterActions.append(
        createDecisionButton(t("review.accept"), "accepted", segment.hunk, onDecision),
      );
      before.append(beforeActions);
      after.append(afterActions);
    }
  }

  function renderReview(): void {
    if (!review || !reviewInline || !reviewSplit || !reviewSplitBefore || !reviewSplitAfter) {
      return;
    }

    const hunks = rewriteDiffHunks(review.segments);
    const resolved = hunks.filter(
      (hunk) => (review?.statuses[hunk.key] ?? "pending") !== "pending",
    ).length;

    if (reviewTitle) {
      reviewTitle.textContent = t("review.title", { action: ACTION_LABELS[review.action] });
    }
    if (reviewProgress) {
      reviewProgress.textContent = t("review.progress", { resolved, total: hunks.length });
    }

    reviewInline.replaceChildren();
    reviewSplitBefore.replaceChildren();
    reviewSplitAfter.replaceChildren();

    review.segments.forEach((segment) => {
      if (segment.kind === "text") {
        reviewInline.append(createTextSpan(segment.value));
      } else {
        appendInlineHunk(reviewInline, segment.hunk);
      }
      appendSplitSegment(reviewSplitBefore, reviewSplitAfter, segment);
    });

    reviewInline.hidden = viewMode !== "inline" || hunks.length === 0;
    reviewSplit.hidden = viewMode !== "split" || hunks.length === 0;
    if (reviewNoChanges) {
      reviewNoChanges.hidden = hunks.length > 0;
    }
    [acceptAllButton, inlineButton, splitButton].forEach((button) => {
      if (button) {
        button.hidden = hunks.length === 0;
      }
    });
    if (rejectAllButton) {
      // This is also the exit action for a review that produced no changes.
      rejectAllButton.hidden = false;
    }
    inlineButton?.setAttribute("aria-pressed", viewMode === "inline" ? "true" : "false");
    splitButton?.setAttribute("aria-pressed", viewMode === "split" ? "true" : "false");

    // Keep the legacy before/after contracts populated for compatibility tests.
    const legacyDiff = createRewriteDiff(review.original, review.rewritten);
    elements.diffBefore.textContent = legacyDiff.before.map((token) => token.text).join("");
    elements.diffAfter.textContent = legacyDiff.after.map((token) => token.text).join("");
  }

  function exitReview(message = IDLE_MESSAGE): void {
    review = null;
    elements.diffPanel.hidden = true;
    setWorkspaceView("editor");
    editor.setEditable(true);
    setStatus(message ? "success" : "idle", message);
    syncAvailability();
    editor.commands.focus();
  }

  function commitReview(): void {
    if (!review) {
      return;
    }

    const resolvedText = resolveRewriteDiff(review.segments, review.statuses);
    const successMessage = quickActions[review.action].successMessage;

    suppressExternalReset = true;
    try {
      if (resolvedText !== review.original) {
        editor.view.dispatch(closeHistory(editor.state.tr));
        setEditorPlainText(editor, resolvedText, { emitUpdate: true });
      }
    } finally {
      suppressExternalReset = false;
    }
    exitReview(successMessage);
  }

  async function runQuickAction(actionKey: QuickActionKey): Promise<void> {
    const originalText = getPlainText(editor);
    const action = quickActions[actionKey];

    if (isApiLocked(root)) {
      setStatus("error", AUTH_REQUIRED_MESSAGE);
      syncAvailability();
      return;
    }
    if (!originalText.trim() || activeRequest || review) {
      syncAvailability();
      return;
    }
    if (actionKey === "custom" && !hasValidCustomPrompt(elements.customPromptInput.value)) {
      setStatus("error", t("quickAction.error.customPromptRequired"));
      return;
    }

    selectedAction = actionKey;
    const controller = new AbortController();
    const requestBody = action.buildRequestBody
      ? action.buildRequestBody(originalText)
      : { text: originalText, language: getSelectedLanguage() };

    activeRequest = { action: actionKey, original: originalText, controller };
    setRunning(true);
    setStatus("running", action.runningMessage);

    try {
      const response = await apiFetch(action.endpoint, {
        method: "POST",
        headers: { Accept: "application/json", "Content-Type": "application/json" },
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

      const rewritten = payload.text;
      if (typeof rewritten !== "string" || (!rewritten.trim() && originalText.trim())) {
        throw new Error(action.errorMessage);
      }

      const diff = createRewriteDiff(originalText, rewritten);
      review = {
        action: actionKey,
        original: originalText,
        rewritten,
        segments: diff.segments,
        statuses: Object.fromEntries(
          rewriteDiffHunks(diff.segments).map((hunk) => [hunk.key, "pending"]),
        ),
      };
      activeRequest = null;
      viewMode = "inline";
      elements.diffPanel.hidden = false;
      setWorkspaceView("diff-review");
      setRunning(false);
      setStatus("success", action.successMessage);
      renderReview();
      (diff.hasChanges ? acceptAllButton : retryButton)?.focus();
    } catch (error) {
      if (isAbortError(error) || activeRequest?.controller !== controller) {
        return;
      }

      activeRequest = null;
      setRunning(false);
      setStatus(
        "error",
        error instanceof Error && error.message.trim()
          ? error.message
          : t("quickAction.error.generic"),
      );
      syncAvailability();
    }
  }

  Object.entries(quickActions).forEach(([key, definition]) => {
    definition.button.addEventListener("click", () => setSelectedAction(key as QuickActionKey));
  });
  elements.runButton.addEventListener("click", () => void runQuickAction(selectedAction));
  elements.customPromptInput.addEventListener("input", syncAvailability);
  directPlainLanguage?.addEventListener("click", () => void runQuickAction("plain-language"));
  directSummaryOption?.addEventListener("change", () => {
    if (!directSummaryOption.value) {
      return;
    }
    elements.summarizeOptionSelect.value = directSummaryOption.value;
    void runQuickAction("summarize").finally(() => {
      directSummaryOption.value = "";
    });
  });
  acceptAllButton?.addEventListener("click", () => {
    if (!review) {
      return;
    }
    rewriteDiffHunks(review.segments).forEach((hunk) => {
      review!.statuses[hunk.key] = "accepted";
    });
    commitReview();
  });
  rejectAllButton?.addEventListener("click", () => exitReview());
  retryButton?.addEventListener("click", () => {
    const action = review?.action;
    exitReview();
    if (action) {
      void runQuickAction(action);
    }
  });
  inlineButton?.addEventListener("click", () => {
    viewMode = "inline";
    renderReview();
  });
  splitButton?.addEventListener("click", () => {
    viewMode = "split";
    renderReview();
  });
  root.addEventListener("editor:text-changed", () => {
    if (!suppressExternalReset) {
      syncAvailability();
    }
  });

  setSelectedAction("plain-language");
  setWorkspaceView("editor");
  setStatus(isApiLocked(root) ? "error" : "idle", isApiLocked(root) ? AUTH_REQUIRED_MESSAGE : "");
  syncAvailability();
}
