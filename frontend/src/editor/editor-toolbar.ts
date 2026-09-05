import { createDocxBlob, textbuddyDocxFilename } from "./docx-download";
import type { EditorTextChangedDetail, WorkspaceBusyChangedDetail } from "./types";
import { t } from "./ui-i18n";

export function mountEditorToolbar(): void {
  const root = document.querySelector<HTMLElement>("#editor-island-root");
  const copyButton = root?.querySelector<HTMLButtonElement>("[data-editor-action='copy']");
  const downloadButton = root?.querySelector<HTMLButtonElement>("[data-editor-action='download']");
  const uploadButton = root?.querySelector<HTMLButtonElement>("[data-editor-action='upload']");
  const statsButton = root?.querySelector<HTMLButtonElement>("[data-editor-action='stats']");
  const statsPopover = root?.querySelector<HTMLElement>("[data-stats-popover]");
  const toolbarStatus = root?.querySelector<HTMLElement>("[data-editor-toolbar-status]");
  const mirror = root?.querySelector<HTMLTextAreaElement>("[data-editor-mirror]");

  if (
    !root ||
    !copyButton ||
    !downloadButton ||
    !uploadButton ||
    !statsButton ||
    !statsPopover ||
    !toolbarStatus ||
    !mirror
  ) {
    return;
  }

  const resolvedRoot = root;
  const resolvedCopyButton = copyButton;
  const resolvedDownloadButton = downloadButton;
  const resolvedUploadButton = uploadButton;
  const resolvedStatsButton = statsButton;
  const resolvedStatsPopover = statsPopover;
  const resolvedToolbarStatus = toolbarStatus;
  const resolvedMirror = mirror;

  let busy = false;

  function announce(state: "success" | "error", message: string): void {
    resolvedToolbarStatus.dataset.state = state;
    resolvedToolbarStatus.textContent = message;
    resolvedToolbarStatus.setAttribute("role", state === "error" ? "alert" : "status");
    window.setTimeout(() => {
      if (resolvedToolbarStatus.textContent === message) {
        resolvedToolbarStatus.textContent = "";
      }
    }, 2500);
  }

  function syncAvailability(text = resolvedMirror.value): void {
    const hasText = text.trim().length > 0;
    resolvedCopyButton.disabled = busy || !hasText;
    resolvedDownloadButton.disabled = busy || !hasText;
    resolvedUploadButton.disabled = busy;
  }

  function closeStats(restoreFocus = false): void {
    resolvedStatsPopover.hidden = true;
    resolvedStatsButton.setAttribute("aria-expanded", "false");
    if (restoreFocus) {
      resolvedStatsButton.focus();
    }
  }

  resolvedCopyButton.addEventListener("click", async () => {
    try {
      await navigator.clipboard.writeText(resolvedMirror.value);
      announce("success", t("toolbar.copySuccess"));
    } catch {
      announce("error", t("toolbar.copyError"));
    }
  });

  resolvedDownloadButton.addEventListener("click", async () => {
    try {
      resolvedDownloadButton.disabled = true;
      const blob = await createDocxBlob(resolvedMirror.value);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = textbuddyDocxFilename();
      document.body.append(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
      announce("success", t("toolbar.downloadSuccess"));
    } catch {
      announce("error", t("toolbar.downloadError"));
    } finally {
      syncAvailability();
    }
  });

  resolvedStatsButton.addEventListener("click", () => {
    const open = resolvedStatsPopover.hidden;
    resolvedStatsPopover.hidden = !open;
    resolvedStatsButton.setAttribute("aria-expanded", open ? "true" : "false");
  });

  document.addEventListener("click", (event) => {
    const target = event.target;
    if (
      resolvedStatsPopover.hidden ||
      !(target instanceof Node) ||
      resolvedStatsPopover.contains(target) ||
      resolvedStatsButton.contains(target)
    ) {
      return;
    }
    closeStats();
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && !resolvedStatsPopover.hidden) {
      event.preventDefault();
      closeStats(true);
    }
  });
  resolvedRoot.addEventListener("editor:text-changed", (event) => {
    const detail = (event as CustomEvent<EditorTextChangedDetail>).detail;
    syncAvailability(detail.text);
  });
  resolvedRoot.addEventListener("workspace:busy-changed", (event) => {
    busy = (event as CustomEvent<WorkspaceBusyChangedDetail>).detail.busy;
    syncAvailability();
    if (busy) {
      closeStats();
    }
  });

  closeStats();
  syncAvailability();
}
