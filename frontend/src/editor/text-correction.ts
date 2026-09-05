import type { Editor } from "@tiptap/core";

import { apiFetch } from "./api-fetch";
import { isApiLocked } from "./auth";
import { shouldTriggerCorrectionImmediately } from "./correction-scheduling";
import { plainTextRangeToDocumentRange, setTextCorrections } from "./correction-mark-extension";
import { extractErrorMessage } from "./http-error";
import {
  createLocalDictionaryStore,
  filterCorrectionBlocksByDictionary,
  isDictionaryWord,
  normalizeDictionaryWord,
} from "./local-dictionary";
import { getPlainText, plainTextToHtml } from "./plain-text";
import { normalizeRequestedLanguage } from "./request-language";
import type {
  CorrectionElements,
  CorrectionStateChangedDetail,
  EditorTextChangedDetail,
  TextCorrectionBlock,
  TextCorrectionResponse,
} from "./types";
import { t } from "./ui-i18n";

const CORRECTION_DEBOUNCE_MS = 350;
const IDLE_MESSAGE = "";
const DEBOUNCE_MESSAGE = t("correction.status.debounce");
const LOADING_MESSAGE = t("correction.status.loading");
const ERROR_MESSAGE = t("correction.status.error");
const AUTH_REQUIRED_MESSAGE = t("correction.status.authRequired");
const RUNNING_MESSAGE = t("correction.status.running");

function createProblemBadge(index: number): HTMLElement {
  const badge = document.createElement("span");
  badge.className = "problem-index";
  badge.textContent = t("correction.problem.badge", { index: index + 1 });
  return badge;
}

function extractProblemText(original: string, block: TextCorrectionBlock): string {
  return original.slice(block.offset, block.offset + block.length) ||
    t("correction.problem.emptyFragment");
}

function cloneBlock(block: TextCorrectionBlock): TextCorrectionBlock {
  return { ...block, replacements: [...block.replacements] };
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException
    ? error.name === "AbortError"
    : error instanceof Error && error.name === "AbortError";
}

function selectedLanguage(elements: CorrectionElements): string {
  return normalizeRequestedLanguage(elements.languageSelect.value);
}

function problemCountMessage(count: number): string {
  return count === 0
    ? t("correction.status.noProblems")
    : t("correction.status.problemCount", { count });
}

