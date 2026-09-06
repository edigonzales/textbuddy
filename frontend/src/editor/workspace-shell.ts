import type { CorrectionStateChangedDetail, WorkspaceBusyChangedDetail } from "./types";

type WorkspaceMode = "transform" | "validate";
type ValidationPanel = "correction" | "advisor" | null;

export interface CorrectionRailVisibilityState {
  mode: WorkspaceMode;
  count: number;
  dismissed: boolean;
  busy: boolean;
  view: WorkspaceBusyChangedDetail["view"];
}

export function shouldShowCorrectionRail(state: CorrectionRailVisibilityState): boolean {
  return state.mode === "validate" && state.count > 0 && !state.dismissed
    && !state.busy && state.view === "editor";
}

export function dismissedAfterCorrectionCountChange(
  dismissed: boolean,
  previousCount: number,
  nextCount: number,
  mode: WorkspaceMode,
): boolean {
  if (nextCount === 0 || (previousCount === 0 && mode === "validate")) return false;
  return dismissed;
}

function focusableElements(container: HTMLElement): HTMLElement[] {
  return Array.from(container.querySelectorAll<HTMLElement>(
    "button:not(:disabled), select:not(:disabled), input:not(:disabled), a[href], [tabindex]:not([tabindex='-1'])",
  )).filter((element) => !element.hidden && !element.closest("[hidden], [inert]")
    && element.getClientRects().length > 0);
}

