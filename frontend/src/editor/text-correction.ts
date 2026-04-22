import type { Editor } from "@tiptap/core";

import { isApiLocked } from "./auth";
import {
  plainTextRangeToDocumentRange,
  setTextCorrections,
} from "./correction-mark-extension";
import { extractErrorMessage } from "./http-error";
import {
  createLocalDictionaryStore,
  filterCorrectionBlocksByDictionary,
  isDictionaryWord,
  normalizeDictionaryWord,
} from "./local-dictionary";
import { getPlainText, plainTextToHtml } from "./plain-text";
import { normalizeRequestedLanguage } from "./request-language";
import {
  diffTextCorrectionSegments,
  segmentTextForCorrection,
  shouldTriggerCorrectionImmediately,
  type TextCorrectionSegment,
} from "./text-correction-segments";
import type {
  CorrectionElements,
  EditorTextChangedDetail,
  TextCorrectionBlock,
  TextCorrectionResponse,
} from "./types";

const CORRECTION_DEBOUNCE_MS = 350;
const IDLE_MESSAGE = "Schreibe Text, um Korrekturen zu sehen.";
const DEBOUNCE_MESSAGE = "Prüfung nach Tipp-Pause...";
const LOADING_MESSAGE = "Prüfe geänderte Segmente...";
const ERROR_MESSAGE = "Korrekturen konnten gerade nicht geladen werden.";
const AUTH_REQUIRED_MESSAGE = "Mit OIDC anmelden, um Korrekturen zu laden.";
const STREAMING_MESSAGE = "Rewrite-Stream läuft gerade.";

interface SegmentCorrectionState extends TextCorrectionSegment {
  blocks: TextCorrectionBlock[];
}

function createProblemBadge(index: number): HTMLElement {
  const badge = document.createElement("span");

  badge.className = "problem-index";
  badge.textContent = `Problem ${index + 1}`;

  return badge;
}

function extractProblemText(original: string, block: TextCorrectionBlock): string {
  const problemText = original.slice(block.offset, block.offset + block.length);
  return problemText || "Leerer Bereich";
}

function cloneBlock(block: TextCorrectionBlock): TextCorrectionBlock {
  return {
    ...block,
    replacements: [...block.replacements],
  };
}

function createSegmentState(
  segment: TextCorrectionSegment,
  blocks: readonly TextCorrectionBlock[] = [],
): SegmentCorrectionState {
  return {
    start: segment.start,
    end: segment.end,
    text: segment.text,
    blocks: blocks.map(cloneBlock),
  };
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException
    ? error.name === "AbortError"
    : error instanceof Error && error.name === "AbortError";
}

function getSelectedLanguage(elements: CorrectionElements): string {
  return normalizeRequestedLanguage(elements.languageSelect.value);
}

