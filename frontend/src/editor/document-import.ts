import type { Editor } from "@tiptap/core";

import { apiFetch } from "./api-fetch";
import { isApiLocked } from "./auth";
import { getPlainText, plainTextDocument } from "./plain-text";
import { dispatchWorkspaceBusy } from "./events";
import { extractErrorMessage } from "./http-error";
import { mapTextLanguageToOcr } from "./import-language";
import type { DocumentImportElements } from "./types";
import { t } from "./ui-i18n";

const DEFAULT_ERROR_MESSAGE = t("import.status.defaultError");

interface DocumentConversionResponse {
  text: string;
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
  const foundUploadButton = root.querySelector<HTMLButtonElement>("[data-editor-action='upload']");
  const foundDropTarget = root.querySelector<HTMLElement>("[data-editor-import-drop-target]");
  const workspaceStatus = root.querySelector<HTMLElement>("[data-workspace-status]");

  if (!foundUploadButton || !foundDropTarget) {
    return;
  }
  const uploadButton = foundUploadButton;
  const dropTarget = foundDropTarget;

  function setState(state: "idle" | "loading" | "success" | "error", message: string): void {
    if (workspaceStatus) {
      workspaceStatus.dataset.state = state;
      workspaceStatus.textContent = message;
      workspaceStatus.hidden = message.length === 0;
      workspaceStatus.setAttribute("role", state === "error" ? "alert" : "status");
    }
  }

  function syncControls(): void {
    const busy = root.dataset.workspaceBusy === "true";
    const locked = isApiLocked(root);
    elements.input.disabled = busy || locked;
    uploadButton.disabled = busy || locked;
    dropTarget.setAttribute("aria-busy", String(busy));
  }

  async function importFile(file: File): Promise<void> {
    if (root.dataset.workspaceBusy === "true" || isApiLocked(root)) return;
    if (!isSupportedFile(file, elements.input.accept)) {
      setState("error", t("import.status.unsupportedFormat", { formats: elements.labels }));
      elements.input.value = "";
      return;
    }

    const original = getPlainText(editor);
    const formData = new FormData();
    const textLanguage = document.querySelector<HTMLSelectElement>("[data-workspace-language]")?.value ?? "auto";
    const ocrLanguage = mapTextLanguageToOcr(textLanguage);
    formData.append("file", file);
    dispatchWorkspaceBusy(root, true, "editor", "document-import");
    setState("loading", t("import.status.loading", {
      fileName: file.name,
      ocrLabel: ocrLanguage.toUpperCase(),
    }));

    try {
      const response = await apiFetch(
        `/api/convert/doc?ocrLanguage=${encodeURIComponent(ocrLanguage)}`,
        { method: "POST", body: formData },
      );
      if (!response.ok) {
        throw new Error(await extractErrorMessage(response, DEFAULT_ERROR_MESSAGE));
      }

      const payload = (await response.json()) as DocumentConversionResponse;
      if (typeof payload.text !== "string" || !payload.text.trim() || getPlainText(editor) !== original) {
        throw new Error(DEFAULT_ERROR_MESSAGE);
      }
      editor.commands.setContent(plainTextDocument(payload.text));
      editor.commands.focus("start");
      setState("success", t("import.status.success", { fileName: file.name }));
    } catch (error) {
      setState(
        "error",
        error instanceof Error && error.message.trim() ? error.message : DEFAULT_ERROR_MESSAGE,
      );
    } finally {
      dispatchWorkspaceBusy(root, false, "editor");
      dropTarget.dataset.dragging = "false";
      elements.input.value = "";
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

  root.addEventListener("workspace:busy-changed", syncControls);
  syncControls();
  setState(
    isApiLocked(root) ? "error" : "idle",
    isApiLocked(root) ? t("import.status.authRequired") : "",
  );
}
