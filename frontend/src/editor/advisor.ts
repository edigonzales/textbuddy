import type { Editor } from "@tiptap/core";

import { apiFetch } from "./api-fetch";
import { isApiLocked } from "./auth";
import { plainTextRangeToDocumentRange, setTextCorrections } from "./correction-mark-extension";
import { dispatchWorkspaceBusy } from "./events";
import { extractErrorMessage } from "./http-error";
import { getPlainText } from "./plain-text";
import type { ReviewController } from "./review-controller";
import type { RewriteDiffSegment } from "./types";
import { t } from "./ui-i18n";

export interface AdvisorDocument {
  name: string; title: string; summary: string; source: string; documentUrl: string; ruleCount: number;
}

export interface AdvisorFinding {
  stableKey: string; documentName: string; documentTitle: string; ruleId: string; ruleTitle: string;
  page: number; message: string; matchedText: string; excerpt: string;
  suggestion: string; referenceUrl: string; start: number; end: number;
}

export type AdvisorSseEvent =
  | { event: "validation"; data: AdvisorFinding }
  | { event: "progress"; data: { checked: number; total: number } }
  | { event: "complete"; data: { truncated: boolean } }
  | { event: "error"; data: { message?: string } };

interface FindingState extends AdvisorFinding { decision: "fix" | "skip"; }

export function parseAdvisorSseBlock(block: string): AdvisorSseEvent | null {
  let eventName = "message";
  const data: string[] = [];
  block.split(/\r?\n/).forEach((line) => {
    if (line.startsWith("event:")) eventName = line.slice(6).trim();
    if (line.startsWith("data:")) data.push(line.slice(5).trimStart());
  });
  if (!["validation", "progress", "complete", "error"].includes(eventName)) return null;
  return { event: eventName, data: JSON.parse(data.join("\n")) } as AdvisorSseEvent;
}

export async function consumeAdvisorSse(
  stream: ReadableStream<Uint8Array>,
  onEvent: (event: AdvisorSseEvent) => void,
): Promise<void> {
  const reader = stream.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  try {
    while (true) {
      const { value, done } = await reader.read();
      buffer += decoder.decode(value, { stream: !done });
      const blocks = buffer.split(/\r?\n\r?\n/);
      buffer = blocks.pop() ?? "";
      blocks.forEach((block) => {
        const event = parseAdvisorSseBlock(block);
        if (event) onEvent(event);
      });
      if (done) break;
    }
  } finally {
    reader.releaseLock();
  }
}

export function toggleAdvisorDecision(value: "fix" | "skip"): "fix" | "skip" {
  return value === "fix" ? "skip" : "fix";
}

