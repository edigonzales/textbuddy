import type { Editor } from "@tiptap/core";
import { closeHistory } from "@tiptap/pm/history";

import { setEditorPlainText } from "./editor-content";
import { dispatchWorkspaceBusy } from "./events";
import { createRewriteDiff, resolveRewriteDiff, rewriteDiffHunks } from "./rewrite-diff";
import type { RewriteDiffHunk, RewriteDiffHunkStatus, RewriteDiffSegment } from "./types";
import { t } from "./ui-i18n";

type ReviewMode = "inline" | "split";

export interface ReadabilityComparison {
  before: number;
  after: number;
  difference: number;
}

export interface ReviewResult {
  committed: boolean;
  changed: boolean;
}

export interface ReviewOptions {
  source: string;
  title: string;
  original: string;
  rewritten: string;
  readability?: ReadabilityComparison | null;
  retry?: () => void;
  onClose?: (result: ReviewResult) => void;
}

export interface ReviewController {
  isOpen(): boolean;
  open(options: ReviewOptions): boolean;
}

interface ReviewState extends ReviewOptions {
  segments: RewriteDiffSegment[];
  statuses: Record<string, RewriteDiffHunkStatus>;
}

interface Controls {
  editorPanel: HTMLElement;
  editorView: HTMLElement;
  reviewView: HTMLElement;
  title: HTMLElement;
  progress: HTMLElement;
  readability: HTMLElement;
  readabilityBefore: HTMLElement;
  readabilityAfter: HTMLElement;
  readabilityDifference: HTMLElement;
  inline: HTMLElement;
  split: HTMLElement;
  splitBefore: HTMLElement;
  splitAfter: HTMLElement;
  noChanges: HTMLElement;
  acceptAll: HTMLButtonElement;
  rejectAll: HTMLButtonElement;
  retry: HTMLButtonElement;
  inlineMode: HTMLButtonElement;
  splitMode: HTMLButtonElement;
}

function findControls(root: HTMLElement): Controls | null {
  const query = <T extends Element>(selector: string): T | null => root.querySelector<T>(selector);
  const controls = {
    editorPanel: query<HTMLElement>("[data-editor-shell]"),
    editorView: query<HTMLElement>("[data-editor-view]"),
    reviewView: query<HTMLElement>("[data-review-view]"),
    title: query<HTMLElement>("[data-review-title]"),
    progress: query<HTMLElement>("[data-review-progress]"),
    readability: query<HTMLElement>("[data-review-readability]"),
    readabilityBefore: query<HTMLElement>("[data-review-readability-before]"),
    readabilityAfter: query<HTMLElement>("[data-review-readability-after]"),
    readabilityDifference: query<HTMLElement>("[data-review-readability-difference]"),
    inline: query<HTMLElement>("[data-review-inline]"),
    split: query<HTMLElement>("[data-review-split]"),
    splitBefore: query<HTMLElement>("[data-review-split-before]"),
    splitAfter: query<HTMLElement>("[data-review-split-after]"),
    noChanges: query<HTMLElement>("[data-review-no-changes]"),
    acceptAll: query<HTMLButtonElement>("[data-review-accept-all]"),
    rejectAll: query<HTMLButtonElement>("[data-review-reject-all]"),
    retry: query<HTMLButtonElement>("[data-review-retry]"),
    inlineMode: query<HTMLButtonElement>("[data-review-mode='inline']"),
    splitMode: query<HTMLButtonElement>("[data-review-mode='split']"),
  };
  return Object.values(controls).every(Boolean) ? controls as Controls : null;
}

function textSpan(text: string, className = ""): HTMLSpanElement {
  const span = document.createElement("span");
  span.textContent = text;
  span.className = className;
  return span;
}

export function formatReviewDifference(value: number): string {
  if (value === 0) return "±0.0";
  return `${value > 0 ? "+" : "−"}${Math.abs(value).toFixed(1)}`;
}

