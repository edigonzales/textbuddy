import { isApiLocked } from "./auth";
import { appendUniqueAdvisorValidationEvent, createAdvisorValidationKey } from "./advisor-validation-state";
import { findAdvisorValidationElements } from "./dom";
import { postAdvisorValidationSse } from "./advisor-validation-sse";
import type { AdvisorValidationEventPayload } from "./types";

const RUNNING_LABEL = "Pruefung laeuft...";
const IDLE_LABEL = "Pruefung starten";
const DEFAULT_ERROR_MESSAGE = "Advisor-Pruefung konnte nicht abgeschlossen werden.";
const AUTH_REQUIRED_MESSAGE = "Mit OIDC anmelden, um die Advisor-Pruefung zu starten.";
const EMPTY_RESULTS_MESSAGE =
  "Noch keine Advisor-Treffer. Starte die Pruefung mit markierten Dokumenten.";

interface ActiveValidationState {
  controller: AbortController;
  selectedDocs: string[];
  receivedEvents: number;
  failed: boolean;
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException
    ? error.name === "AbortError"
    : error instanceof Error && error.name === "AbortError";
}

function pluralizeResults(count: number): string {
  return `${count} Treffer`;
}

export function mountAdvisorValidation(): void {
  const resolvedElements = findAdvisorValidationElements();
  const resolvedEditorRoot = document.querySelector<HTMLElement>("#editor-island-root");
  const resolvedMirror = document.querySelector<HTMLTextAreaElement>("[data-editor-mirror]");

  if (!resolvedElements || !resolvedEditorRoot || !resolvedMirror) {
    return;
  }

  const elements = resolvedElements;
  const editorRoot = resolvedEditorRoot;
  const mirror = resolvedMirror;

  let activeValidation: ActiveValidationState | null = null;
  let results: AdvisorValidationEventPayload[] = [];
  let selectedResultKey = "";

  function selectedDocs(): string[] {
    return elements.docCheckboxes.filter((checkbox) => checkbox.checked).map((checkbox) => checkbox.value);
  }

  function setPanelState(
    state: "idle" | "streaming" | "success" | "error",
    message: string,
  ): void {
    elements.panel.dataset.advisorState = state;
    elements.status.textContent = message;
  }

  function syncValidateButton(): void {
    if (activeValidation) {
      elements.validateButton.disabled = true;
      elements.validateButton.textContent = RUNNING_LABEL;
      return;
    }

    elements.validateButton.textContent = IDLE_LABEL;
    elements.validateButton.disabled =
      isApiLocked(editorRoot) ||
      mirror.value.trim().length === 0 || selectedDocs().length === 0;
  }

  function syncIdleStatus(): void {
    if (activeValidation) {
      return;
    }

    if (isApiLocked(editorRoot)) {
      setPanelState("error", AUTH_REQUIRED_MESSAGE);
      return;
    }

    const docs = selectedDocs();
    const hasText = mirror.value.trim().length > 0;

    if (!hasText) {
      setPanelState("idle", "Schreibe Text im Editor und waehle dann Referenzdokumente fuer die Advisor-Pruefung.");
      return;
    }

    if (docs.length === 0) {
      setPanelState("idle", "Waehle mindestens ein Referenzdokument fuer die Advisor-Pruefung.");
      return;
    }

    setPanelState(
      "idle",
      `${docs.length} Dokument${docs.length === 1 ? "" : "e"} ausgewaehlt. Advisor-Pruefung kann gestartet werden.`,
    );
  }

  function renderSelectedResult(): void {
    const selectedResult = results.find(
      (result) => createAdvisorValidationKey(result) === selectedResultKey,
    );

    if (!selectedResult) {
      elements.detailPanel.hidden = true;
      elements.detailTitle.textContent = "";
      elements.detailReference.textContent = "";
      elements.detailMatch.textContent = "";
      elements.detailMessage.textContent = "";
      elements.detailExcerpt.textContent = "";
      elements.detailSuggestion.textContent = "";
      elements.detailLink.href = "#";
      return;
    }

    elements.detailPanel.hidden = false;
    elements.detailTitle.textContent = selectedResult.ruleTitle;
    elements.detailReference.textContent = `${selectedResult.documentTitle}, ${selectedResult.pageLabel}`;
    elements.detailMatch.textContent =
      selectedResult.matchedText.trim().length > 0
        ? `Treffer im Text: ${selectedResult.matchedText}`
        : "Treffer ohne markierten Ausdruck";
    elements.detailMessage.textContent = selectedResult.message;
    elements.detailExcerpt.textContent = selectedResult.excerpt;
    elements.detailSuggestion.textContent = selectedResult.suggestion;
    elements.detailLink.href = selectedResult.referenceUrl;
  }

  function renderResults(): void {
    elements.resultCount.textContent = pluralizeResults(results.length);
    elements.resultEmpty.hidden = results.length > 0;

    elements.resultList.replaceChildren(
      ...results.map((result) => {
        const article = document.createElement("article");
        article.className = "advisor-result";
        article.dataset.selected =
          createAdvisorValidationKey(result) === selectedResultKey ? "true" : "false";
        article.setAttribute("data-testid", "advisor-result-item");

        const button = document.createElement("button");
        button.type = "button";
        button.className = "advisor-result-button";
        button.setAttribute("data-testid", "advisor-result-select");
        button.addEventListener("click", () => {
          selectedResultKey = createAdvisorValidationKey(result);
          renderResults();
          renderSelectedResult();
        });

        const title = document.createElement("strong");
        title.className = "advisor-result-title";
        title.textContent = result.ruleTitle;
        title.setAttribute("data-testid", "advisor-result-title");

        const reference = document.createElement("span");
        reference.className = "advisor-result-reference";
        reference.textContent = `${result.documentTitle}, ${result.pageLabel}`;
        reference.setAttribute("data-testid", "advisor-result-reference");

        const excerpt = document.createElement("span");
        excerpt.className = "advisor-result-excerpt";
        excerpt.textContent = result.excerpt;

        button.append(title, reference, excerpt);
        article.append(button);

        return article;
      }),
    );

    renderSelectedResult();
  }

  function resetResults(): void {
    results = [];
    selectedResultKey = "";
    elements.resultEmpty.textContent = EMPTY_RESULTS_MESSAGE;
    renderResults();
  }

  async function runValidation(): Promise<void> {
    if (isApiLocked(editorRoot)) {
      syncValidateButton();
      setPanelState("error", AUTH_REQUIRED_MESSAGE);
      return;
    }

    const docs = selectedDocs();
    const text = mirror.value.trim();

    if (docs.length === 0 || text.length === 0) {
      syncValidateButton();
      syncIdleStatus();
      return;
    }

    activeValidation = {
      controller: new AbortController(),
      selectedDocs: docs,
      receivedEvents: 0,
      failed: false,
    };
    resetResults();
    syncValidateButton();
    setPanelState("streaming", `Advisor-Pruefung laeuft fuer ${docs.length} Dokument${docs.length === 1 ? "" : "e"}...`);

    try {
      await postAdvisorValidationSse("/api/advisor/validate", {
        body: {
          text,
          docs,
        },
        signal: activeValidation.controller.signal,
        onValidation: (payload) => {
          if (!activeValidation) {
            return;
          }

          activeValidation.receivedEvents += 1;
          const mergeResult = appendUniqueAdvisorValidationEvent(results, payload);

          results = mergeResult.events;

          if (mergeResult.added && selectedResultKey.length === 0) {
            selectedResultKey = createAdvisorValidationKey(payload);
          }

          renderResults();
          setPanelState(
            "streaming",
            `${results.length} eindeutige Treffer aus ${activeValidation.receivedEvents} empfangenen Events.`,
          );
        },
        onError: (payload) => {
          if (!activeValidation) {
            return;
          }

          activeValidation.failed = true;
          setPanelState("error", payload.message || DEFAULT_ERROR_MESSAGE);
        },
      });

      if (!activeValidation) {
        return;
      }

      const completedValidation = activeValidation;

      activeValidation = null;
      syncValidateButton();

      if (completedValidation.failed) {
        return;
      }

      if (results.length === 0) {
        setPanelState(
          "success",
          `Keine Treffer in ${completedValidation.selectedDocs.length} Dokument${completedValidation.selectedDocs.length === 1 ? "" : "en"} gefunden.`,
        );
        return;
      }

      setPanelState(
        "success",
        `${results.length} eindeutige Treffer aus ${completedValidation.receivedEvents} empfangenen Events.`,
      );
    } catch (error) {
      if (isAbortError(error)) {
        return;
      }

      activeValidation = null;
      syncValidateButton();
      setPanelState(
        "error",
        error instanceof Error && error.message.trim().length > 0
          ? error.message
          : DEFAULT_ERROR_MESSAGE,
      );
    }
  }

  elements.validateButton.addEventListener("click", () => {
    void runValidation();
  });

  elements.docCheckboxes.forEach((checkbox) => {
    checkbox.addEventListener("change", () => {
      syncValidateButton();
      syncIdleStatus();
    });
  });

  editorRoot.addEventListener("editor:text-changed", () => {
    syncValidateButton();
    syncIdleStatus();
  });

  resetResults();
  syncValidateButton();
  syncIdleStatus();
}
