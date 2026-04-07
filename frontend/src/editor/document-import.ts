import type { Editor } from "@tiptap/core";

import { isApiLocked } from "./auth";
import { setEditorHtml } from "./editor-content";
import { extractErrorMessage } from "./http-error";
import type { DocumentImportElements } from "./types";

const IDLE_MESSAGE = "Bereit für Upload oder Drag-and-Drop.";
const DEFAULT_ERROR_MESSAGE = "Dokument konnte nicht importiert werden.";
const AUTH_REQUIRED_MESSAGE = "Mit OIDC anmelden, um Dokumente zu importieren.";
const DEFAULT_OCR_LANGUAGE = "de";

interface DocumentConversionResponse {
  html: string;
}

function extractAcceptTokens(accept: string): string[] {
  return accept
    .split(",")
    .map((value) => value.trim().toLowerCase())
    .filter((value) => value.length > 0);
}

function fileMatchesToken(file: File, token: string): boolean {
  const filename = file.name.trim().toLowerCase();
  const contentType = file.type.trim().toLowerCase();

  if (token.startsWith(".")) {
    return filename.endsWith(token);
  }

  if (token.endsWith("/*")) {
    const prefix = token.slice(0, token.length - 1);
    return contentType.startsWith(prefix);
  }

  return contentType === token;
}

function isSupportedFile(file: File, accept: string): boolean {
  const tokens = extractAcceptTokens(accept);

  if (tokens.length === 0) {
    return true;
  }

  return tokens.some((token) => fileMatchesToken(file, token));
}

function resolveOcrLanguage(value: string): string {
  const normalized = value.trim().toLowerCase();

  if (normalized === "de" || normalized === "en" || normalized === "fr" || normalized === "it") {
    return normalized;
  }

  return DEFAULT_OCR_LANGUAGE;
}

export function mountDocumentImport(
  editor: Editor,
  root: HTMLElement,
  elements: DocumentImportElements,
): void {
  let activeRequest: AbortController | null = null;

  function setPanelState(
    state: "idle" | "loading" | "success" | "error",
    message: string,
  ): void {
    elements.panel.dataset.documentImportState = state;
    elements.status.textContent = message;
  }

  function setBusy(busy: boolean): void {
    const authLocked = isApiLocked(root);

    elements.button.disabled = busy || authLocked;
    elements.input.disabled = busy || authLocked;
    elements.ocrLanguageSelect.disabled = busy || authLocked;
    elements.dropzone.dataset.busy = busy ? "true" : "false";
    elements.dropzone.dataset.authLocked = authLocked ? "true" : "false";
  }

  function openFilePicker(): void {
    if (elements.input.disabled) {
      return;
    }

    elements.input.click();
  }

  async function importFile(file: File): Promise<void> {
    activeRequest?.abort();

    if (!isSupportedFile(file, elements.input.accept)) {
      setPanelState(
        "error",
        `Nicht unterstütztes Format. Erlaubt sind: ${elements.labels}.`,
      );
      elements.input.value = "";
      return;
    }

    const controller = new AbortController();
    const formData = new FormData();
    const ocrLanguage = resolveOcrLanguage(elements.ocrLanguageSelect.value);
    const ocrLabel =
      elements.ocrLanguageSelect.selectedOptions.item(0)?.textContent?.trim() ?? ocrLanguage;

    formData.append("file", file);
    activeRequest = controller;
    setBusy(true);
    setPanelState("loading", `Konvertiere ${file.name} (OCR: ${ocrLabel})...`);

    try {
      const response = await fetch(
        `/api/convert/doc?ocrLanguage=${encodeURIComponent(ocrLanguage)}`,
        {
          method: "POST",
          body: formData,
          signal: controller.signal,
        },
      );

      if (!response.ok) {
        throw new Error(await extractErrorMessage(response, DEFAULT_ERROR_MESSAGE));
      }

      const payload = (await response.json()) as DocumentConversionResponse;

      setEditorHtml(editor, payload.html ?? "");
      editor.commands.focus("start");
      setPanelState("success", `${file.name} wurde importiert.`);
    } catch (error) {
      if (controller.signal.aborted) {
        return;
      }

      const message =
        error instanceof Error && error.message.trim().length > 0
          ? error.message
          : DEFAULT_ERROR_MESSAGE;
      setPanelState("error", message);
    } finally {
      if (activeRequest === controller) {
        activeRequest = null;
      }

      setBusy(false);
      elements.dropzone.dataset.dragging = "false";
      elements.input.value = "";
    }
  }

  elements.button.addEventListener("click", () => {
    openFilePicker();
  });

  elements.dropzone.addEventListener("click", (event) => {
    if (event.target === elements.input) {
      return;
    }

    openFilePicker();
  });

  elements.dropzone.addEventListener("keydown", (event) => {
    if (event.key !== "Enter" && event.key !== " ") {
      return;
    }

    event.preventDefault();
    openFilePicker();
  });

  elements.input.addEventListener("change", () => {
    const [file] = Array.from(elements.input.files ?? []);

    if (!file) {
      return;
    }

    void importFile(file);
  });

  elements.dropzone.addEventListener("dragenter", (event) => {
    event.preventDefault();

    if (elements.input.disabled) {
      return;
    }

    elements.dropzone.dataset.dragging = "true";
  });

  elements.dropzone.addEventListener("dragover", (event) => {
    event.preventDefault();

    if (elements.input.disabled) {
      return;
    }

    elements.dropzone.dataset.dragging = "true";
  });

  elements.dropzone.addEventListener("dragleave", (event) => {
    const nextTarget = event.relatedTarget;

    if (nextTarget instanceof Node && elements.dropzone.contains(nextTarget)) {
      return;
    }

    elements.dropzone.dataset.dragging = "false";
  });

  elements.dropzone.addEventListener("drop", (event) => {
    event.preventDefault();
    elements.dropzone.dataset.dragging = "false";

    if (elements.input.disabled) {
      return;
    }

    const [file] = Array.from(event.dataTransfer?.files ?? []);

    if (!file) {
      return;
    }

    void importFile(file);
  });

  setBusy(false);

  if (isApiLocked(root)) {
    setPanelState("error", AUTH_REQUIRED_MESSAGE);
    return;
  }

  setPanelState("idle", IDLE_MESSAGE);
}