export function createReviewController(editor: Editor, root: HTMLElement): ReviewController {
  const controls = findControls(root);
  let review: ReviewState | null = null;
  let mode: ReviewMode = "inline";

  function setView(view: "editor" | "diff-review"): void {
    if (!controls) return;
    root.dataset.workspaceView = view;
    controls.editorPanel.dataset.workspaceView = view;
    controls.editorView.hidden = view !== "editor";
    controls.reviewView.hidden = view !== "diff-review";
  }

  function decisionButton(hunk: RewriteDiffHunk, decision: "accepted" | "rejected"): HTMLButtonElement {
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
      if (hunk.removedText) wrapper.append(textSpan(hunk.removedText, "diff-removed"));
      if (hunk.removedText && hunk.addedText) wrapper.append(textSpan(" → ", "diff-arrow"));
      if (hunk.addedText) wrapper.append(textSpan(hunk.addedText, "diff-added"));
      const actions = document.createElement("span");
      actions.className = "diff-hunk-actions";
      actions.append(decisionButton(hunk, "accepted"), decisionButton(hunk, "rejected"));
      wrapper.append(actions);
    }
    container.append(wrapper);
  }

  function appendSplit(before: HTMLElement, after: HTMLElement, segment: RewriteDiffSegment): void {
    if (segment.kind === "text") {
      before.append(textSpan(segment.value));
      after.append(textSpan(segment.value));
      return;
    }
    const status = review?.statuses[segment.hunk.key] ?? "pending";
    before.append(textSpan(segment.hunk.removedText,
      status === "accepted" ? "diff-rejected" : status === "pending" ? "diff-removed" : ""));
    after.append(textSpan(segment.hunk.addedText,
      status === "rejected" ? "diff-rejected" : "diff-added"));
    if (status === "pending") {
      const beforeActions = document.createElement("span");
      const afterActions = document.createElement("span");
      beforeActions.className = "diff-hunk-actions";
      afterActions.className = "diff-hunk-actions";
      beforeActions.append(decisionButton(segment.hunk, "rejected"));
      afterActions.append(decisionButton(segment.hunk, "accepted"));
      before.append(beforeActions);
      after.append(afterActions);
    }
  }

  function render(): void {
    if (!controls || !review) return;
    const hunks = rewriteDiffHunks(review.segments);
    const resolved = hunks.filter((hunk) => review?.statuses[hunk.key] !== "pending").length;
    controls.title.textContent = review.title;
    controls.progress.textContent = t("review.progress", { resolved, total: hunks.length });
    controls.readability.hidden = !review.readability;
    if (review.readability) {
      controls.readabilityBefore.textContent = review.readability.before.toFixed(1);
      controls.readabilityAfter.textContent = review.readability.after.toFixed(1);
      controls.readabilityDifference.textContent = `(${formatReviewDifference(review.readability.difference)})`;
    }
    controls.inline.replaceChildren();
    controls.splitBefore.replaceChildren();
    controls.splitAfter.replaceChildren();
    review.segments.forEach((segment) => {
      if (segment.kind === "text") controls.inline.append(textSpan(segment.value));
      else appendInline(controls.inline, segment.hunk);
      appendSplit(controls.splitBefore, controls.splitAfter, segment);
    });
    controls.inline.hidden = mode !== "inline" || hunks.length === 0;
    controls.split.hidden = mode !== "split" || hunks.length === 0;
    controls.noChanges.hidden = hunks.length > 0;
    controls.acceptAll.hidden = hunks.length === 0;
    controls.inlineMode.hidden = hunks.length === 0;
    controls.splitMode.hidden = hunks.length === 0;
    controls.retry.hidden = !review.retry;
    controls.inlineMode.setAttribute("aria-pressed", mode === "inline" ? "true" : "false");
    controls.splitMode.setAttribute("aria-pressed", mode === "split" ? "true" : "false");
  }

  function close(result: ReviewResult): void {
    const finished = review;
    review = null;
    delete root.dataset.reviewSource;
    setView("editor");
    dispatchWorkspaceBusy(root, false, "editor");
    finished?.onClose?.(result);
    editor.commands.focus();
  }

  function commit(): void {
    if (!review) return;
    const resolvedText = resolveRewriteDiff(review.segments, review.statuses);
    const changed = resolvedText !== review.original;
    if (changed) {
      editor.view.dispatch(closeHistory(editor.state.tr));
      setEditorPlainText(editor, resolvedText, { emitUpdate: true });
    }
    close({ committed: true, changed });
  }

  function decide(key: string, status: RewriteDiffHunkStatus): void {
    if (!review) return;
    review.statuses[key] = status;
    if (rewriteDiffHunks(review.segments).some((hunk) => review?.statuses[hunk.key] === "pending")) render();
    else commit();
  }

  controls?.acceptAll.addEventListener("click", () => {
    if (!review) return;
    rewriteDiffHunks(review.segments).forEach((hunk) => { review!.statuses[hunk.key] = "accepted"; });
    commit();
  });
  controls?.rejectAll.addEventListener("click", () => close({ committed: false, changed: false }));
  controls?.retry.addEventListener("click", () => {
    const retry = review?.retry;
    close({ committed: false, changed: false });
    retry?.();
  });
  controls?.inlineMode.addEventListener("click", () => { mode = "inline"; render(); });
  controls?.splitMode.addEventListener("click", () => { mode = "split"; render(); });

  setView("editor");
  return {
    isOpen: () => review !== null,
    open(options) {
      if (!controls || review) return false;
      const diff = createRewriteDiff(options.original, options.rewritten);
      review = {
        ...options,
        readability: options.readability ?? null,
        segments: diff.segments,
        statuses: Object.fromEntries(rewriteDiffHunks(diff.segments).map((hunk) => [hunk.key, "pending"])),
      };
      mode = "inline";
      root.dataset.reviewSource = options.source;
      setView("diff-review");
      dispatchWorkspaceBusy(root, true, "diff-review", "review");
      render();
      (diff.hasChanges ? controls.acceptAll : (options.retry ? controls.retry : controls.rejectAll)).focus();
      return true;
    },
  };
}
