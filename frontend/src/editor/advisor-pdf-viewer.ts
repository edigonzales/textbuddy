import { t } from "./ui-i18n";

interface AdvisorPdfViewerElements {
  panel: HTMLElement;
  title: HTMLElement;
  closeButton: HTMLButtonElement;
  pageInput: HTMLInputElement;
  previousButton: HTMLButtonElement;
  nextButton: HTMLButtonElement;
  zoomOutButton: HTMLButtonElement;
  zoomInButton: HTMLButtonElement;
  zoomLabel: HTMLElement;
  downloadLink: HTMLAnchorElement;
  frame: HTMLIFrameElement;
}

interface AdvisorPdfViewerState {
  docUrl: string;
  docName: string;
  title: string;
  page: number;
  zoom: number;
}

const DEFAULT_ZOOM = 100;
const MIN_ZOOM = 50;
const MAX_ZOOM = 200;
const ZOOM_STEP = 10;

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function normalizePage(value: number): number {
  if (!Number.isFinite(value)) {
    return 1;
  }

  return Math.max(1, Math.floor(value));
}

function stripHash(url: string): string {
  const hashIndex = url.indexOf("#");
  return hashIndex >= 0 ? url.slice(0, hashIndex) : url;
}

function deriveDocumentName(url: string): string {
  const path = stripHash(url);
  const segments = path.split("/").filter((segment) => segment.length > 0);
  return segments.length > 0 ? segments.at(-1) ?? "advisor" : "advisor";
}

function parsePageFromUrl(url: string): number {
  const hashIndex = url.indexOf("#");

  if (hashIndex < 0) {
    return 1;
  }

  const params = new URLSearchParams(url.slice(hashIndex + 1));
  const rawPage = Number.parseInt(params.get("page") ?? "", 10);

  return normalizePage(rawPage);
}

function buildViewerUrl(docUrl: string, page: number, zoom: number): string {
  const normalizedPage = normalizePage(page);
  const normalizedZoom = clamp(Math.round(zoom), MIN_ZOOM, MAX_ZOOM);

  return `${stripHash(docUrl)}#page=${normalizedPage}&zoom=${normalizedZoom}`;
}

function findElements(): AdvisorPdfViewerElements | null {
  const panel = document.querySelector<HTMLElement>("[data-advisor-pdf-viewer]");

  if (!panel) {
    return null;
  }

  const title = panel.querySelector<HTMLElement>("[data-advisor-pdf-title]");
  const closeButton = panel.querySelector<HTMLButtonElement>("[data-advisor-pdf-close]");
  const pageInput = panel.querySelector<HTMLInputElement>("[data-advisor-pdf-page-input]");
  const previousButton = panel.querySelector<HTMLButtonElement>("[data-advisor-pdf-prev]");
  const nextButton = panel.querySelector<HTMLButtonElement>("[data-advisor-pdf-next]");
  const zoomOutButton = panel.querySelector<HTMLButtonElement>("[data-advisor-pdf-zoom-out]");
  const zoomInButton = panel.querySelector<HTMLButtonElement>("[data-advisor-pdf-zoom-in]");
  const zoomLabel = panel.querySelector<HTMLElement>("[data-advisor-pdf-zoom-label]");
  const downloadLink = panel.querySelector<HTMLAnchorElement>("[data-advisor-pdf-download]");
  const frame = panel.querySelector<HTMLIFrameElement>("[data-advisor-pdf-frame]");

  if (
    !title ||
    !closeButton ||
    !pageInput ||
    !previousButton ||
    !nextButton ||
    !zoomOutButton ||
    !zoomInButton ||
    !zoomLabel ||
    !downloadLink ||
    !frame
  ) {
    return null;
  }

  return {
    panel,
    title,
    closeButton,
    pageInput,
    previousButton,
    nextButton,
    zoomOutButton,
    zoomInButton,
    zoomLabel,
    downloadLink,
    frame,
  };
}

function resolveOpenRequest(trigger: HTMLElement): {
  url: string;
  title: string;
  docName: string;
  page: number;
} | null {
  if (trigger.matches("[data-advisor-open]")) {
    const url = (trigger.dataset.advisorDocUrl ?? "").trim();

    if (url.length === 0) {
      return null;
    }

    return {
      url,
      title:
        (trigger.dataset.advisorDocTitle ?? t("viewer.docFallback")).trim() ||
        t("viewer.docFallback"),
      docName: (trigger.dataset.advisorDocName ?? deriveDocumentName(url)).trim() || "advisor",
      page: parsePageFromUrl(url),
    };
  }

  if (trigger.matches("[data-advisor-result-detail-open]")) {
    const url = (trigger.dataset.advisorViewerUrl ?? "").trim();

    if (url.length === 0) {
      return null;
    }

    return {
      url,
      title:
        (trigger.dataset.advisorDocTitle ?? t("viewer.hitDocFallback")).trim() ||
        t("viewer.hitDocFallback"),
      docName: deriveDocumentName(url),
      page: parsePageFromUrl(url),
    };
  }

  return null;
}

