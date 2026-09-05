import type { Editor } from "@tiptap/core";

import { apiFetch } from "./api-fetch";
import { isApiLocked } from "./auth";
import { importedHtmlToPlainText, setEditorPlainText } from "./editor-content";
import { extractErrorMessage } from "./http-error";
import { mapTextLanguageToOcr } from "./import-language";
import type { DocumentImportElements } from "./types";
import { t } from "./ui-i18n";

const DEFAULT_ERROR_MESSAGE = t("import.status.defaultError");

interface DocumentConversionResponse {
  html: string;
}

function isSupportedFile(file: File, accept: string): boolean {
  const filename = file.name.trim().toLowerCase();
  const contentType = file.type.trim().toLowerCase();
  return accept
    .split(",")
    .map((value) => value.trim().toLowerCase())
    .filter(Boolean)
    .some((token) => token.startsWith(".") ? filename.endsWith(token) : contentType === token);
}

export function mountDocumentImport(
  editor: Editor,
  root: HTMLElement,
  elements: DocumentImportElements,
): void {
  let activeRequest: AbortController | null = null;
  const foundUploadButton = root.querySelector<HTMLButtonElement>("[data-editor-action='upload']");
  const foundDropTarget = root.querySelector<HTMLElement>("[data-editor-import-drop-target]");
  const workspaceStatus = root.querySelector<HTMLElement>("[data-workspace-status]");

  if (!foundUploadButton || !foundDropTarget) {
    return;
  }
  const uploadButton = foundUploadButton;
  const dropTarget = foundDropTarget;

  function setState(state: "idle" | "loading" | "success" | "error", message: string): void {
    root.dataset.documentImportState = state;
    if (workspaceStatus) {
      workspaceStatus.dataset.state = state;
      workspaceStatus.textContent = message;
      workspaceStatus.hidden = message.length === 0;
      workspaceStatus.setAttribute("role", state === "error" ? "alert" : "status");
    }
    root.dispatchEvent(
      new CustomEvent("document-import:state", { bubbles: true, detail: { state, message } }),
    );
  }

  function setBusy(busy: boolean): void {
    const disabled = busy || isApiLocked(root);
    elements.input.disabled = disabled;
    uploadButton.disabled = disabled;
    dropTarget.dataset.busy = busy ? "true" : "false";
    dropTarget.dataset.authLocked = isApiLocked(root) ? "true" : "false";
    dropTarget.setAttribute("aria-busy", busy ? "true" : "false");
    root.dataset.documentImportRunning = busy ? "true" : "false";
    editor.setEditable(!busy && root.dataset.quickActionRunning !== "true");
    root.dispatchEvent(
      new CustomEvent("workspace:busy-changed", {
        bubbles: true,
        detail: { busy, view: "editor" },
      }),
    );
  }

  async function importFile(file: File): Promise<void> {
    activeRequest?.abort();
    if (!isSupportedFile(file, elements.input.accept)) {
      setState("error", t("import.status.unsupportedFormat", { formats: elements.labels }));
      elements.input.value = "";
      return;
    }

    const controller = new AbortController();
    const formData = new FormData();
    const textLanguage = document.querySelector<HTMLSelectElement>("[data-workspace-language]")?.value ?? "auto";
    const ocrLanguage = mapTextLanguageToOcr(textLanguage);
    formData.append("file", file);
    activeRequest = controller;
    setBusy(true);
    setState("loading", t("import.status.loading", {
      fileName: file.name,
      ocrLabel: ocrLanguage.toUpperCase(),
    }));

    try {
      const response = await apiFetch(
        `/api/convert/doc?ocrLanguage=${encodeURIComponent(ocrLanguage)}`,
        { method: "POST", body: formData, signal: controller.signal },
      );
      if (!response.ok) {
        throw new Error(await extractErrorMessage(response, DEFAULT_ERROR_MESSAGE));
      }

      const payload = (await response.json()) as DocumentConversionResponse;
      if (activeRequest !== controller) {
        return;
      }
      setEditorPlainText(editor, importedHtmlToPlainText(payload.html ?? ""));
      editor.commands.focus("start");
      setState("success", t("import.status.success", { fileName: file.name }));
    } catch (error) {
      if (!controller.signal.aborted) {
        setState(
          "error",
          error instanceof Error && error.message.trim() ? error.message : DEFAULT_ERROR_MESSAGE,
        );
      }
    } finally {
      if (activeRequest === controller) {
        activeRequest = null;
        setBusy(false);
        dropTarget.dataset.dragging = "false";
        elements.input.value = "";
      }
    }
  }

  uploadButton.addEventListener("click", () => {
    if (!elements.input.disabled) {
      elements.input.click();
    }
  });
  elements.input.addEventListener("change", () => {
    const file = elements.input.files?.item(0);
    if (file) {
      void importFile(file);
    }
  });
  dropTarget.addEventListener("dragenter", (event) => {
    event.preventDefault();
    if (!elements.input.disabled) {
      dropTarget.dataset.dragging = "true";
    }
  });
  dropTarget.addEventListener("dragover", (event) => event.preventDefault());
  dropTarget.addEventListener("dragleave", (event) => {
    const nextTarget = event.relatedTarget;
    if (!(nextTarget instanceof Node) || !dropTarget.contains(nextTarget)) {
      dropTarget.dataset.dragging = "false";
    }
  });
  dropTarget.addEventListener("drop", (event) => {
    event.preventDefault();
    dropTarget.dataset.dragging = "false";
    const file = event.dataTransfer?.files.item(0);
    if (file && !elements.input.disabled) {
      void importFile(file);
    }
  });

  setBusy(false);
  setState(
    isApiLocked(root) ? "error" : "idle",
    isApiLocked(root) ? t("import.status.authRequired") : "",
  );
}
