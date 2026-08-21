import { t } from "./ui-i18n";

interface AdvisorPdfViewerElements {
  panel: HTMLElement;
  title: HTMLElement;
  closeButton: HTMLButtonElement;
  downloadLink: HTMLAnchorElement;
  frame: HTMLIFrameElement;
}

interface AdvisorPdfViewerState {
  url: string;
  title: string;
  documentName: string;
}

function stripHash(url: string): string {
  const hashIndex = url.indexOf("#");
  return hashIndex >= 0 ? url.slice(0, hashIndex) : url;
}

function viewerUrl(url: string): string {
  return url.includes("#") ? url : `${url}#page=1`;
}

function documentName(url: string): string {
  return stripHash(url).split("/").filter(Boolean).at(-1) ?? "advisor";
}

function findElements(): AdvisorPdfViewerElements | null {
  const panel = document.querySelector<HTMLElement>("[data-advisor-pdf-viewer]");
  const title = panel?.querySelector<HTMLElement>("[data-advisor-pdf-title]");
  const closeButton = panel?.querySelector<HTMLButtonElement>("[data-advisor-pdf-close]");
  const downloadLink = panel?.querySelector<HTMLAnchorElement>("[data-advisor-pdf-download]");
  const frame = panel?.querySelector<HTMLIFrameElement>("[data-advisor-pdf-frame]");

  return panel && title && closeButton && downloadLink && frame
    ? { panel, title, closeButton, downloadLink, frame }
    : null;
}

function openRequest(trigger: HTMLElement): AdvisorPdfViewerState | null {
  const isCatalogDocument = trigger.matches("[data-advisor-open]");
  const url = (
    isCatalogDocument
      ? trigger.dataset.advisorDocUrl
      : trigger.dataset.advisorViewerUrl
  )?.trim();

  if (!url) {
    return null;
  }

  const fallbackTitle = isCatalogDocument ? t("viewer.docFallback") : t("viewer.hitDocFallback");

  return {
    url,
    title: trigger.dataset.advisorDocTitle?.trim() || fallbackTitle,
    documentName: trigger.dataset.advisorDocName?.trim() || documentName(url),
  };
}

export function mountAdvisorPdfViewer(): void {
  const elements = findElements();

  if (!elements) {
    return;
  }

  const viewer = elements;
  let lastOpenTrigger: HTMLElement | null = null;

  function close(): void {
    viewer.panel.hidden = true;
    viewer.title.textContent = t("viewer.docFallback");
    viewer.downloadLink.href = "#";
    viewer.frame.src = "about:blank";

    if (lastOpenTrigger && document.contains(lastOpenTrigger)) {
      lastOpenTrigger.focus();
    }
  }

  function open(trigger: HTMLElement, request: AdvisorPdfViewerState): void {
    lastOpenTrigger = trigger;
    viewer.panel.hidden = false;
    viewer.title.textContent = request.title;
    viewer.downloadLink.href = stripHash(request.url);
    viewer.downloadLink.download = `${request.documentName}.pdf`;
    viewer.frame.src = viewerUrl(request.url);
    viewer.panel.scrollIntoView({ behavior: "smooth", block: "nearest" });
    viewer.closeButton.focus();
  }

  document.addEventListener("click", (event) => {
    const target = event.target;

    if (!(target instanceof Element)) {
      return;
    }

    if (target.closest("[data-advisor-pdf-close]")) {
      event.preventDefault();
      close();
      return;
    }

    const trigger = target.closest<HTMLElement>(
      "[data-advisor-open], [data-advisor-result-detail-open]",
    );
    const request = trigger ? openRequest(trigger) : null;

    if (trigger && request) {
      event.preventDefault();
      open(trigger, request);
    }
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && !viewer.panel.hidden) {
      event.preventDefault();
      close();
    }
  });

  close();
}