export function mountAdvisorPdfViewer(): void {
  const elements = findElements();

  if (!elements) {
    return;
  }

  const resolvedElements = elements;
  let state: AdvisorPdfViewerState | null = null;
  let lastOpenTrigger: HTMLElement | null = null;

  function render(): void {
    if (!state) {
      resolvedElements.panel.hidden = true;
      resolvedElements.title.textContent = t("viewer.docFallback");
      resolvedElements.pageInput.value = "1";
      resolvedElements.zoomLabel.textContent = `${DEFAULT_ZOOM}%`;
      resolvedElements.downloadLink.href = "#";
      resolvedElements.downloadLink.download = "advisor.pdf";
      resolvedElements.frame.src = "about:blank";
      resolvedElements.pageInput.disabled = true;
      resolvedElements.previousButton.disabled = true;
      resolvedElements.nextButton.disabled = true;
      resolvedElements.zoomOutButton.disabled = true;
      resolvedElements.zoomInButton.disabled = true;
      return;
    }

    resolvedElements.panel.hidden = false;
    resolvedElements.title.textContent = state.title;
    resolvedElements.pageInput.value = String(state.page);
    resolvedElements.zoomLabel.textContent = `${state.zoom}%`;
    resolvedElements.downloadLink.href = state.docUrl;
    resolvedElements.downloadLink.download = `${state.docName}.pdf`;
    resolvedElements.frame.src = buildViewerUrl(state.docUrl, state.page, state.zoom);
    resolvedElements.pageInput.disabled = false;
    resolvedElements.previousButton.disabled = false;
    resolvedElements.nextButton.disabled = false;
    resolvedElements.zoomOutButton.disabled = false;
    resolvedElements.zoomInButton.disabled = false;
  }

  function openViewer(url: string, title: string, docName: string, page: number): void {
    state = {
      docUrl: stripHash(url),
      title,
      docName,
      page: normalizePage(page),
      zoom: DEFAULT_ZOOM,
    };
    render();
    resolvedElements.panel.scrollIntoView({ behavior: "smooth", block: "nearest" });
    resolvedElements.closeButton.focus();
  }

  function closeViewer(): void {
    state = null;
    render();

    if (lastOpenTrigger && document.contains(lastOpenTrigger)) {
      lastOpenTrigger.focus();
      return;
    }

    document.querySelector<HTMLButtonElement>("[data-advisor-validate]")?.focus();
  }

  function updatePage(page: number): void {
    if (!state) {
      return;
    }

    state = {
      ...state,
      page: normalizePage(page),
    };
    render();
  }

  function updateZoom(nextZoom: number): void {
    if (!state) {
      return;
    }

    state = {
      ...state,
      zoom: clamp(nextZoom, MIN_ZOOM, MAX_ZOOM),
    };
    render();
  }

  document.addEventListener("click", (event) => {
    const target = event.target;

    if (!(target instanceof Element)) {
      return;
    }

    const closeTrigger = target.closest<HTMLElement>("[data-advisor-pdf-close]");

    if (closeTrigger) {
      event.preventDefault();
      closeViewer();
      return;
    }

    const openTrigger = target.closest<HTMLElement>(
      "[data-advisor-open], [data-advisor-result-detail-open]",
    );

    if (!openTrigger) {
      return;
    }

    const openRequest = resolveOpenRequest(openTrigger);

    if (!openRequest) {
      return;
    }

    event.preventDefault();
    lastOpenTrigger = openTrigger;
    openViewer(openRequest.url, openRequest.title, openRequest.docName, openRequest.page);
  });

  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape" || !state) {
      return;
    }

    event.preventDefault();
    closeViewer();
  });

  resolvedElements.previousButton.addEventListener("click", () => {
    if (!state) {
      return;
    }

    updatePage(state.page - 1);
  });

  resolvedElements.nextButton.addEventListener("click", () => {
    if (!state) {
      return;
    }

    updatePage(state.page + 1);
  });

  resolvedElements.pageInput.addEventListener("change", () => {
    const requestedPage = Number.parseInt(resolvedElements.pageInput.value, 10);

    updatePage(requestedPage);
  });

  resolvedElements.zoomOutButton.addEventListener("click", () => {
    if (!state) {
      return;
    }

    updateZoom(state.zoom - ZOOM_STEP);
  });

  resolvedElements.zoomInButton.addEventListener("click", () => {
    if (!state) {
      return;
    }

    updateZoom(state.zoom + ZOOM_STEP);
  });

  render();
}
