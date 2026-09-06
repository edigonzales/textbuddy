import type { Editor } from "@tiptap/core";

import { apiFetch } from "./api-fetch";
import { isApiLocked } from "./auth";
import { dispatchWorkspaceBusy } from "./events";
import { extractErrorMessage } from "./http-error";
import { getPlainText } from "./plain-text";
import { normalizeRequestedLanguage } from "./request-language";
import type { ReadabilityComparison, ReviewController } from "./review-controller";
import { calculateTextStatistics, supportsGermanFlesch } from "./text-statistics";
import type { QuickActionResponse } from "./types";
import { t } from "./ui-i18n";

type QuickAction = "plain-language" | "summarize";

interface QuickActionRequestBody {
  text: string;
  language: string;
  option?: string;
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

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException
    ? error.name === "AbortError"
    : error instanceof Error && error.name === "AbortError";
}

function readability(
  action: QuickAction,
  language: string,
  original: string,
  rewritten: string,
): ReadabilityComparison | null {
  if (action !== "plain-language" || !supportsGermanFlesch(language)) return null;
  const before = calculateTextStatistics(original);
  const after = calculateTextStatistics(rewritten);
  if (before.words === 0 || after.words === 0) return null;
  const difference = Math.round((after.fleschScore - before.fleschScore) * 10) / 10;
  return {
    before: before.fleschScore,
    after: after.fleschScore,
    difference: Object.is(difference, -0) ? 0 : difference,
  };
}

export function mountQuickActions(
  editor: Editor,
  root: HTMLElement,
  reviewController: ReviewController,
): void {
  const plainLanguage = document.querySelector<HTMLButtonElement>("[data-mvp-quick-action='plain-language']")!;
  const summary = document.querySelector<HTMLSelectElement>("[data-mvp-summary-option]")!;
  const language = document.querySelector<HTMLSelectElement>("[data-workspace-language]")!;
  const workspaceStatus = root.querySelector<HTMLElement>("[data-workspace-status]");
  if (!plainLanguage || !summary || !language) return;

  let activeRequest: AbortController | null = null;

  function setStatus(state: "idle" | "running" | "success" | "error", message: string): void {
    root.dataset.quickActionState = state;
    if (!workspaceStatus) return;
    workspaceStatus.dataset.state = state;
    workspaceStatus.textContent = message;
    workspaceStatus.hidden = !message;
    workspaceStatus.setAttribute("role", state === "error" ? "alert" : "status");
  }

  function syncAvailability(): void {
    const unavailable = isApiLocked(root) || activeRequest !== null || reviewController.isOpen()
      || root.dataset.workspaceBusy === "true";
    const hasText = getPlainText(editor).trim().length > 0;
    plainLanguage.disabled = unavailable || !hasText;
    summary.disabled = unavailable || !hasText;
  }

  function setRunning(running: boolean): void {
    root.dataset.quickActionRunning = running ? "true" : "false";
    if (running) dispatchWorkspaceBusy(root, true, "editor", "quick-action");
    syncAvailability();
  }

  function requestBody(action: QuickAction, original: string): QuickActionRequestBody {
    return {
      text: original,
      language: normalizeRequestedLanguage(language.value),
      ...(action === "summarize" ? { option: summary.value } : {}),
    };
  }

  async function runAction(action: QuickAction, retryRequest?: QuickActionRequestBody): Promise<void> {
    const original = retryRequest?.text ?? getPlainText(editor);
    if (isApiLocked(root)) {
      setStatus("error", t("quickAction.status.authRequired"));
      syncAvailability();
      return;
    }
    if (!original.trim() || activeRequest || reviewController.isOpen() || root.dataset.workspaceBusy === "true") {
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
      if (!response.ok) throw new Error(await extractErrorMessage(response, definition.error));
      const payload = (await response.json()) as QuickActionResponse;
      if (activeRequest !== controller) return;
      if (typeof payload.text !== "string" || (!payload.text.trim() && original.trim())) {
        throw new Error(definition.error);
      }

      activeRequest = null;
      root.dataset.quickActionRunning = "false";
      setStatus("success", definition.success);
      reviewController.open({
        source: "quick-action",
        title: t("review.title", { action: definition.label }),
        original,
        rewritten: payload.text,
        readability: readability(action, body.language, original, payload.text),
        retry: () => void runAction(action, body),
        onClose: () => syncAvailability(),
      });
    } catch (error) {
      if (isAbortError(error) || activeRequest !== controller) return;
      activeRequest = null;
      root.dataset.quickActionRunning = "false";
      dispatchWorkspaceBusy(root, false, "editor");
      setStatus("error", error instanceof Error && error.message.trim()
        ? error.message : t("quickAction.error.generic"));
      syncAvailability();
    }
  }

  plainLanguage.addEventListener("click", () => void runAction("plain-language"));
  summary.addEventListener("change", () => {
    if (summary.value) void runAction("summarize").finally(() => { summary.value = ""; });
  });
  root.addEventListener("editor:text-changed", syncAvailability);
  root.addEventListener("workspace:busy-changed", syncAvailability);
  setStatus(isApiLocked(root) ? "error" : "idle",
    isApiLocked(root) ? t("quickAction.status.authRequired") : "");
  syncAvailability();
}
