const DEFAULT_TAB = "actions";

export function mountInspectorTabs(): void {
  const root = document.querySelector<HTMLElement>("#editor-island-root");

  if (!root) {
    return;
  }

  const tabs = Array.from(root.querySelectorAll<HTMLButtonElement>("[data-inspector-tab]"));
  const panels = Array.from(root.querySelectorAll<HTMLElement>("[data-inspector-panel]"));

  if (tabs.length === 0 || panels.length === 0) {
    return;
  }

  function activate(tabName: string, focusTab = false): void {
    tabs.forEach((tab) => {
      const active = tab.dataset.inspectorTab === tabName;

      tab.setAttribute("aria-selected", active ? "true" : "false");
      tab.tabIndex = active ? 0 : -1;

      if (active && focusTab) {
        tab.focus();
      }
    });

    panels.forEach((panel) => {
      panel.hidden = panel.dataset.inspectorPanel !== tabName;
    });
  }

  function activateByIndex(index: number): void {
    const nextTab = tabs.at((index + tabs.length) % tabs.length);

    if (nextTab) {
      activate(nextTab.dataset.inspectorTab ?? DEFAULT_TAB, true);
    }
  }

  tabs.forEach((tab, index) => {
    tab.addEventListener("click", () => {
      activate(tab.dataset.inspectorTab ?? DEFAULT_TAB);
    });

    tab.addEventListener("keydown", (event) => {
      if (event.key === "ArrowRight") {
        event.preventDefault();
        activateByIndex(index + 1);
        return;
      }

      if (event.key === "ArrowLeft") {
        event.preventDefault();
        activateByIndex(index - 1);
        return;
      }

      if (event.key === "Home") {
        event.preventDefault();
        activateByIndex(0);
        return;
      }

      if (event.key === "End") {
        event.preventDefault();
        activateByIndex(tabs.length - 1);
      }
    });
  });

  activate(root.dataset.inspectorActiveTab ?? DEFAULT_TAB);
}
