import { t } from "./ui-i18n";

export function mountLocalModeStatus(): void {
  const status = document.querySelector<HTMLElement>("[data-local-mode-status]");
  const trigger = status?.querySelector<HTMLButtonElement>("[data-local-mode-trigger]");
  const popover = status?.querySelector<HTMLElement>("[data-local-mode-popover]");

  if (!status || !trigger || !popover) {
    return;
  }

  const resolvedStatus = status;
  const resolvedTrigger = trigger;
  const resolvedPopover = popover;
  let pinned = false;

  function sync(open: boolean, restoreFocus = false): void {
    resolvedPopover.hidden = !open;
    resolvedTrigger.dataset.open = String(open);
    resolvedTrigger.setAttribute("aria-expanded", String(open));
    resolvedTrigger.setAttribute(
      "aria-label",
      t(open ? "auth.localMode.close" : "auth.localMode.open"),
    );

    if (!open && restoreFocus) {
      resolvedTrigger.focus();
    }
  }

  function openTemporarily(): void {
    if (!pinned) {
      sync(true);
    }
  }

  function closeIfTemporary(): void {
    if (!pinned && !resolvedTrigger.matches(":focus")) {
      sync(false);
    }
  }

  resolvedTrigger.addEventListener("pointerenter", openTemporarily);
  resolvedStatus.addEventListener("pointerleave", closeIfTemporary);
  resolvedTrigger.addEventListener("focus", openTemporarily);
  resolvedTrigger.addEventListener("blur", (event) => {
    const relatedTarget = event.relatedTarget;
    if (!pinned && (!(relatedTarget instanceof Node) || !resolvedStatus.contains(relatedTarget))) {
      sync(false);
    }
  });
  resolvedTrigger.addEventListener("click", () => {
    pinned = !pinned;
    sync(pinned);
  });

  document.addEventListener("pointerdown", (event) => {
    const target = event.target;
    if (!(target instanceof Node) || !resolvedStatus.contains(target)) {
      pinned = false;
      sync(false);
    }
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && !popover.hidden) {
      event.preventDefault();
      pinned = false;
      sync(false, true);
    }
  });

  sync(false);
}
