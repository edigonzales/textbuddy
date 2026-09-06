import type { Editor } from "@tiptap/core";
import { closeHistory } from "@tiptap/pm/history";

import { apiFetch } from "./api-fetch";
import { isApiLocked } from "./auth";
import { setEditorPlainText } from "./editor-content";
import { extractErrorMessage } from "./http-error";
import { getPlainText } from "./plain-text";
import { normalizeRequestedLanguage } from "./request-language";
import { createRewriteDiff, resolveRewriteDiff, rewriteDiffHunks } from "./rewrite-diff";
import { calculateTextStatistics, supportsGermanFlesch } from "./text-statistics";
import type {
  QuickActionResponse,
  RewriteDiffHunk,
  RewriteDiffHunkStatus,
  RewriteDiffSegment,
  WorkspaceBusyChangedDetail,
} from "./types";
import { t } from "./ui-i18n";

type QuickAction = "plain-language" | "summarize";
type ReviewMode = "inline" | "split";

interface QuickActionRequestBody {
  text: string;
  language: string;
  option?: string;
}

interface ReviewState {
  action: QuickAction;
  request: QuickActionRequestBody;
  original: string;
  readability: ReadabilityComparison | null;
  segments: RewriteDiffSegment[];
  statuses: Record<string, RewriteDiffHunkStatus>;
}

interface ReadabilityComparison {
  before: number;
  after: number;
  difference: number;
}

interface Controls {
  plainLanguage: HTMLButtonElement;
  summary: HTMLSelectElement;
  language: HTMLSelectElement;
  editorPanel: HTMLElement;
  editorView: HTMLElement;
  reviewView: HTMLElement;
  reviewTitle: HTMLElement;
  reviewProgress: HTMLElement;
  reviewReadability: HTMLElement;
  reviewReadabilityBefore: HTMLElement;
  reviewReadabilityAfter: HTMLElement;
  reviewReadabilityDifference: HTMLElement;
  reviewInline: HTMLElement;
  reviewSplit: HTMLElement;
  reviewSplitBefore: HTMLElement;
  reviewSplitAfter: HTMLElement;
  noChanges: HTMLElement;
  acceptAll: HTMLButtonElement;
  rejectAll: HTMLButtonElement;
  retry: HTMLButtonElement;
  inlineMode: HTMLButtonElement;
  splitMode: HTMLButtonElement;
}

const ACTIONS = {
  "plain-language": {
    label: t("quickAction.action.plainLanguage"),
    endpoint: "/api/quick-actions/plain-language",
    running: t("quickAction.running.plainLanguage"),
    success: t("quickAction.success.plainLanguage"),
    error: t("quickAction.error.plainLanguage"),
  },
  summarize: {
    label: t("quickAction.action.summarize"),
    endpoint: "/api/quick-actions/summarize",
    running: t("quickAction.running.summarize"),
    success: t("quickAction.success.summarize"),
    error: t("quickAction.error.summarize"),
  },
} as const;

function findControls(root: HTMLElement): Controls | null {
  const query = <T extends Element>(selector: string): T | null =>
    document.querySelector<T>(selector) ?? root.querySelector<T>(selector);
  const controls = {
    plainLanguage: query<HTMLButtonElement>("[data-mvp-quick-action='plain-language']"),
    summary: query<HTMLSelectElement>("[data-mvp-summary-option]"),
    language: query<HTMLSelectElement>("[data-workspace-language]"),
    editorPanel: query<HTMLElement>("[data-editor-shell]"),
    editorView: query<HTMLElement>("[data-editor-view]"),
    reviewView: query<HTMLElement>("[data-review-view]"),
    reviewTitle: query<HTMLElement>("[data-review-title]"),
    reviewProgress: query<HTMLElement>("[data-review-progress]"),
    reviewReadability: query<HTMLElement>("[data-review-readability]"),
    reviewReadabilityBefore: query<HTMLElement>("[data-review-readability-before]"),
    reviewReadabilityAfter: query<HTMLElement>("[data-review-readability-after]"),
    reviewReadabilityDifference: query<HTMLElement>("[data-review-readability-difference]"),
    reviewInline: query<HTMLElement>("[data-review-inline]"),
    reviewSplit: query<HTMLElement>("[data-review-split]"),
    reviewSplitBefore: query<HTMLElement>("[data-review-split-before]"),
    reviewSplitAfter: query<HTMLElement>("[data-review-split-after]"),
    noChanges: query<HTMLElement>("[data-review-no-changes]"),
    acceptAll: query<HTMLButtonElement>("[data-review-accept-all]"),
    rejectAll: query<HTMLButtonElement>("[data-review-reject-all]"),
    retry: query<HTMLButtonElement>("[data-review-retry]"),
    inlineMode: query<HTMLButtonElement>("[data-review-mode='inline']"),
    splitMode: query<HTMLButtonElement>("[data-review-mode='split']"),
  };

  return Object.values(controls).every(Boolean) ? controls as Controls : null;
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException
    ? error.name === "AbortError"
    : error instanceof Error && error.name === "AbortError";
}