export function mountTextCorrectionBridge(
  editor: Editor,
  root: HTMLElement,
  elements: CorrectionElements,
): void {
  let debounceHandle: number | undefined;
  let latestRequestId = 0;
  let currentText = getPlainText(editor);
  let panelState: "idle" | "loading" | "success" | "error" = "idle";
  let panelMessage = IDLE_MESSAGE;
  let segmentStates: SegmentCorrectionState[] = [];
  let dictionaryWords = new Set<string>();

  const dictionaryStore = createLocalDictionaryStore();
  const inFlightRequests = new Set<AbortController>();

  function isQuickActionStreaming(): boolean {
    return root.dataset.quickActionStreaming === "true";
  }

  function setPanelState(state: "idle" | "loading" | "success" | "error", message: string): void {
    panelState = state;
    panelMessage = message;
    elements.panel.dataset.correctionState = state;
    elements.panel.setAttribute("aria-busy", state === "loading" ? "true" : "false");
    elements.status.setAttribute("role", state === "error" ? "alert" : "status");
    elements.status.setAttribute("aria-live", state === "error" ? "assertive" : "polite");
    elements.status.setAttribute("aria-atomic", "true");
    elements.status.textContent = message;
  }

  function abortInFlightRequests(): void {
    inFlightRequests.forEach((controller) => {
      controller.abort();
    });
    inFlightRequests.clear();
  }

  function clearCorrections(): void {
    segmentStates = [];
    setTextCorrections(editor, []);
    elements.list.replaceChildren();
  }

  function focusProblem(block: TextCorrectionBlock): void {
    const range = plainTextRangeToDocumentRange(editor.state.doc, block.offset, block.length);

    if (!range) {
      return;
    }

    editor.chain().focus().setTextSelection(range).run();
  }

  function applySuggestion(block: TextCorrectionBlock, replacement: string): void {
    const activeText = getPlainText(editor);

    if (block.offset < 0 || block.offset + block.length > activeText.length) {
      return;
    }

    const nextText =
      activeText.slice(0, block.offset) +
      replacement +
      activeText.slice(block.offset + block.length);

    editor.commands.setContent(plainTextToHtml(nextText), {
      emitUpdate: true,
    });
    editor.commands.focus();
  }

  function createProblemItem(
    original: string,
    block: TextCorrectionBlock,
    index: number,
  ): HTMLElement {
    const item = document.createElement("article");
    const header = document.createElement("div");
    const fragment = document.createElement("code");
    const title = document.createElement("p");
    const detail = document.createElement("p");
    const suggestions = document.createElement("div");
    const dictionaryButton = document.createElement("button");

    item.className = "problem-item";
    item.dataset.testid = "correction-problem-item";
    item.tabIndex = 0;
    item.setAttribute("role", "button");
    item.setAttribute("aria-label", `Problem ${index + 1}: ${extractProblemText(original, block)}`);

    header.className = "problem-item-head";
    header.append(createProblemBadge(index));

    fragment.className = "problem-fragment";
    fragment.textContent = extractProblemText(original, block);
    header.append(fragment);

    title.className = "problem-message";
    title.textContent = block.shortMessage || block.message || "Korrektur";

    detail.className = "problem-detail";
    detail.textContent = block.message || "Es liegt ein Problem in diesem Textbereich vor.";

    suggestions.className = "problem-suggestions";

    if (block.replacements.length === 0) {
      const emptyState = document.createElement("span");

      emptyState.className = "problem-empty";
      emptyState.textContent = "Kein Vorschlag";
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

    const problemText = extractProblemText(original, block);

    if (isDictionaryWord(problemText)) {
      dictionaryButton.type = "button";
      dictionaryButton.className = "dictionary-inline-button";
      dictionaryButton.dataset.testid = "dictionary-add-problem";
      dictionaryButton.textContent = "Als bekannt merken";
      dictionaryButton.addEventListener("click", (event) => {
        event.stopPropagation();
        addDictionaryWord(problemText);
      });
      suggestions.append(dictionaryButton);
    }

    item.addEventListener("click", () => {
      focusProblem(block);
    });

    item.addEventListener("keydown", (event) => {
      if (event.key !== "Enter" && event.key !== " ") {
        return;
      }

      event.preventDefault();
      focusProblem(block);
    });

    item.append(header, title, detail, suggestions);

    return item;
  }

  function buildVisibleBlocks(original: string): TextCorrectionBlock[] {
    const mergedBlocks = segmentStates.flatMap((segmentState) =>
      segmentState.blocks.map((block) => ({
        ...cloneBlock(block),
        offset: segmentState.start + block.offset,
      })),
    );

    return filterCorrectionBlocksByDictionary(original, mergedBlocks, dictionaryWords);
  }

  function renderProblems(
    original: string,
    state: "idle" | "loading" | "success" | "error",
    message?: string,
  ): void {
    const blocks = buildVisibleBlocks(original);

    setTextCorrections(
      editor,
      blocks.map((block) => ({
        offset: block.offset,
        length: block.length,
      })),
    );

    if (blocks.length === 0) {
      elements.list.replaceChildren();
    } else {
      elements.list.replaceChildren(
        ...blocks.map((block, index) => createProblemItem(original, block, index)),
      );
    }

    if (state === "success") {
      setPanelState(
        "success",
        blocks.length === 0
          ? "Keine Probleme gefunden."
          : `${blocks.length} Problem${blocks.length === 1 ? "" : "e"} gefunden.`,
      );
      return;
    }

    setPanelState(state, message ?? panelMessage);
  }

  function renderDictionaryWords(): void {
    const words = [...dictionaryWords].sort((left, right) => left.localeCompare(right));

    if (words.length === 0) {
      elements.dictionaryList.replaceChildren();
      elements.dictionaryEmpty.hidden = false;
      return;
    }

    const items = words.map((word) => {
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
      removeButton.textContent = "Entfernen";
      removeButton.addEventListener("click", () => {
        removeDictionaryWord(word);
      });

      item.append(label, removeButton);
      return item;
    });

    elements.dictionaryEmpty.hidden = true;
    elements.dictionaryList.replaceChildren(...items);
  }

  function refreshRenderedState(): void {
    if (isApiLocked(root)) {
      clearCorrections();
      setPanelState("error", AUTH_REQUIRED_MESSAGE);
      return;
    }

    if (!currentText.trim()) {
      clearCorrections();
      setPanelState("idle", IDLE_MESSAGE);
      return;
    }

    renderProblems(
      currentText,
      panelState,
      panelState === "success" ? undefined : panelMessage,
    );
  }

  async function persistDictionary(): Promise<void> {
    await dictionaryStore.save([...dictionaryWords]);
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
    void persistDictionary();
  }

  function removeDictionaryWord(word: string): void {
    if (!dictionaryWords.has(word)) {
      return;
    }

    const nextWords = new Set(dictionaryWords);

    nextWords.delete(word);
    dictionaryWords = nextWords;
    renderDictionaryWords();
    refreshRenderedState();
    void persistDictionary();
  }

  function reuseSegmentStates(
    previousStates: readonly SegmentCorrectionState[],
    nextSegments: readonly TextCorrectionSegment[],
    unchangedPrefixCount: number,
    unchangedSuffixCount: number,
  ): SegmentCorrectionState[] {
    const reusedStates: SegmentCorrectionState[] = [];

    reusedStates.push(
      ...nextSegments
        .slice(0, unchangedPrefixCount)
        .map((segment, index) => createSegmentState(segment, previousStates[index]?.blocks)),
    );

    reusedStates.push(
      ...nextSegments
        .slice(unchangedPrefixCount, nextSegments.length - unchangedSuffixCount)
        .map((segment) => createSegmentState(segment)),
    );

    reusedStates.push(
      ...nextSegments
        .slice(nextSegments.length - unchangedSuffixCount)
        .map((segment, index) =>
          createSegmentState(
            segment,
            previousStates[previousStates.length - unchangedSuffixCount + index]?.blocks,
          ),
        ),
    );

    return reusedStates;
  }

  async function fetchSegmentCorrections(
    segmentState: SegmentCorrectionState,
    language: string,
  ): Promise<SegmentCorrectionState> {
    if (!segmentState.text.trim()) {
      return createSegmentState(segmentState);
    }

    const controller = new AbortController();

    inFlightRequests.add(controller);

    try {
      const response = await fetch("/api/text-correction", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          text: segmentState.text,
          language,
        }),
        signal: controller.signal,
      });

      if (!response.ok) {
        throw new Error(
          await extractErrorMessage(
            response,
            `Text correction request failed with status ${response.status}`,
          ),
        );
      }

      const payload = (await response.json()) as TextCorrectionResponse;

      return createSegmentState(segmentState, payload.blocks ?? []);
    } finally {
      inFlightRequests.delete(controller);
    }
  }

  async function requestCorrections(
    changedStartIndex: number,
    changedSegments: readonly SegmentCorrectionState[],
    requestId: number,
    originalText: string,
    language: string,
  ): Promise<void> {
    try {
      const updatedSegments = await Promise.all(
        changedSegments.map((segment) => fetchSegmentCorrections(segment, language)),
      );

      if (
        requestId !== latestRequestId ||
        getPlainText(editor) !== originalText ||
        getSelectedLanguage(elements) !== language
      ) {
        return;
      }

      const nextStates = [...segmentStates];

      updatedSegments.forEach((segment, index) => {
        nextStates[changedStartIndex + index] = segment;
      });

      segmentStates = nextStates;
      renderProblems(originalText, "success");
    } catch (error) {
      if (isAbortError(error) || requestId !== latestRequestId) {
        return;
      }

      renderProblems(
        originalText,
        "error",
        error instanceof Error && error.message.trim().length > 0
          ? error.message
          : ERROR_MESSAGE,
      );
    }
  }

  function scheduleCorrection(
    previousText: string,
    nextText: string,
    options: {
      forceFullCheck?: boolean;
      immediate?: boolean;
    } = {},
  ): void {
    if (!options.forceFullCheck && previousText === nextText) {
      return;
    }

    latestRequestId += 1;

    if (typeof debounceHandle === "number") {
      window.clearTimeout(debounceHandle);
      debounceHandle = undefined;
    }

    abortInFlightRequests();

    if (isApiLocked(root)) {
      currentText = nextText;
      clearCorrections();
      setPanelState("error", AUTH_REQUIRED_MESSAGE);
      return;
    }

    if (!nextText.trim()) {
      currentText = nextText;
      clearCorrections();
      setPanelState("idle", IDLE_MESSAGE);
      return;
    }

    const nextSegments = segmentTextForCorrection(nextText);
    const diff = diffTextCorrectionSegments(
      options.forceFullCheck ? [] : segmentStates,
      nextSegments,
    );

    segmentStates = reuseSegmentStates(
      options.forceFullCheck ? [] : segmentStates,
      diff.nextSegments,
      diff.unchangedPrefixCount,
      diff.unchangedSuffixCount,
    );

    currentText = nextText;

    if (diff.changedNextSegments.length === 0) {
      renderProblems(nextText, "success");
      return;
    }

    const requestId = latestRequestId;
    const changedStartIndex = diff.unchangedPrefixCount;
    const changedEndIndex = changedStartIndex + diff.changedNextSegments.length;
    const changedStates = segmentStates.slice(changedStartIndex, changedEndIndex);
    const shouldRunImmediately =
      options.immediate ??
      options.forceFullCheck ??
      shouldTriggerCorrectionImmediately(previousText, nextText);

    renderProblems(
      nextText,
      "loading",
      shouldRunImmediately ? LOADING_MESSAGE : DEBOUNCE_MESSAGE,
    );

    const executeCorrection = () => {
      void requestCorrections(
        changedStartIndex,
        changedStates,
        requestId,
        nextText,
        getSelectedLanguage(elements),
      );
    };

    if (shouldRunImmediately) {
      executeCorrection();
      return;
    }

    debounceHandle = window.setTimeout(executeCorrection, CORRECTION_DEBOUNCE_MS);
  }

  elements.dictionaryForm.addEventListener("submit", (event) => {
    event.preventDefault();
    addDictionaryWord(elements.dictionaryInput.value);
  });

  elements.dictionaryInput.addEventListener("input", () => {
    elements.dictionaryInput.setCustomValidity("");
  });

  elements.languageSelect.addEventListener("change", () => {
    scheduleCorrection("", currentText, {
      forceFullCheck: true,
      immediate: true,
    });
  });

  root.addEventListener("editor:text-changed", (event) => {
    const detail = (event as CustomEvent<EditorTextChangedDetail>).detail;
    const previousText = currentText;

    currentText = detail.text;

    if (isQuickActionStreaming()) {
      latestRequestId += 1;

      if (typeof debounceHandle === "number") {
        window.clearTimeout(debounceHandle);
        debounceHandle = undefined;
      }

      abortInFlightRequests();
      clearCorrections();
      setPanelState("idle", STREAMING_MESSAGE);
      return;
    }

    scheduleCorrection(previousText, detail.text);
  });

  void dictionaryStore.load().then((words) => {
    dictionaryWords = new Set(words);
    renderDictionaryWords();
    refreshRenderedState();
  });

  renderDictionaryWords();
  setPanelState("idle", isApiLocked(root) ? AUTH_REQUIRED_MESSAGE : IDLE_MESSAGE);
}