export function mountTextCorrectionBridge(
  editor: Editor,
  root: HTMLElement,
  elements: CorrectionElements,
): void {
  let debounceHandle: number | undefined;
  let latestRequestId = 0;
  let activeRequest: AbortController | null = null;
  let currentText = getPlainText(editor);
  let currentBlocks: TextCorrectionBlock[] = [];
  let panelState: "idle" | "loading" | "success" | "error" = "idle";
  let panelMessage = IDLE_MESSAGE;
  let dictionaryWords = new Set<string>();
  let visibleBlockCount = 0;
  const dictionaryStore = createLocalDictionaryStore();

  function isQuickActionRunning(): boolean {
    return root.dataset.quickActionRunning === "true";
  }

  function setPanelState(
    state: "idle" | "loading" | "success" | "error",
    message: string,
  ): void {
    panelState = state;
    panelMessage = message;
    elements.panel.dataset.correctionState = state;
    elements.panel.setAttribute("aria-busy", state === "loading" ? "true" : "false");
    elements.status.setAttribute("role", state === "error" ? "alert" : "status");
    elements.status.setAttribute("aria-live", state === "error" ? "assertive" : "polite");
    elements.status.setAttribute("aria-atomic", "true");
    elements.status.textContent = message;
    root.dispatchEvent(
      new CustomEvent<CorrectionStateChangedDetail>("correction:state-changed", {
        bubbles: true,
        detail: { state, message, count: visibleBlockCount },
      }),
    );
  }

  function abortActiveRequest(): void {
    activeRequest?.abort();
    activeRequest = null;
  }

  function clearCorrections(): void {
    currentBlocks = [];
    visibleBlockCount = 0;
    setTextCorrections(editor, []);
    elements.list.replaceChildren();
  }

  function focusProblem(block: TextCorrectionBlock): void {
    const range = plainTextRangeToDocumentRange(editor.state.doc, block.offset, block.length);
    if (range) {
      editor.chain().focus().setTextSelection(range).run();
    }
  }

  function applySuggestion(block: TextCorrectionBlock, replacement: string): void {
    const activeText = getPlainText(editor);
    if (block.offset < 0 || block.offset + block.length > activeText.length) {
      return;
    }
    const nextText = activeText.slice(0, block.offset) + replacement +
      activeText.slice(block.offset + block.length);
    editor.commands.setContent(plainTextToHtml(nextText), { emitUpdate: true });
    editor.commands.focus();
  }

  function addDictionaryWord(candidate: string): void {
    const normalizedWord = normalizeDictionaryWord(candidate);
    if (!isDictionaryWord(normalizedWord) || dictionaryWords.has(normalizedWord)) {
      elements.dictionaryInput.value = "";
      elements.dictionaryInput.setCustomValidity("");
      return;
    }

    dictionaryWords = new Set([...dictionaryWords, normalizedWord]);
    elements.dictionaryInput.value = "";
    elements.dictionaryInput.setCustomValidity("");
    renderDictionaryWords();
    refreshRenderedState();
    void dictionaryStore.save([...dictionaryWords]);
  }

  function removeDictionaryWord(word: string): void {
    if (!dictionaryWords.delete(word)) {
      return;
    }
    dictionaryWords = new Set(dictionaryWords);
    renderDictionaryWords();
    refreshRenderedState();
    void dictionaryStore.save([...dictionaryWords]);
  }

  function createProblemItem(
    original: string,
    block: TextCorrectionBlock,
    index: number,
  ): HTMLElement {
    const item = document.createElement("article");
    const header = document.createElement("button");
    const fragment = document.createElement("code");
    const title = document.createElement("p");
    const detail = document.createElement("p");
    const suggestions = document.createElement("div");
    const problemText = extractProblemText(original, block);

    item.className = "problem-item";
    item.dataset.testid = "correction-problem-item";
    item.dataset.correctionItemIndex = String(index);
    item.setAttribute("role", "listitem");
    header.type = "button";
    header.className = "problem-item-head problem-focus-button";
    header.dataset.correctionFocusIndex = String(index);
    header.setAttribute("aria-label", `${t("correction.problem.badge", { index: index + 1 })}: ${problemText}`);
    header.append(createProblemBadge(index));
    fragment.className = "problem-fragment";
    fragment.textContent = problemText;
    header.append(fragment);
    title.className = "problem-message";
    title.textContent = block.shortMessage || block.message || t("correction.problem.defaultTitle");
    detail.className = "problem-detail";
    detail.textContent = block.message || t("correction.problem.defaultDetail");
    suggestions.className = "problem-suggestions";

    if (block.replacements.length === 0) {
      const emptyState = document.createElement("span");
      emptyState.className = "problem-empty";
      emptyState.textContent = t("correction.problem.noSuggestion");
      suggestions.append(emptyState);
    } else {
      block.replacements.slice(0, 3).forEach((replacement) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "suggestion-button";
        button.dataset.testid = "correction-suggestion";
        button.textContent = replacement;
        button.addEventListener("click", (event) => {
          event.stopPropagation();
          applySuggestion(block, replacement);
        });
        suggestions.append(button);
      });
    }

    if (isDictionaryWord(problemText)) {
      const dictionaryButton = document.createElement("button");
      dictionaryButton.type = "button";
      dictionaryButton.className = "dictionary-inline-button";
      dictionaryButton.dataset.testid = "dictionary-add-problem";
      dictionaryButton.textContent = t("correction.problem.markKnown");
      dictionaryButton.addEventListener("click", (event) => {
        event.stopPropagation();
        addDictionaryWord(problemText);
      });
      suggestions.append(dictionaryButton);
    }

    header.addEventListener("click", () => focusProblem(block));
    item.append(header, title, detail, suggestions);
    return item;
  }

  function visibleBlocks(original: string): TextCorrectionBlock[] {
    return filterCorrectionBlocksByDictionary(original, currentBlocks, dictionaryWords);
  }

  function renderProblems(
    original: string,
    state: "idle" | "loading" | "success" | "error",
    message?: string,
  ): void {
    const blocks = visibleBlocks(original);
    visibleBlockCount = blocks.length;
    setTextCorrections(editor, blocks.map(({ offset, length }) => ({ offset, length })));
    elements.list.replaceChildren(
      ...blocks.map((block, index) => createProblemItem(original, block, index)),
    );
    setPanelState(
      state,
      state === "success" ? problemCountMessage(blocks.length) : (message ?? panelMessage),
    );
  }

  function renderDictionaryWords(): void {
    const words = [...dictionaryWords].sort((left, right) => left.localeCompare(right));
    elements.dictionaryEmpty.hidden = words.length > 0;
    elements.dictionaryList.replaceChildren(
      ...words.map((word) => {
        const item = document.createElement("li");
        const label = document.createElement("span");
        const removeButton = document.createElement("button");
        item.className = "dictionary-word";
        item.dataset.testid = "dictionary-word-item";
        label.className = "dictionary-word-label";
        label.textContent = word;
        removeButton.type = "button";
        removeButton.className = "dictionary-word-remove";
        removeButton.dataset.testid = "dictionary-word-remove";
        removeButton.textContent = t("correction.dictionary.remove");
        removeButton.addEventListener("click", () => removeDictionaryWord(word));
        item.append(label, removeButton);
        return item;
      }),
    );
  }

  function refreshRenderedState(): void {
    if (isApiLocked(root)) {
      clearCorrections();
      setPanelState("error", AUTH_REQUIRED_MESSAGE);
    } else if (!currentText.trim()) {
      clearCorrections();
      setPanelState("idle", IDLE_MESSAGE);
    } else {
      renderProblems(currentText, panelState, panelState === "success" ? undefined : panelMessage);
    }
  }

  async function requestCorrections(
    requestId: number,
    originalText: string,
    language: string,
  ): Promise<void> {
    const controller = new AbortController();
    activeRequest = controller;
    try {
      const response = await apiFetch("/api/text-correction", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text: originalText, language }),
        signal: controller.signal,
      });
      if (!response.ok) {
        throw new Error(await extractErrorMessage(response, ERROR_MESSAGE));
      }

      const payload = (await response.json()) as TextCorrectionResponse;
      if (
        requestId !== latestRequestId ||
        getPlainText(editor) !== originalText ||
        selectedLanguage(elements) !== language
      ) {
        return;
      }
      currentBlocks = (payload.blocks ?? []).map(cloneBlock);
      renderProblems(originalText, "success");
    } catch (error) {
      if (isAbortError(error) || requestId !== latestRequestId) {
        return;
      }
      currentBlocks = [];
      renderProblems(
        originalText,
        "error",
        error instanceof Error && error.message.trim() ? error.message : ERROR_MESSAGE,
      );
    } finally {
      if (activeRequest === controller) {
        activeRequest = null;
      }
    }
  }

  function scheduleCorrection(
    previousText: string,
    nextText: string,
    options: { forceFullCheck?: boolean; immediate?: boolean } = {},
  ): void {
    if (!options.forceFullCheck && previousText === nextText) {
      return;
    }

    latestRequestId += 1;
    if (typeof debounceHandle === "number") {
      window.clearTimeout(debounceHandle);
      debounceHandle = undefined;
    }
    abortActiveRequest();
    currentText = nextText;
    clearCorrections();

    if (isApiLocked(root)) {
      setPanelState("error", AUTH_REQUIRED_MESSAGE);
      return;
    }
    if (!nextText.trim()) {
      setPanelState("idle", IDLE_MESSAGE);
      return;
    }

    const requestId = latestRequestId;
    const language = selectedLanguage(elements);
    const immediate = options.immediate ?? options.forceFullCheck ??
      shouldTriggerCorrectionImmediately(previousText, nextText);
    renderProblems(nextText, "loading", immediate ? LOADING_MESSAGE : DEBOUNCE_MESSAGE);

    const execute = () => {
      debounceHandle = undefined;
      void requestCorrections(requestId, nextText, language);
    };
    if (immediate) {
      execute();
    } else {
      debounceHandle = window.setTimeout(execute, CORRECTION_DEBOUNCE_MS);
    }
  }

  elements.dictionaryForm.addEventListener("submit", (event) => {
    event.preventDefault();
    addDictionaryWord(elements.dictionaryInput.value);
  });
  elements.dictionaryInput.addEventListener("input", () => {
    elements.dictionaryInput.setCustomValidity("");
  });
  elements.languageSelect.addEventListener("change", () => {
    scheduleCorrection("", currentText, { forceFullCheck: true, immediate: true });
  });
  root.addEventListener("correction:retry", () => {
    scheduleCorrection("", currentText, { forceFullCheck: true, immediate: true });
  });
  root.addEventListener("click", (event) => {
    const target = event.target;
    const mark = target instanceof Element
      ? target.closest<HTMLElement>("[data-correction-index]")
      : null;
    if (mark) {
      root.dispatchEvent(
        new CustomEvent<{ index: number }>("workspace:open-correction", {
          bubbles: true,
          detail: { index: Number.parseInt(mark.dataset.correctionIndex ?? "0", 10) || 0 },
        }),
      );
    }
  });
  root.addEventListener("editor:text-changed", (event) => {
    const nextText = (event as CustomEvent<EditorTextChangedDetail>).detail.text;
    const previousText = currentText;
    currentText = nextText;
    if (isQuickActionRunning()) {
      latestRequestId += 1;
      if (typeof debounceHandle === "number") {
        window.clearTimeout(debounceHandle);
        debounceHandle = undefined;
      }
      abortActiveRequest();
      clearCorrections();
      setPanelState("idle", RUNNING_MESSAGE);
      return;
    }
    scheduleCorrection(previousText, nextText);
  });

  void dictionaryStore.load().then((words) => {
    dictionaryWords = new Set(words);
    renderDictionaryWords();
    refreshRenderedState();
  });
  renderDictionaryWords();
  setPanelState("idle", isApiLocked(root) ? AUTH_REQUIRED_MESSAGE : IDLE_MESSAGE);
}