function textSpan(text: string, className = ""): HTMLSpanElement {
  const span = document.createElement("span");
  span.textContent = text;
  span.className = className;
  return span;
}

function createReadabilityComparison(
  action: QuickAction,
  language: string,
  original: string,
  rewritten: string,
): ReadabilityComparison | null {
  if (action !== "plain-language" || !supportsGermanFlesch(language)) {
    return null;
  }

  const before = calculateTextStatistics(original);
  const after = calculateTextStatistics(rewritten);
  if (before.words === 0 || after.words === 0) {
    return null;
  }

  const difference = Math.round((after.fleschScore - before.fleschScore) * 10) / 10;
  return {
    before: before.fleschScore,
    after: after.fleschScore,
    difference: Object.is(difference, -0) ? 0 : difference,
  };
}

function formatDifference(value: number): string {
  if (value === 0) {
    return "±0.0";
  }

  return `${value > 0 ? "+" : "−"}${Math.abs(value).toFixed(1)}`;
}

export function mountQuickActions(editor: Editor, root: HTMLElement): void {
  const foundControls = findControls(root);
  if (!foundControls) {
    return;
  }
  const controls = foundControls;

  let activeRequest: AbortController | null = null;
  let review: ReviewState | null = null;
  let reviewMode: ReviewMode = "inline";
  let suppressTextChange = false;
  const workspaceStatus = root.querySelector<HTMLElement>("[data-workspace-status]");

  function dispatchBusy(busy: boolean, view: WorkspaceBusyChangedDetail["view"]): void {
    root.dispatchEvent(
      new CustomEvent<WorkspaceBusyChangedDetail>("workspace:busy-changed", {
        bubbles: true,
        detail: { busy, view },
      }),
    );
  }

  function setStatus(state: "idle" | "running" | "success" | "error", message: string): void {
    root.dataset.quickActionState = state;
    if (workspaceStatus) {
      workspaceStatus.dataset.state = state;
      workspaceStatus.textContent = message;
      workspaceStatus.hidden = message.length === 0;
      workspaceStatus.setAttribute("role", state === "error" ? "alert" : "status");
    }
  }

  function setWorkspaceView(view: WorkspaceBusyChangedDetail["view"]): void {
    root.dataset.workspaceView = view;
    controls.editorPanel.dataset.workspaceView = view;
    controls.editorView.hidden = view !== "editor";
    controls.reviewView.hidden = view !== "diff-review";
    dispatchBusy(view === "diff-review", view);
  }

  function syncAvailability(): void {
    const unavailable = isApiLocked(root) || activeRequest !== null || review !== null;
    const hasText = getPlainText(editor).trim().length > 0;
    controls.plainLanguage.disabled = unavailable || !hasText;
    controls.summary.disabled = unavailable || !hasText;
  }

  function setRunning(running: boolean): void {
    root.dataset.quickActionRunning = running ? "true" : "false";
    editor.setEditable(!running && !review);
    dispatchBusy(running || review !== null, review ? "diff-review" : "editor");
    syncAvailability();
  }

  function decisionButton(
    hunk: RewriteDiffHunk,
    decision: "accepted" | "rejected",
  ): HTMLButtonElement {
    const button = document.createElement("button");
    const label = t(decision === "accepted" ? "review.accept" : "review.reject");
    button.type = "button";
    button.className = `diff-decision diff-decision-${decision}`;
    button.dataset.diffDecision = decision;
    button.dataset.diffHunkKey = hunk.key;
    button.setAttribute("aria-label", label);
    button.title = label;
    button.textContent = decision === "accepted" ? "✓" : "×";
    button.addEventListener("click", () => decide(hunk.key, decision));
    return button;
  }

  function appendInline(container: HTMLElement, hunk: RewriteDiffHunk): void {
    const status = review?.statuses[hunk.key] ?? "pending";
    const wrapper = document.createElement("span");
    wrapper.className = "diff-hunk-inline";
    wrapper.dataset.diffStatus = status;

    if (status === "accepted") {
      wrapper.append(textSpan(hunk.addedText, "diff-added"));
    } else if (status === "rejected") {
      wrapper.append(textSpan(hunk.removedText, "diff-rejected"));
    } else {
      if (hunk.removedText) {
        wrapper.append(textSpan(hunk.removedText, "diff-removed"));
      }
      if (hunk.removedText && hunk.addedText) {
        wrapper.append(textSpan(" → ", "diff-arrow"));
      }
      if (hunk.addedText) {
        wrapper.append(textSpan(hunk.addedText, "diff-added"));
      }
      const actions = document.createElement("span");
      actions.className = "diff-hunk-actions";
      actions.append(decisionButton(hunk, "accepted"), decisionButton(hunk, "rejected"));
      wrapper.append(actions);
    }
    container.append(wrapper);
  }

  function appendSplit(
    before: HTMLElement,
    after: HTMLElement,
    segment: RewriteDiffSegment,
  ): void {
    if (segment.kind === "text") {
      before.append(textSpan(segment.value));
      after.append(textSpan(segment.value));
      return;
    }

    const { hunk } = segment;
    const status = review?.statuses[hunk.key] ?? "pending";
    before.append(textSpan(
      hunk.removedText,
      status === "accepted" ? "diff-rejected" : status === "pending" ? "diff-removed" : "",
    ));
    after.append(textSpan(
      hunk.addedText,
      status === "rejected" ? "diff-rejected" : "diff-added",
    ));
    if (status === "pending") {
      const beforeActions = document.createElement("span");
      const afterActions = document.createElement("span");
      beforeActions.className = "diff-hunk-actions";
      afterActions.className = "diff-hunk-actions";
      beforeActions.append(decisionButton(hunk, "rejected"));
      afterActions.append(decisionButton(hunk, "accepted"));
      before.append(beforeActions);
      after.append(afterActions);
    }
  }

  function renderReview(): void {
    if (!review) {
      return;
    }
    const hunks = rewriteDiffHunks(review.segments);
    const resolved = hunks.filter((hunk) => review?.statuses[hunk.key] !== "pending").length;
    controls.reviewTitle.textContent = t("review.title", { action: ACTIONS[review.action].label });
    controls.reviewProgress.textContent = t("review.progress", { resolved, total: hunks.length });
    controls.reviewReadability.hidden = review.readability === null;
    if (review.readability) {
      controls.reviewReadabilityBefore.textContent = review.readability.before.toFixed(1);
      controls.reviewReadabilityAfter.textContent = review.readability.after.toFixed(1);
      controls.reviewReadabilityDifference.textContent = `(${formatDifference(review.readability.difference)})`;
    }
    controls.reviewInline.replaceChildren();
    controls.reviewSplitBefore.replaceChildren();
    controls.reviewSplitAfter.replaceChildren();

    review.segments.forEach((segment) => {
      if (segment.kind === "text") {
        controls.reviewInline.append(textSpan(segment.value));
      } else {
        appendInline(controls.reviewInline, segment.hunk);
      }
      appendSplit(controls.reviewSplitBefore, controls.reviewSplitAfter, segment);
    });

    controls.reviewInline.hidden = reviewMode !== "inline" || hunks.length === 0;
    controls.reviewSplit.hidden = reviewMode !== "split" || hunks.length === 0;
    controls.noChanges.hidden = hunks.length > 0;
    controls.acceptAll.hidden = hunks.length === 0;
    controls.inlineMode.hidden = hunks.length === 0;
    controls.splitMode.hidden = hunks.length === 0;
    controls.rejectAll.hidden = false;
    controls.inlineMode.setAttribute("aria-pressed", reviewMode === "inline" ? "true" : "false");
    controls.splitMode.setAttribute("aria-pressed", reviewMode === "split" ? "true" : "false");
  }

  function exitReview(message = ""): void {
    review = null;
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
    const original = review.original;
    const success = ACTIONS[review.action].success;
    suppressTextChange = true;
    try {
      if (resolvedText !== original) {
        editor.view.dispatch(closeHistory(editor.state.tr));
        setEditorPlainText(editor, resolvedText, { emitUpdate: true });
      }
    } finally {
      suppressTextChange = false;
    }
    exitReview(success);
  }

  function decide(key: string, status: RewriteDiffHunkStatus): void {
    if (!review) {
      return;
    }
    review.statuses[key] = status;
    const pending = rewriteDiffHunks(review.segments).some(
      (hunk) => review?.statuses[hunk.key] === "pending",
    );
    if (pending) {
      renderReview();
    } else {
      commitReview();
    }
  }

  function requestBody(action: QuickAction, original: string): QuickActionRequestBody {
    return {
      text: original,
      language: normalizeRequestedLanguage(controls.language.value),
      ...(action === "summarize" ? { option: controls.summary.value } : {}),
    };
  }

  async function runAction(
    action: QuickAction,
    retryRequest?: QuickActionRequestBody,
  ): Promise<void> {
    const original = retryRequest?.text ?? getPlainText(editor);
    if (isApiLocked(root)) {
      setStatus("error", t("quickAction.status.authRequired"));
      syncAvailability();
      return;
    }
    if (!original.trim() || activeRequest || review) {
      syncAvailability();
      return;
    }

    const definition = ACTIONS[action];
    const body = retryRequest ?? requestBody(action, original);
    const controller = new AbortController();
    activeRequest = controller;
    setRunning(true);
    setStatus("running", definition.running);

    try {
      const response = await apiFetch(definition.endpoint, {
        method: "POST",
        headers: { Accept: "application/json", "Content-Type": "application/json" },
        body: JSON.stringify(body),
        signal: controller.signal,
      });
      if (!response.ok) {
        throw new Error(await extractErrorMessage(response, definition.error));
      }
      const payload = (await response.json()) as QuickActionResponse;
      if (activeRequest !== controller) {
        return;
      }
      if (typeof payload.text !== "string" || (!payload.text.trim() && original.trim())) {
        throw new Error(definition.error);
      }

      const diff = createRewriteDiff(original, payload.text);
      review = {
        action,
        request: body,
        original,
        readability: createReadabilityComparison(action, body.language, original, payload.text),
        segments: diff.segments,
        statuses: Object.fromEntries(
          rewriteDiffHunks(diff.segments).map((hunk) => [hunk.key, "pending"]),
        ),
      };
      activeRequest = null;
      reviewMode = "inline";
      setWorkspaceView("diff-review");
      setRunning(false);
      setStatus("success", definition.success);
      renderReview();
      (diff.hasChanges ? controls.acceptAll : controls.retry).focus();
    } catch (error) {
      if (isAbortError(error) || activeRequest !== controller) {
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

  controls.plainLanguage.addEventListener("click", () => void runAction("plain-language"));
  controls.summary.addEventListener("change", () => {
    if (controls.summary.value) {
      void runAction("summarize").finally(() => {
        controls.summary.value = "";
      });
    }
  });
  controls.acceptAll.addEventListener("click", () => {
    if (review) {
      rewriteDiffHunks(review.segments).forEach((hunk) => {
        review!.statuses[hunk.key] = "accepted";
      });
      commitReview();
    }
  });
  controls.rejectAll.addEventListener("click", () => exitReview());
  controls.retry.addEventListener("click", () => {
    const previous = review;
    exitReview();
    if (previous) {
      void runAction(previous.action, previous.request);
    }
  });
  controls.inlineMode.addEventListener("click", () => {
    reviewMode = "inline";
    renderReview();
  });
  controls.splitMode.addEventListener("click", () => {
    reviewMode = "split";
    renderReview();
  });
  root.addEventListener("editor:text-changed", () => {
    if (!suppressTextChange) {
      syncAvailability();
    }
  });

  setWorkspaceView("editor");
  setStatus(
    isApiLocked(root) ? "error" : "idle",
    isApiLocked(root) ? t("quickAction.status.authRequired") : "",
  );
  syncAvailability();
}