export function mountAdvisor(editor: Editor, root: HTMLElement, review: ReviewController): void {
  const panel = root.querySelector<HTMLElement>("[data-advisor-panel]")!;
  const documentsElement = root.querySelector<HTMLElement>("[data-advisor-documents]")!;
  const status = root.querySelector<HTMLElement>("[data-advisor-status]")!;
  const findingsElement = root.querySelector<HTMLElement>("[data-advisor-findings]")!;
  const startButton = root.querySelector<HTMLButtonElement>("[data-advisor-start]")!;
  const cancelButton = root.querySelector<HTMLButtonElement>("[data-advisor-cancel]")!;
  const reloadButton = root.querySelector<HTMLButtonElement>("[data-advisor-reload]")!;
  const fixButton = root.querySelector<HTMLButtonElement>("[data-advisor-fix]")!;
  if (!panel || !documentsElement || !status || !findingsElement || !startButton
      || !cancelButton || !reloadButton || !fixButton) return;

  let documents: AdvisorDocument[] | null = null;
  const selectedDocuments = new Set<string>();
  let findings: FindingState[] = [];
  let checked = 0;
  let total = 0;
  let originalText: string | null = null;
  let activeController: AbortController | null = null;
  let activePhase: "validate" | "fix" | null = null;
  let truncated = false;

  function setStatus(message: string, error = false): void {
    status.textContent = message;
    status.setAttribute("role", error ? "alert" : "status");
    status.setAttribute("aria-live", error ? "assertive" : "polite");
  }

  function setBusy(busy: boolean): void {
    documentsElement.querySelectorAll<HTMLInputElement>("input").forEach((input) => {
      input.disabled = busy || (!input.checked && selectedDocuments.size >= 5);
    });
    startButton.hidden = busy;
    cancelButton.hidden = !busy;
    reloadButton.disabled = busy;
    fixButton.disabled = busy;
    if (busy) dispatchWorkspaceBusy(root, true, "editor", "advisor");
  }

  const selectedCount = (): number => findings.filter((finding) => finding.decision === "fix").length;

  function syncActions(): void {
    const locked = isApiLocked(root) || root.dataset.workspaceBusy === "true";
    startButton.disabled = locked || selectedDocuments.size === 0 || !getPlainText(editor).trim()
      || activeController !== null;
    fixButton.hidden = findings.length === 0;
    fixButton.disabled = locked || selectedCount() === 0 || activeController !== null;
  }

  function clearFindingState(message = ""): void {
    findings = [];
    checked = 0;
    total = 0;
    originalText = null;
    truncated = false;
    findingsElement.replaceChildren();
    fixButton.hidden = true;
    setTextCorrections(editor, []);
    setStatus(message);
    syncActions();
  }

  function focusFinding(finding: FindingState): void {
    if (originalText !== getPlainText(editor)) return;
    setTextCorrections(editor, [{ offset: finding.start, length: finding.end - finding.start }]);
    const range = plainTextRangeToDocumentRange(editor.state.doc, finding.start, finding.end - finding.start);
    if (range) editor.chain().focus().setTextSelection(range).run();
  }

  function renderFindings(): void {
    findingsElement.replaceChildren(...findings.map((finding, index) => {
      const item = document.createElement("article");
      const focus = document.createElement("button");
      const badge = document.createElement("span");
      const fragment = document.createElement("code");
      const title = document.createElement("h4");
      const meta = document.createElement("p");
      const excerpt = document.createElement("p");
      const message = document.createElement("p");
      const suggestion = document.createElement("p");
      const source = document.createElement("a");
      const decision = document.createElement("button");
      item.className = "advisor-finding";
      item.dataset.testid = "advisor-finding";
      item.setAttribute("role", "listitem");
      focus.type = "button";
      focus.className = "advisor-finding-focus";
      focus.dataset.testid = "advisor-finding-focus";
      focus.addEventListener("click", () => focusFinding(finding));
      badge.className = "problem-index";
      badge.textContent = t("advisor.finding.badge", { index: index + 1 });
      fragment.className = "problem-fragment";
      fragment.textContent = finding.matchedText;
      focus.append(badge, fragment);
      title.textContent = finding.ruleTitle;
      meta.className = "advisor-finding-meta";
      meta.textContent = `${finding.documentTitle} · ${t("advisor.finding.page", { page: finding.page })}`;
      excerpt.className = "advisor-finding-excerpt";
      excerpt.dataset.testid = "advisor-finding-excerpt";
      excerpt.textContent = finding.excerpt;
      message.className = "advisor-finding-message";
      message.textContent = finding.message;
      suggestion.className = "advisor-finding-suggestion";
      suggestion.textContent = finding.suggestion;
      source.href = finding.referenceUrl;
      source.target = "_blank";
      source.rel = "noopener";
      source.textContent = t("advisor.document.open");
      decision.type = "button";
      decision.className = "advisor-decision";
      decision.dataset.testid = "advisor-decision";
      decision.dataset.decision = finding.decision;
      decision.setAttribute("aria-pressed", finding.decision === "fix" ? "true" : "false");
      decision.textContent = t(finding.decision === "fix" ? "advisor.finding.fix" : "advisor.finding.skip");
      decision.addEventListener("click", () => {
        finding.decision = toggleAdvisorDecision(finding.decision);
        renderFindings();
      });
      item.append(focus, title, meta, excerpt, message, suggestion, source, decision);
      return item;
    }));
    syncActions();
  }

  function renderDocuments(): void {
    if (!documents) return;
    documentsElement.replaceChildren(...documents.map((entry) => {
      const item = document.createElement("article");
      const label = document.createElement("label");
      const checkbox = document.createElement("input");
      const copy = document.createElement("span");
      const title = document.createElement("strong");
      const summary = document.createElement("span");
      const meta = document.createElement("span");
      const source = document.createElement("a");
      item.className = "advisor-document";
      item.dataset.testid = "advisor-document";
      label.className = "advisor-document-choice";
      checkbox.type = "checkbox";
      checkbox.value = entry.name;
      checkbox.checked = selectedDocuments.has(entry.name);
      checkbox.dataset.testid = "advisor-document-checkbox";
      checkbox.disabled = activeController !== null || (!checkbox.checked && selectedDocuments.size >= 5);
      copy.className = "advisor-document-copy";
      title.textContent = entry.title;
      summary.textContent = entry.summary;
      meta.className = "advisor-document-meta";
      meta.textContent = `${entry.source} · ${t("advisor.document.rules", { count: entry.ruleCount })}`;
      source.href = entry.documentUrl;
      source.target = "_blank";
      source.rel = "noopener";
      source.textContent = t("advisor.document.open");
      source.addEventListener("click", (event) => event.stopPropagation());
      checkbox.addEventListener("change", () => {
        if (checkbox.checked) selectedDocuments.add(entry.name);
        else selectedDocuments.delete(entry.name);
        clearFindingState();
        renderDocuments();
      });
      copy.append(title, summary, meta);
      label.append(checkbox, copy);
      item.append(label, source);
      return item;
    }));
    syncActions();
  }

  async function loadDocuments(): Promise<void> {
    if (documents || activeController) return;
    setStatus(t("advisor.loading"));
    reloadButton.hidden = true;
    try {
      const response = await apiFetch("/api/advisor/docs", { headers: { Accept: "application/json" } });
      if (!response.ok) throw new Error(await extractErrorMessage(response, t("advisor.loadError")));
      const payload = (await response.json()) as AdvisorDocument[];
      documents = Array.isArray(payload) ? payload : [];
      setStatus("");
      renderDocuments();
    } catch (error) {
      documents = null;
      documentsElement.replaceChildren();
      reloadButton.hidden = false;
      setStatus(error instanceof Error && error.message.trim() ? error.message : t("advisor.loadError"), true);
    }
  }

  function finishOperation(controller: AbortController, reviewOpened = false): void {
    if (activeController !== controller) return;
    activeController = null;
    activePhase = null;
    setBusy(false);
    if (!reviewOpened) dispatchWorkspaceBusy(root, false, "editor");
    syncActions();
  }

  async function validate(): Promise<void> {
    if (activeController || root.dataset.workspaceBusy === "true") return;
    const text = getPlainText(editor);
    if (isApiLocked(root)) { setStatus(t("advisor.authRequired"), true); return; }
    if (!text.trim()) { setStatus(t("advisor.emptyText"), true); return; }
    if (!selectedDocuments.size) { setStatus(t("advisor.selectDocument"), true); return; }
    const controller = new AbortController();
    activeController = controller;
    activePhase = "validate";
    findings = [];
    checked = 0;
    total = 0;
    originalText = text;
    truncated = false;
    renderFindings();
    setBusy(true);
    setStatus(t("advisor.progress", { checked: 0, total: 0, count: 0 }));
    try {
      const response = await apiFetch("/api/advisor/validate", {
        method: "POST",
        headers: { Accept: "text/event-stream", "Content-Type": "application/json" },
        body: JSON.stringify({ text, docs: [...selectedDocuments] }),
        signal: controller.signal,
      });
      if (!response.ok || !response.body) throw new Error(await extractErrorMessage(response, t("advisor.error")));
      let completion: boolean | undefined;
      await consumeAdvisorSse(response.body, (event) => {
        if (activeController !== controller || originalText !== text) return;
        if (event.event === "error") throw new Error(event.data.message || t("advisor.error"));
        if (event.event === "complete") {
          completion = event.data.truncated;
          return;
        }
        if (event.event === "progress") {
          checked = event.data.checked;
          total = event.data.total;
        } else if (!findings.some((finding) => finding.stableKey === event.data.stableKey)) {
          findings.push({ ...event.data, decision: "fix" });
          renderFindings();
        }
        setStatus(t("advisor.progress", { checked, total, count: findings.length }));
      });
      if (activeController !== controller) return;
      if (typeof completion !== "boolean" || originalText !== text || getPlainText(editor) !== text) {
        throw new Error(t("advisor.error"));
      }
      truncated = completion;
      reportResults();
    } catch (error) {
      if (controller.signal.aborted || activeController !== controller) return;
      clearFindingState();
      setStatus(error instanceof Error && error.message.trim() ? error.message : t("advisor.error"), true);
    } finally {
      finishOperation(controller);
    }
  }

  function reportResults(): void {
    setStatus(t(truncated ? "advisor.incomplete" : "advisor.complete", { count: findings.length }));
  }

  async function fix(): Promise<void> {
    if (activeController || root.dataset.workspaceBusy === "true") return;
    const text = originalText;
    if (!text || text !== getPlainText(editor)) { clearFindingState(); return; }
    const selected = findings.filter((finding) => finding.decision === "fix")
      .sort((left, right) => left.start - right.start);
    if (!selected.length) return;
    const controller = new AbortController();
    activeController = controller;
    activePhase = "fix";
    setBusy(true);
    setStatus(t("advisor.fix.running"));
    let reviewOpened = false;
    try {
      const response = await apiFetch("/api/advisor/fix", {
        method: "POST",
        headers: { Accept: "application/json", "Content-Type": "application/json" },
        body: JSON.stringify({
          text,
          findings: selected.map(({ documentName, ruleId, start, end, matchedText, suggestion }) => ({
            documentName, ruleId, start, end, matchedText, suggestion,
          })),
        }),
        signal: controller.signal,
      });
      if (!response.ok) throw new Error(await extractErrorMessage(response, t("advisor.fix.error")));
      const replacements: unknown = await response.json();
      if (activeController !== controller) return;
      if (!Array.isArray(replacements) || replacements.length !== selected.length
        || replacements.some((replacement) => typeof replacement !== "string")) {
        throw new Error(t("advisor.fix.error"));
      }

      const segments: RewriteDiffSegment[] = [];
      let cursor = 0;
      selected.forEach(({ start, end, matchedText }, index) => {
        if (start < cursor || end <= start || end > text.length || text.slice(start, end) !== matchedText) {
          throw new Error(t("review.stale"));
        }
        const replacement = replacements[index].replace(/\r\n?/g, "\n");
        if (replacement === matchedText) return;
        if (start > cursor) segments.push({ kind: "text", value: text.slice(cursor, start) });
        segments.push({
          kind: "change",
          hunk: { key: `hunk-${index}`, removedText: matchedText, addedText: replacement },
        });
        cursor = end;
      });
      if (cursor < text.length) segments.push({ kind: "text", value: text.slice(cursor) });
      reviewOpened = review.open({
        source: "advisor",
        title: t("advisor.review.title"),
        original: text,
        segments,
        onClose: (changed) => {
          if (changed) clearFindingState();
          else renderFindings();
        },
      });
      if (!reviewOpened) throw new Error(t("review.stale"));
      reportResults();
    } catch (error) {
      if (controller.signal.aborted || activeController !== controller) return;
      setStatus(error instanceof Error && error.message.trim() ? error.message : t("advisor.fix.error"), true);
    } finally {
      finishOperation(controller, reviewOpened);
    }
  }

  function cancel(): void {
    if (!activeController) return;
    const controller = activeController;
    const phase = activePhase;
    controller.abort();
    finishOperation(controller);
    if (phase === "validate") clearFindingState(t("advisor.cancelled"));
    else setStatus(t("advisor.cancelled"));
  }

  startButton.addEventListener("click", () => void validate());
  cancelButton.addEventListener("click", cancel);
  reloadButton.addEventListener("click", () => void loadDocuments());
  fixButton.addEventListener("click", () => void fix());
  root.addEventListener("workspace:busy-changed", syncActions);
  root.addEventListener("advisor:opened", () => {
    setTextCorrections(editor, []);
    if (isApiLocked(root)) setStatus(t("advisor.authRequired"), true);
    else void loadDocuments();
    syncActions();
  });
  root.addEventListener("validation:panel-changed", (event) => {
    if ((event as CustomEvent<{ panel: string }>).detail.panel !== "advisor") setTextCorrections(editor, []);
  });
  root.addEventListener("editor:text-changed", () => {
    if (root.dataset.reviewSource === "advisor") return;
    if (originalText !== null && getPlainText(editor) !== originalText) clearFindingState();
    else syncActions();
  });
  setStatus(isApiLocked(root) ? t("advisor.authRequired") : "", isApiLocked(root));
  syncActions();
}
