import type {
  CorrectionStateChangedDetail,
  WorkspaceBusyChangedDetail,
} from "./types";

type WorkspaceMode = "transform" | "validate";

export interface CorrectionRailVisibilityState {
  mode: WorkspaceMode;
  count: number;
  dismissed: boolean;
  busy: boolean;
  view: WorkspaceBusyChangedDetail["view"];
}

export function shouldShowCorrectionRail(state: CorrectionRailVisibilityState): boolean {
  return (
    state.mode === "validate" &&
    state.count > 0 &&
    !state.dismissed &&
    !state.busy &&
    state.view === "editor"
  );
}

export function dismissedAfterCorrectionCountChange(
  dismissed: boolean,
  previousCount: number,
  nextCount: number,
  mode: WorkspaceMode,
): boolean {
  if (nextCount === 0 || (previousCount === 0 && mode === "validate")) {
    return false;
  }
  return dismissed;
}

function focusableElements(container: HTMLElement): HTMLElement[] {
  return Array.from(
    container.querySelectorAll<HTMLElement>(
      "button:not(:disabled), select:not(:disabled), input:not(:disabled), a[href], [tabindex]:not([tabindex='-1'])",
    ),
  ).filter(
    (element) =>
      !element.hidden &&
      !element.closest("[hidden], [inert]") &&
      element.getClientRects().length > 0,
  );
}