export function mountWorkspaceShell(): void {
  const root = document.querySelector<HTMLElement>("#editor-island-root")!;
  const modeButtons = Array.from(document.querySelectorAll<HTMLButtonElement>("button[data-workspace-mode]"));
  const ribbons = Array.from(document.querySelectorAll<HTMLElement>("[data-workspace-ribbon]"));
  const rail = root.querySelector<HTMLElement>("[data-validation-rail]")!;
  const correctionPanel = root.querySelector<HTMLElement>("[data-correction-panel]")!;
  const advisorPanel = root.querySelector<HTMLElement>("[data-advisor-panel]")!;
  const closeButtons = Array.from(root.querySelectorAll<HTMLButtonElement>("[data-validation-rail-close]"));
  const overlay = root.querySelector<HTMLElement>("[data-correction-overlay]");
  const correctionButton = document.querySelector<HTMLButtonElement>("[data-correction-results-toggle]")!;
  const advisorButton = document.querySelector<HTMLButtonElement>("[data-advisor-toggle]")!;
  const resultCount = document.querySelector<HTMLElement>("[data-correction-result-count]")!;
  const modeBadge = document.querySelector<HTMLElement>("[data-correction-mode-badge]")!;
  const ribbonStatus = document.querySelector<HTMLElement>("[data-correction-ribbon-status]")!;
  const retryButton = document.querySelector<HTMLButtonElement>("[data-correction-retry]");
  if (!root || !rail || !correctionPanel || !advisorPanel || !correctionButton || !advisorButton
      || !resultCount || !modeBadge || !ribbonStatus || modeButtons.length === 0) return;

  let mode: WorkspaceMode = "transform";
  let activePanel: ValidationPanel = null;
  let count = 0;
  let dismissed = false;
  let busy = false;
  let view: WorkspaceBusyChangedDetail["view"] = "editor";
  let railOpener: HTMLElement | null = null;
  const mobileQuery = window.matchMedia("(max-width: 767px)");

  function railShouldBeOpen(): boolean {
    if (mode !== "validate" || dismissed || view !== "editor" || activePanel === null) return false;
    if (activePanel === "advisor") return true;
    return count > 0 && !busy;
  }

  function syncRail(): void {
    const open = railShouldBeOpen();
    rail.hidden = !open;
    correctionPanel.hidden = !open || activePanel !== "correction";
    advisorPanel.hidden = !open || activePanel !== "advisor";
    rail.setAttribute("aria-hidden", open ? "false" : "true");
    correctionButton.setAttribute("aria-expanded", open && activePanel === "correction" ? "true" : "false");
    advisorButton.setAttribute("aria-expanded", open && activePanel === "advisor" ? "true" : "false");
    if (overlay) overlay.hidden = !open;
    document.body.dataset.correctionSlideoverOpen = open && mobileQuery.matches ? "true" : "false";
  }

  function setMode(nextMode: WorkspaceMode): void {
    if (busy || view === "diff-review") return;
    const changed = mode !== nextMode;
    mode = nextMode;
    if (changed && mode === "validate" && count > 0 && activePanel === null) activePanel = "correction";
    if (changed && mode === "validate") dismissed = false;
    root.dataset.workspaceMode = mode;
    modeButtons.forEach((button) => {
      const active = button.dataset.workspaceMode === mode;
      button.setAttribute("aria-selected", active ? "true" : "false");
      button.tabIndex = active ? 0 : -1;
    });
    ribbons.forEach((ribbon) => { ribbon.hidden = ribbon.dataset.workspaceRibbon !== mode; });
    syncRail();
  }

  function updateCount(nextCount: number): void {
    const previousCount = count;
    count = nextCount;
    resultCount.textContent = String(count);
    correctionButton.disabled = count === 0 || busy;
    correctionButton.setAttribute("aria-disabled", correctionButton.disabled ? "true" : "false");
    modeBadge.textContent = String(count);
    modeBadge.hidden = count === 0;
    dismissed = dismissedAfterCorrectionCountChange(dismissed, previousCount, count, mode);
    if (previousCount === 0 && count > 0 && mode === "validate" && activePanel === null) {
      activePanel = "correction";
    }
    syncRail();
  }

  function openPanel(panel: Exclude<ValidationPanel, null>, opener: HTMLElement): void {
    if (busy && root.dataset.workspaceBusySource !== "advisor") return;
    railOpener = opener;
    if (!rail.hidden && activePanel === panel) {
      dismissed = true;
      syncRail();
      return;
    }
    activePanel = panel;
    dismissed = false;
    syncRail();
    root.dispatchEvent(new CustomEvent("validation:panel-changed", { bubbles: true, detail: { panel } }));
    if (panel === "advisor") root.dispatchEvent(new CustomEvent("advisor:opened", { bubbles: true }));
    if (mobileQuery.matches) closeButtons.find((button) => !button.closest("[hidden]"))?.focus();
  }

  function dismissRail(): void {
    dismissed = true;
    syncRail();
    (railOpener ?? (activePanel === "advisor" ? advisorButton : correctionButton)).focus();
  }

  modeButtons.forEach((button, index) => {
    button.addEventListener("click", () => setMode(button.dataset.workspaceMode === "validate" ? "validate" : "transform"));
    button.addEventListener("keydown", (event) => {
      if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") return;
      event.preventDefault();
      const direction = event.key === "ArrowRight" ? 1 : -1;
      const next = modeButtons.at((index + direction + modeButtons.length) % modeButtons.length);
      next?.click();
      next?.focus();
    });
  });
  correctionButton.addEventListener("click", () => openPanel("correction", correctionButton));
  advisorButton.addEventListener("click", () => openPanel("advisor", advisorButton));
  closeButtons.forEach((button) => button.addEventListener("click", dismissRail));
  overlay?.addEventListener("click", dismissRail);
  retryButton?.addEventListener("click", () => root.dispatchEvent(new CustomEvent("correction:retry", { bubbles: true })));

  root.addEventListener("correction:state-changed", (event) => {
    const detail = (event as CustomEvent<CorrectionStateChangedDetail>).detail;
    ribbonStatus.textContent = detail.message;
    ribbonStatus.dataset.state = detail.state;
    retryButton?.toggleAttribute("hidden", detail.state !== "error");
    updateCount(detail.count);
  });
  root.addEventListener("workspace:open-correction", (event) => {
    const detail = (event as CustomEvent<{ index?: number }>).detail;
    setMode("validate");
    railOpener = correctionButton;
    activePanel = "correction";
    dismissed = false;
    syncRail();
    root.dispatchEvent(new CustomEvent("validation:panel-changed", {
      bubbles: true,
      detail: { panel: "correction" },
    }));
    const item = correctionPanel.querySelector<HTMLElement>(`[data-correction-focus-index='${detail?.index ?? 0}']`);
    window.setTimeout(() => {
      item?.scrollIntoView({ block: "center" });
      item?.focus();
    }, 0);
  });
  root.addEventListener("workspace:busy-changed", (event) => {
    const detail = (event as CustomEvent<WorkspaceBusyChangedDetail>).detail;
    busy = detail.busy;
    view = detail.view;
    modeButtons.forEach((button) => { button.disabled = busy; });
    correctionButton.disabled = busy || count === 0;
    advisorButton.disabled = busy;
    syncRail();
  });

  document.addEventListener("keydown", (event) => {
    if (rail.hidden || !mobileQuery.matches) return;
    if (event.key === "Escape") {
      event.preventDefault();
      dismissRail();
      return;
    }
    if (event.key !== "Tab") return;
    const focusable = focusableElements(rail);
    const first = focusable.at(0);
    const last = focusable.at(-1);
    if (!first || !last) return;
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
