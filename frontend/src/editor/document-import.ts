import type { Editor } from "@tiptap/core";

import { apiFetch } from "./api-fetch";

import { isApiLocked } from "./auth";
import { setEditorHtml } from "./editor-content";
import { extractErrorMessage } from "./http-error";
import { mapTextLanguageToOcr } from "./import-language";
import type { DocumentImportElements } from "./types";
import { t } from "./ui-i18n";

const IDLE_MESSAGE = "";
const DEFAULT_ERROR_MESSAGE = t("import.status.defaultError");
const AUTH_REQUIRED_MESSAGE = t("import.status.authRequired");

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

export function mountDocumentImport(
  editor: Editor,
  root: HTMLElement,
  elements: DocumentImportElements,
): void {
  let activeRequest: AbortController | null = null;
  const toolbarUploadButton = root.querySelector<HTMLButtonElement>("[data-editor-action='upload']");
  const externalDropTarget = root.querySelector<HTMLElement>("[data-editor-import-drop-target]");
  const workspaceStatus = document.querySelector<HTMLElement>("[data-workspace-status]");

  function setPanelState(
    state: "idle" | "loading" | "success" | "error",
    message: string,
  ): void {
    elements.panel.dataset.documentImportState = state;
    elements.panel.setAttribute("aria-busy", state === "loading" ? "true" : "false");
    elements.status.setAttribute("role", state === "error" ? "alert" : "status");
    elements.status.setAttribute("aria-live", state === "error" ? "assertive" : "polite");
    elements.status.setAttribute("aria-atomic", "true");
    elements.status.textContent = message;
    if (workspaceStatus && message) {
      workspaceStatus.dataset.state = state;
      workspaceStatus.textContent = message;
      workspaceStatus.hidden = false;
      workspaceStatus.setAttribute("role", state === "error" ? "alert" : "status");
    }
    root.dispatchEvent(
      new CustomEvent("document-import:state", {
        bubbles: true,
        detail: { state, message },
      }),
    );
  }

  function setBusy(busy: boolean): void {
    const authLocked = isApiLocked(root);

    elements.button.disabled = busy || authLocked;
    elements.input.disabled = busy || authLocked;
    elements.ocrLanguageSelect.disabled = busy || authLocked;
    elements.button.setAttribute("aria-disabled", elements.button.disabled ? "true" : "false");
    elements.input.setAttribute("aria-disabled", elements.input.disabled ? "true" : "false");
    elements.ocrLanguageSelect.setAttribute(
      "aria-disabled",
      elements.ocrLanguageSelect.disabled ? "true" : "false",
    );
    elements.dropzone.dataset.busy = busy ? "true" : "false";
    elements.dropzone.dataset.authLocked = authLocked ? "true" : "false";
    elements.dropzone.setAttribute("aria-busy", busy ? "true" : "false");
    elements.dropzone.setAttribute("aria-disabled", busy || authLocked ? "true" : "false");
    toolbarUploadButton?.toggleAttribute("disabled", busy || authLocked);
    root.dataset.documentImportRunning = busy ? "true" : "false";
    editor.setEditable(!busy && root.dataset.quickActionRunning !== "true");
    root.dispatchEvent(
      new CustomEvent("workspace:busy-changed", {
        bubbles: true,
        detail: { busy, view: "editor" },
      }),
    );
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
        t("import.status.unsupportedFormat", {
          formats: elements.labels,
        }),
      );
      elements.input.value = "";
      return;
    }

    const controller = new AbortController();
    const formData = new FormData();
    const selectedTextLanguage =
      root.querySelector<HTMLSelectElement>("[data-correction-language]")?.value ??
      elements.ocrLanguageSelect.value;
    const ocrLanguage = mapTextLanguageToOcr(selectedTextLanguage);
    elements.ocrLanguageSelect.value = ocrLanguage;
    const ocrLabel =
      elements.ocrLanguageSelect.selectedOptions.item(0)?.textContent?.trim() ?? ocrLanguage;

    formData.append("file", file);
    activeRequest = controller;
    setBusy(true);
    setPanelState(
      "loading",
      t("import.status.loading", {
        fileName: file.name,
        ocrLabel,
      }),
    );

    try {
      const response = await apiFetch(
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
      setPanelState("success", t("import.status.success", { fileName: file.name }));
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
  toolbarUploadButton?.addEventListener("click", openFilePicker);

  elements.dropzone.addEventListener("click", (event) => {
    const target = event.target;

    if (
      target === elements.input ||
      (target instanceof Element &&
        target.closest("button,select,option,textarea,input,a,label"))
    ) {
      return;
    }

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

  if (externalDropTarget) {
    externalDropTarget.addEventListener("dragenter", (event) => {
      event.preventDefault();
      if (!elements.input.disabled) {
        externalDropTarget.dataset.dragging = "true";
      }
    });
    externalDropTarget.addEventListener("dragover", (event) => {
      event.preventDefault();
      if (!elements.input.disabled) {
        externalDropTarget.dataset.dragging = "true";
      }
    });
    externalDropTarget.addEventListener("dragleave", (event) => {
      const nextTarget = event.relatedTarget;
      if (!(nextTarget instanceof Node) || !externalDropTarget.contains(nextTarget)) {
        externalDropTarget.dataset.dragging = "false";
      }
    });
    externalDropTarget.addEventListener("drop", (event) => {
      event.preventDefault();
      externalDropTarget.dataset.dragging = "false";
      if (elements.input.disabled) {
        return;
      }
      const [file] = Array.from(event.dataTransfer?.files ?? []);
      if (file) {
        void importFile(file);
      }
    });
  }

  setBusy(false);

  if (isApiLocked(root)) {
    setPanelState("error", AUTH_REQUIRED_MESSAGE);
    return;
  }

  setPanelState("idle", IDLE_MESSAGE);
}