export function mountWorkspaceShell(): void {
  const root = document.querySelector<HTMLElement>("#editor-island-root");
  const modeButtons = Array.from(
    document.querySelectorAll<HTMLButtonElement>("button[data-workspace-mode]"),
  );
  const ribbons = Array.from(
    document.querySelectorAll<HTMLElement>("[data-workspace-ribbon]"),
  );
  const rail = root?.querySelector<HTMLElement>("[data-correction-rail]");
  const correctionPanel = root?.querySelector<HTMLElement>("[data-correction-panel]");
  const closeButton = root?.querySelector<HTMLButtonElement>("[data-correction-rail-close]");
  const overlay = root?.querySelector<HTMLElement>("[data-correction-overlay]");
  const resultButton = document.querySelector<HTMLButtonElement>("[data-correction-results-toggle]");
  const resultCount = document.querySelector<HTMLElement>("[data-correction-result-count]");
  const modeBadge = document.querySelector<HTMLElement>("[data-correction-mode-badge]");
  const ribbonStatus = document.querySelector<HTMLElement>("[data-correction-ribbon-status]");
  const retryButton = document.querySelector<HTMLButtonElement>("[data-correction-retry]");

  if (
    !root ||
    modeButtons.length === 0 ||
    ribbons.length === 0 ||
    !rail ||
    !correctionPanel ||
    !resultButton ||
    !resultCount ||
    !modeBadge ||
    !ribbonStatus
  ) {
    return;
  }

  const resolvedRoot = root;
  const resolvedRail = rail;
  const resolvedCorrectionPanel = correctionPanel;
  const resolvedResultButton = resultButton;
  const resolvedResultCount = resultCount;
  const resolvedModeBadge = modeBadge;
  const resolvedRibbonStatus = ribbonStatus;

  let mode: WorkspaceMode = "transform";
  let count = 0;
  let dismissed = false;
  let busy = false;
  let view: WorkspaceBusyChangedDetail["view"] = "editor";
  let railOpener: HTMLElement | null = null;
  const mobileQuery = window.matchMedia("(max-width: 767px)");

  function railShouldBeOpen(): boolean {
    return shouldShowCorrectionRail({ mode, count, dismissed, busy, view });
  }

  function syncRail(): void {
    const open = railShouldBeOpen();

    resolvedRail.hidden = !open;
    resolvedCorrectionPanel.hidden = !open;
    resolvedRail.setAttribute("aria-hidden", open ? "false" : "true");
    resolvedResultButton.setAttribute("aria-expanded", open ? "true" : "false");
    if (overlay) {
      overlay.hidden = !open;
    }
    document.body.dataset.correctionSlideoverOpen = open && mobileQuery.matches ? "true" : "false";
  }

  function setMode(nextMode: WorkspaceMode): void {
    if (busy || view === "diff-review") {
      return;
    }

    const modeChanged = mode !== nextMode;
    mode = nextMode;
    if (modeChanged && mode === "validate" && count > 0) {
      dismissed = false;
    }
    resolvedRoot.dataset.workspaceMode = mode;
    modeButtons.forEach((button) => {
      const active = button.dataset.workspaceMode === mode;
      button.setAttribute("aria-selected", active ? "true" : "false");
      button.tabIndex = active ? 0 : -1;
    });
    ribbons.forEach((ribbon) => {
      ribbon.hidden = ribbon.dataset.workspaceRibbon !== mode;
    });
    if (mode === "validate" && count > 0 && !dismissed) {
      railOpener = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    }
    syncRail();
  }

  function updateCount(nextCount: number): void {
    const previousCount = count;
    count = nextCount;
    resolvedResultCount.textContent = String(count);
    resolvedResultButton.disabled = count === 0 || busy;
    resolvedResultButton.setAttribute("aria-disabled", resolvedResultButton.disabled ? "true" : "false");
    resolvedModeBadge.textContent = String(count);
    resolvedModeBadge.hidden = count === 0;

    dismissed = dismissedAfterCorrectionCountChange(dismissed, previousCount, count, mode);
    syncRail();
  }

  function dismissRail(restoreFocus = true): void {
    dismissed = true;
    syncRail();
    if (restoreFocus) {
      (railOpener ?? resolvedResultButton).focus();
    }
  }

  modeButtons.forEach((button, index) => {
    button.addEventListener("click", () => {
      setMode(button.dataset.workspaceMode === "validate" ? "validate" : "transform");
    });
    button.addEventListener("keydown", (event) => {
      if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") {
        return;
      }
      event.preventDefault();
      const direction = event.key === "ArrowRight" ? 1 : -1;
      const next = modeButtons.at((index + direction + modeButtons.length) % modeButtons.length);
      next?.click();
      next?.focus();
    });
  });

  resolvedResultButton.addEventListener("click", () => {
    railOpener = resolvedResultButton;
    if (!resolvedRail.hidden) {
      dismissRail(false);
      return;
    }
    dismissed = false;
    syncRail();
    if (mobileQuery.matches) {
      closeButton?.focus();
    }
  });
  closeButton?.addEventListener("click", () => dismissRail());
  overlay?.addEventListener("click", () => dismissRail());
  retryButton?.addEventListener("click", () => {
    resolvedRoot.dispatchEvent(new CustomEvent("correction:retry", { bubbles: true }));
  });

  resolvedRoot.addEventListener("correction:state-changed", (event) => {
    const detail = (event as CustomEvent<CorrectionStateChangedDetail>).detail;
    resolvedRibbonStatus.textContent = detail.message;
    resolvedRibbonStatus.dataset.state = detail.state;
    retryButton?.toggleAttribute("hidden", detail.state !== "error");
    updateCount(detail.count);
  });
  resolvedRoot.addEventListener("workspace:open-correction", (event) => {
    const detail = (event as CustomEvent<{ index?: number }>).detail;
    dismissed = false;
    setMode("validate");
    syncRail();
    const item = resolvedCorrectionPanel.querySelector<HTMLElement>(
      `[data-correction-focus-index='${detail?.index ?? 0}']`,
    );
    window.setTimeout(() => {
      item?.scrollIntoView({ block: "center" });
      item?.focus();
    }, 0);
  });
  resolvedRoot.addEventListener("workspace:busy-changed", (event) => {
    const detail = (event as CustomEvent<WorkspaceBusyChangedDetail>).detail;
    busy = detail.busy;
    view = detail.view;
    modeButtons.forEach((button) => {
      button.disabled = busy;
    });
    resolvedResultButton.disabled = busy || count === 0;
    syncRail();
  });

  document.addEventListener("keydown", (event) => {
    if (resolvedRail.hidden || !mobileQuery.matches) {
      return;
    }
    if (event.key === "Escape") {
      event.preventDefault();
      dismissRail();
      return;
    }
    if (event.key !== "Tab") {
      return;
    }
    const focusable = focusableElements(resolvedRail);
    const first = focusable.at(0);
    const last = focusable.at(-1);
    if (!first || !last) {
      return;
    }
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  });

  updateCount(0);
  setMode("transform");
}
