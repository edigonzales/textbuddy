import { expect, test, type Page } from "@playwright/test";

interface CorrectionRequestPayload {
  text: string;
  language: string;
}

interface WordSynonymRequestPayload {
  word: string;
  context: string;
}

interface SentenceRewriteRequestPayload {
  sentence: string;
}

interface QuickActionRequestPayload {
  text: string;
  language: string;
  option?: string;
  prompt?: string;
}

interface AdvisorValidateRequestPayload {
  text: string;
  docs: string[];
}

interface AdvisorValidationEventPayload {
  stableKey: string;
  documentName: string;
  documentTitle: string;
  ruleId: string;
  ruleTitle: string;
  page: number;
  pageLabel: string;
  message: string;
  matchedText: string;
  excerpt: string;
  suggestion: string;
  referenceUrl: string;
}

type InspectorTab = "correction" | "actions" | "advisor" | "import" | "stats";

function createCorrectionResponse(text: string) {
  const blocks = [];
  const tehOffset = text.indexOf("teh");
  const recieveOffset = text.indexOf("recieve");

  if (tehOffset >= 0) {
    blocks.push({
      offset: tehOffset,
      length: 3,
      message: "Possible spelling mistake found.",
      shortMessage: "Spelling",
      ruleId: "STUB_SPELLING_TEH",
      replacements: ["the"],
    });
  }

  if (recieveOffset >= 0) {
    blocks.push({
      offset: recieveOffset,
      length: 7,
      message: "Possible spelling mistake found.",
      shortMessage: "Spelling",
      ruleId: "STUB_SPELLING_RECIEVE",
      replacements: ["receive"],
    });
  }

  return {
    original: text,
    blocks,
  };
}

function createSseBody(events: Array<{ event: string; payload: unknown }>): string {
  return events
    .map(({ event, payload }) => `event: ${event}\ndata: ${JSON.stringify(payload)}\n\n`)
    .join("");
}

function createQuickActionBody(text: string): string {
  return JSON.stringify({ text });
}

async function runQuickAction(page: Page, actionTestId: string) {
  await page.getByTestId(actionTestId).click();
  await page.getByTestId("quick-action-run").click();
}

async function openInspectorTab(page: Page, tab: InspectorTab) {
  await page.getByTestId(`inspector-tab-${tab}`).click();
}

async function selectPreviousCharacters(page: Page, count: number): Promise<void> {
  await page.keyboard.down("Shift");

  for (let index = 0; index < count; index += 1) {
    await page.keyboard.press("ArrowLeft");
  }

  await page.keyboard.up("Shift");
}

test("typing updates mirror and undo redo state", async ({ page }) => {
  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");
  const characterCount = page.getByTestId("editor-character-count");
  const wordCount = page.getByTestId("editor-word-count");
  const undoButton = page.getByTestId("editor-undo");
  const redoButton = page.getByTestId("editor-redo");

  await expect(editor).toBeVisible();
  await expect(mirror).toHaveValue("");
  await expect(characterCount).toHaveText("0");
  await expect(wordCount).toHaveText("0");
  await expect(undoButton).toBeDisabled();
  await expect(redoButton).toBeDisabled();

  await editor.click();
  await page.keyboard.type("Hallo Welt");

  await expect(mirror).toHaveValue("Hallo Welt");
  await expect(characterCount).toHaveText("10");
  await expect(wordCount).toHaveText("2");
  await expect(undoButton).toBeEnabled();
  await expect(redoButton).toBeDisabled();

  await undoButton.click();

  await expect(mirror).toHaveValue("");
  await expect(characterCount).toHaveText("0");
  await expect(wordCount).toHaveText("0");
  await expect(undoButton).toBeDisabled();
  await expect(redoButton).toBeEnabled();

  await redoButton.click();

  await expect(mirror).toHaveValue("Hallo Welt");
  await expect(characterCount).toHaveText("10");
  await expect(wordCount).toHaveText("2");
});

test("text statistics panel updates counters and Flesch score", async ({ page }) => {
  await page.goto("/");
  await openInspectorTab(page, "stats");

  const editor = page.getByTestId("editor-input");

  await editor.click();
  await page.keyboard.type("Mal Tal. Ball Fall.");

  await expect(page.getByTestId("text-stats-characters")).toHaveText("19");
  await expect(page.getByTestId("text-stats-words")).toHaveText("4");
  await expect(page.getByTestId("text-stats-syllables")).toHaveText("4");
  await expect(page.getByTestId("text-stats-sentences")).toHaveText("2");
  await expect(page.getByTestId("text-stats-avg-sentence-length")).toHaveText("2.0");
  await expect(page.getByTestId("text-stats-avg-syllables-per-word")).toHaveText("1.00");
  await expect(page.getByTestId("text-stats-flesch")).toHaveText("119.5");
  await expect(page.getByTestId("text-stats-flesch-label")).toHaveText(
    "Sehr leicht verständlich",
  );
});

test("document import uploads supported files and injects html into the editor", async ({
  page,
}) => {
  let requestCount = 0;
  let releaseUpload: (() => void) | null = null;
  let importRequestUrl = "";

  await page.route("**/api/convert/doc**", async (route) => {
    requestCount += 1;
    importRequestUrl = route.request().url();

    await new Promise<void>((resolve) => {
      releaseUpload = resolve;
    });

    await route.fulfill({
      json: {
        html: "<h1>Import Titel</h1><p>Erste Zeile.</p><ul><li>Listenpunkt</li></ul>",
      },
    });
  });

  await page.goto("/");
  await openInspectorTab(page, "import");

  const importInput = page.getByTestId("document-import-input");
  const importStatus = page.getByTestId("document-import-status");
  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");

  await importInput.setInputFiles({
    name: "import.docx",
    mimeType:
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    buffer: Buffer.from("dummy"),
  });

  await expect(importStatus).toContainText("Konvertiere import.docx (OCR: Deutsch)...");

  releaseUpload?.();

  await expect(importStatus).toContainText("import.docx wurde importiert.");
  await expect(editor).toContainText("Import Titel");
  await expect(editor).toContainText("Erste Zeile.");
  await expect(editor).toContainText("Listenpunkt");
  await expect(mirror).toHaveValue(/Import Titel[\s\S]*Erste Zeile\.[\s\S]*Listenpunkt/);
  expect(requestCount).toBe(1);
  expect(importRequestUrl).toContain("ocrLanguage=de");
});

test("document import rejects unsupported formats before upload", async ({ page }) => {
  let requestCount = 0;

  await page.route("**/api/convert/doc**", async (route) => {
    requestCount += 1;
    await route.abort();
  });

  await page.goto("/");
  await openInspectorTab(page, "import");

  const importInput = page.getByTestId("document-import-input");
  const importStatus = page.getByTestId("document-import-status");
  const mirror = page.getByTestId("editor-mirror");

  await importInput.setInputFiles({
    name: "payload.exe",
    mimeType: "application/octet-stream",
    buffer: Buffer.from("noop"),
  });

  await expect(importStatus).toContainText("Nicht unterstütztes Format.");
  await expect(mirror).toHaveValue("");
  expect(requestCount).toBe(0);
});

test("document import sends selected OCR language", async ({ page }) => {
  let importRequestUrl = "";

  await page.route("**/api/convert/doc**", async (route) => {
    importRequestUrl = route.request().url();
    await route.fulfill({
      json: {
        html: "<p>OCR Import</p>",
      },
    });
  });

  await page.goto("/");
  await openInspectorTab(page, "import");

  await page.getByTestId("document-import-ocr-language").selectOption("fr");
  await page.getByTestId("document-import-input").setInputFiles({
    name: "scan.png",
    mimeType: "image/png",
    buffer: Buffer.from("dummy"),
  });

  await expect(page.getByTestId("document-import-status")).toContainText(
    "scan.png wurde importiert.",
  );
  expect(importRequestUrl).toContain("ocrLanguage=fr");
});

test("text correction marks problems and applies a suggestion", async ({ page }) => {
  await page.goto("/");
  await openInspectorTab(page, "correction");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");
  const correctionStatus = page.getByTestId("correction-status");
  const problemItems = page.getByTestId("correction-problem-item");
  const correctionMarks = page.getByTestId("correction-mark");

  await editor.click();
  await page.keyboard.type("This is teh text.");

  await expect(correctionStatus).toContainText("1 Problem");
  await expect(problemItems).toHaveCount(1);
  await expect(problemItems.first()).toContainText("teh");
  await expect(correctionMarks).toHaveCount(1);

  await problemItems.first().click();
  await page.getByTestId("correction-suggestion").first().click();

  await expect(mirror).toHaveValue("This is the text.");
  await expect(correctionStatus).toHaveText("Keine Probleme gefunden.");
  await expect(problemItems).toHaveCount(0);
  await expect(correctionMarks).toHaveCount(0);
});

test("rewrite bubble opens only for explicit word and sentence selections", async ({ page }) => {
  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const bubble = page.getByTestId("rewrite-bubble");
  const primaryAction = page.getByTestId("rewrite-primary-action");
  const secondaryAction = page.getByTestId("rewrite-secondary-action");

  await editor.click();
  await page.keyboard.type("Alpha schnell.");

  await expect(bubble).toBeHidden();

  await page.keyboard.press("ArrowLeft");
  await expect(bubble).toBeHidden();

  await selectPreviousCharacters(page, 7);

  await expect(bubble).toBeVisible();
  await expect(bubble).toHaveAttribute("aria-label", "Vorschläge für „schnell“");
  await expect(primaryAction).toHaveText("Synonyme");
  await expect(secondaryAction).toHaveText("Satz umformulieren");
  await expect(secondaryAction).toBeVisible();

  await page.keyboard.press("End");
  await selectPreviousCharacters(page, 14);

  await expect(bubble).toBeVisible();
  await expect(primaryAction).toHaveText("Satz umformulieren");
  await expect(secondaryAction).toBeHidden();
});

test("word synonym uses the focused word context and replaces only that range", async ({ page }) => {
  const requestBodies: WordSynonymRequestPayload[] = [];

  await page.route("**/api/word-synonym", async (route) => {
    const payload = route.request().postDataJSON() as WordSynonymRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      json: {
        synonyms: ["rasch"],
      },
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");
  const bubble = page.getByTestId("rewrite-bubble");

  await editor.click();
  await page.keyboard.type("Alpha schnell.");

  await expect(bubble).toBeHidden();

  await page.keyboard.press("ArrowLeft");
  await selectPreviousCharacters(page, 7);

  await expect(bubble).toBeVisible();

  await page.getByTestId("rewrite-primary-action").click();

  await expect.poll(() => requestBodies.at(-1)?.word).toBe("schnell");
  await expect.poll(() => requestBodies.at(-1)?.context).toBe("Alpha schnell.");
  await expect(page.getByTestId("rewrite-status")).toContainText("Synonym");

  await page.getByTestId("rewrite-option").first().click();

  await expect(mirror).toHaveValue("Alpha rasch.");
});

test("sentence rewrite is reachable from the word bubble and replaces only the sentence range", async ({ page }) => {
  const requestBodies: SentenceRewriteRequestPayload[] = [];

  await page.route("**/api/sentence-rewrite", async (route) => {
    const payload = route.request().postDataJSON() as SentenceRewriteRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      json: {
        sentence: payload.sentence,
        options: ["Alpha Alternative."],
      },
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");

  await editor.click();
  await page.keyboard.type("Alpha schnell. Beta Satz.");
  for (let index = 0; index < 12; index += 1) {
    await page.keyboard.press("ArrowLeft");
  }
  await selectPreviousCharacters(page, 7);

  await expect(page.getByTestId("rewrite-bubble")).toBeVisible();
  await expect(page.getByTestId("rewrite-secondary-action")).toBeVisible();

  await page.getByTestId("rewrite-secondary-action").click();

  await expect.poll(() => requestBodies.at(-1)?.sentence).toBe("Alpha schnell.");
  await expect(page.getByTestId("rewrite-status")).toContainText("Alternative");

  await page.getByTestId("rewrite-option").first().click();

  await expect(mirror).toHaveValue("Alpha Alternative. Beta Satz.");
});

test("sentence mode is reachable without word focus", async ({ page }) => {
  await page.goto("/");

  const editor = page.getByTestId("editor-input");

  await editor.click();
  await page.keyboard.type("Alpha Satz.");
  await selectPreviousCharacters(page, 11);

  await expect(page.getByTestId("rewrite-bubble")).toBeVisible();
  await expect(page.getByTestId("rewrite-primary-action")).toHaveText("Satz umformulieren");
  await expect(page.getByTestId("rewrite-secondary-action")).toBeHidden();
});

test("rewrite bubble exposes compact loading, empty, error and keyboard result states", async ({
  page,
}) => {
  let requestCount = 0;
  let releaseEmptyResponse: (() => void) | undefined;

  await page.route("**/api/word-synonym", async (route) => {
    requestCount += 1;

    if (requestCount === 1) {
      await new Promise<void>((resolve) => {
        releaseEmptyResponse = resolve;
      });
      await route.fulfill({ json: { synonyms: [] } });
      return;
    }

    if (requestCount === 2) {
      await route.fulfill({
        status: 503,
        json: { message: "Vorschlagsdienst nicht verfügbar." },
      });
      return;
    }

    await route.fulfill({ json: { synonyms: ["rasch", "flink"] } });
  });

  await page.goto("/");
  const editor = page.getByTestId("editor-input");
  const action = page.getByTestId("rewrite-primary-action");
  const status = page.getByTestId("rewrite-status");

  await editor.click();
  await page.keyboard.type("Alpha schnell.");
  await page.keyboard.press("ArrowLeft");
  await selectPreviousCharacters(page, 7);
  await action.click();

  await expect(status).toHaveText("Synonyme werden geladen...");
  await expect.poll(() => Boolean(releaseEmptyResponse)).toBe(true);
  releaseEmptyResponse?.();
  await expect(status).toHaveText("Keine Synonyme gefunden.");

  await action.click();
  await expect(status).toHaveText("Vorschlagsdienst nicht verfügbar.");

  await action.focus();
  await page.keyboard.press("Enter");
  await expect(page.getByTestId("rewrite-option")).toHaveCount(2);
  await expect(page.getByTestId("rewrite-option").first()).toBeFocused();
});

test("Escape and outside clicks dismiss the current selection and abort pending requests", async ({
  page,
}) => {
  await page.addInitScript(() => {
    const trackedWindow = window as Window & { textbuddyAbortCount?: number };
    const originalAbort = AbortController.prototype.abort;

    trackedWindow.textbuddyAbortCount = 0;
    AbortController.prototype.abort = function (...args: Parameters<AbortController["abort"]>) {
      trackedWindow.textbuddyAbortCount = (trackedWindow.textbuddyAbortCount ?? 0) + 1;
      return originalAbort.apply(this, args);
    };
  });

  let releaseRequest: (() => void) | undefined;
  await page.route("**/api/word-synonym", async (route) => {
    await new Promise<void>((resolve) => {
      releaseRequest = resolve;
    });

    await route.fulfill({ json: { synonyms: ["rasch"] } }).catch(() => undefined);
  });

  await page.goto("/");
  const editor = page.getByTestId("editor-input");
  const bubble = page.getByTestId("rewrite-bubble");

  await editor.click();
  await page.keyboard.type("Alpha schnell.");
  await page.keyboard.press("ArrowLeft");
  await selectPreviousCharacters(page, 7);
  await page.getByTestId("rewrite-primary-action").click();
  await expect(page.getByTestId("rewrite-status")).toHaveText("Synonyme werden geladen...");
  await expect.poll(() => Boolean(releaseRequest)).toBe(true);
  const abortCountBeforeEscape = await page.evaluate(() => (
    window as Window & { textbuddyAbortCount?: number }
  ).textbuddyAbortCount ?? 0);

  await page.keyboard.press("Escape");
  await expect(bubble).toBeHidden();
  await expect.poll(() => page.evaluate(() => (
    window as Window & { textbuddyAbortCount?: number }
  ).textbuddyAbortCount ?? 0)).toBeGreaterThan(abortCountBeforeEscape);
  releaseRequest?.();

  await page.locator("#editor-island-root").dispatchEvent("editor:selection-changed");
  await expect(bubble).toBeHidden();

  await editor.focus();
  await page.keyboard.press("Home");
  await page.keyboard.press("End");
  await page.keyboard.press("ArrowLeft");
  await selectPreviousCharacters(page, 7);
  await expect(bubble).toBeVisible();

  await page.locator(".app-brand").click();
  await expect(bubble).toBeHidden();
  await page.locator("#editor-island-root").dispatchEvent("editor:selection-changed");
  await expect(bubble).toBeHidden();
});

test("workspace and selection popup do not overflow desktop or mobile viewports", async ({ page }) => {
  for (const viewport of [
    { width: 1440, height: 1000 },
    { width: 390, height: 844 },
  ]) {
    await page.setViewportSize(viewport);
    await page.goto("/");

    const editor = page.getByTestId("editor-input");
    await editor.click();
    await page.keyboard.type("Ein kompakter Beispielsatz endet mit Auswahl.");
    await page.keyboard.press("ArrowLeft");
    await selectPreviousCharacters(page, 7);
    await expect(page.getByTestId("rewrite-bubble")).toBeVisible();

    const geometry = await page.evaluate(() => {
      const bubble = document.querySelector<HTMLElement>("[data-rewrite-bubble]");
      const canvas = document.querySelector<HTMLElement>("[data-editor-canvas]");
      const bubbleRect = bubble?.getBoundingClientRect();
      const canvasRect = canvas?.getBoundingClientRect();

      return {
        documentWidth: document.documentElement.scrollWidth,
        viewportWidth: document.documentElement.clientWidth,
        bubbleLeft: bubbleRect?.left ?? -1,
        bubbleRight: bubbleRect?.right ?? Number.POSITIVE_INFINITY,
        canvasLeft: canvasRect?.left ?? -1,
        canvasRight: canvasRect?.right ?? Number.POSITIVE_INFINITY,
      };
    });

    expect(geometry.documentWidth).toBeLessThanOrEqual(geometry.viewportWidth);
    expect(geometry.bubbleLeft).toBeGreaterThanOrEqual(geometry.canvasLeft + 7);
    expect(geometry.bubbleRight).toBeLessThanOrEqual(geometry.canvasRight - 7);
    expect(geometry.bubbleLeft).toBeGreaterThanOrEqual(7);
    expect(geometry.bubbleRight).toBeLessThanOrEqual(viewport.width - 7);
  }
});

test("advisor catalog shows multiple selectable documents and serves reachable PDFs", async ({
  page,
  request,
}) => {
  await page.goto("/");
  await openInspectorTab(page, "advisor");

  const advisorItems = page.getByTestId("advisor-doc-item");
  const advisorCheckboxes = page.getByTestId("advisor-doc-checkbox");
  const advisorTitles = page.getByTestId("advisor-doc-title");

  await expect(page.getByTestId("advisor-panel")).toBeVisible();
  await expect(advisorItems).toHaveCount(5);
  await expect(advisorTitles.nth(0)).toHaveText("Empfehlungen zu Anglizismen");
  await expect(advisorTitles.nth(3)).toHaveText("Schreibweisungen");

  await advisorCheckboxes.nth(1).check();
  await advisorCheckboxes.nth(3).check();

  await expect(advisorCheckboxes.nth(1)).toBeChecked();
  await expect(advisorCheckboxes.nth(3)).toBeChecked();

  const response = await request.get("/api/advisor/doc/schreibweisungen");
  const body = await response.body();

  expect(response.ok()).toBeTruthy();
  expect(response.headers()["content-type"]).toContain("application/pdf");
  expect(response.headers()["x-frame-options"]).toBe("SAMEORIGIN");
  expect(response.headers()["content-security-policy"]).toContain("frame-src 'self'");
  expect(body.toString("utf-8")).toContain("%PDF-1.4");
});

test("advisor pdf viewer opens, downloads and closes the selected document", async ({ page }) => {
  await page.goto("/");
  await openInspectorTab(page, "advisor");

  await page.getByTestId("advisor-doc-open").first().click();

  await expect(page.getByTestId("advisor-pdf-viewer")).toBeVisible();
  await expect(page.getByTestId("advisor-pdf-frame")).toHaveAttribute(
    "src",
    /\/api\/advisor\/doc\/empfehlungen-anglizismen-maerz-2020#page=1/,
  );
  await expect(page.getByTestId("advisor-pdf-download")).toHaveAttribute(
    "href",
    "/api/advisor/doc/empfehlungen-anglizismen-maerz-2020",
  );

  await page.getByTestId("advisor-pdf-close").click();
  await expect(page.getByTestId("advisor-pdf-viewer")).toBeHidden();
});

test("advisor validation streams results and deduplicates them in the panel", async ({ page }) => {
  const requestBodies: AdvisorValidateRequestPayload[] = [];

  await page.route("**/api/advisor/validate", async (route) => {
    const payload = route.request().postDataJSON() as AdvisorValidateRequestPayload;
    const firstEvent: AdvisorValidationEventPayload = {
      stableKey: "schreibweisungen::per-sofort-vermeiden::per-sofort",
      documentName: "schreibweisungen",
      documentTitle: "Schreibweisungen",
      ruleId: "per-sofort-vermeiden",
      ruleTitle: "Per sofort durch ab sofort ersetzen",
      page: 7,
      pageLabel: "Seite 1",
      message: "Die Formulierung wirkt intern und wenig standardisiert.",
      matchedText: "per sofort",
      excerpt: "Bitte handeln Sie per sofort und laden Sie die Datei herunter.",
      suggestion: "Nutze 'ab sofort'.",
      referenceUrl: "/api/advisor/doc/schreibweisungen#page=1",
    };

    requestBodies.push(payload);
    await route.fulfill({
      status: 200,
      headers: {
        "Content-Type": "text/event-stream",
      },
      body: createSseBody([
        {
          event: "validation",
          payload: firstEvent,
        },
        {
          event: "validation",
          payload: {
            ...firstEvent,
            excerpt: "Duplikat, das im Client ignoriert werden soll.",
          },
        },
        {
          event: "validation",
          payload: {
            stableKey:
              "empfehlungen-anglizismen-maerz-2020::downloaden-statt-herunterladen::downloaden",
            documentName: "empfehlungen-anglizismen-maerz-2020",
            documentTitle: "Empfehlungen zu Anglizismen",
            ruleId: "downloaden-statt-herunterladen",
            ruleTitle: "Deutsche Alternative für downloaden",
            page: 6,
            pageLabel: "Seite 1",
            message: "Der Ausdruck wirkt als vermeidbarer Anglizismus.",
            matchedText: "downloaden",
            excerpt: "Bitte downloaden Sie das Formular per sofort.",
            suggestion: "Nutze nach Möglichkeit 'herunterladen'.",
            referenceUrl: "/api/advisor/doc/empfehlungen-anglizismen-maerz-2020#page=1",
          },
        },
      ]),
    });
  });

  await page.goto("/");
  await openInspectorTab(page, "advisor");

  const editor = page.getByTestId("editor-input");
  const advisorCheckboxes = page.getByTestId("advisor-doc-checkbox");

  await editor.click();
  await page.keyboard.type("Bitte downloaden Sie das Formular per sofort.");

  await advisorCheckboxes.nth(0).check();
  await advisorCheckboxes.nth(3).check();
  await page.getByTestId("advisor-validate").click();

  await expect.poll(() => requestBodies.at(-1)?.text).toBe(
    "Bitte downloaden Sie das Formular per sofort.",
  );
  await expect.poll(() => requestBodies.at(-1)?.docs).toEqual([
    "empfehlungen-anglizismen-maerz-2020",
    "schreibweisungen",
  ]);
  await expect(page.getByTestId("advisor-result-item")).toHaveCount(2);
  await expect(page.getByTestId("advisor-result-count")).toHaveText("2 Treffer");
  await expect(page.getByTestId("advisor-status")).toContainText("2 eindeutige Treffer");
  await expect(page.getByTestId("advisor-result-detail-title")).toHaveText(
    "Per sofort durch ab sofort ersetzen",
  );
  await expect(page.getByTestId("advisor-result-detail-reference")).toContainText("Schreibweisungen");
  await expect(page.getByTestId("advisor-result-detail-reference")).toContainText("Seite 1");

  await page.getByTestId("advisor-result-select").nth(1).click();

  await expect(page.getByTestId("advisor-result-detail-title")).toHaveText(
    "Deutsche Alternative für downloaden",
  );
  await expect(page.getByTestId("advisor-result-detail-reference")).toContainText(
    "Empfehlungen zu Anglizismen",
  );
  await expect(page.getByTestId("advisor-result-detail-link")).toHaveAttribute(
    "href",
    "/api/advisor/doc/empfehlungen-anglizismen-maerz-2020#page=1",
  );

  await page.getByTestId("advisor-result-detail-open").click();
  await expect(page.getByTestId("advisor-pdf-frame")).toHaveAttribute(
    "src",
    /\/api\/advisor\/doc\/empfehlungen-anglizismen-maerz-2020#page=1/,
  );
});

test("plain language rewrites the editor, shows a diff and supports full undo", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/plain-language", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: createQuickActionBody("Kurz und einfach: Der einfache Thema ist wichtig."),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");

  await expect(page.locator("[data-quick-action]")).toHaveCount(9);

  await editor.click();
  await page.keyboard.type("Der komplizierte Sachverhalt ist relevant.");
  await runQuickAction(page, "quick-action-plain-language");

  await expect.poll(() => requestBodies.at(-1)?.text).toBe(
    "Der komplizierte Sachverhalt ist relevant.",
  );
  await expect.poll(() => requestBodies.at(-1)?.language).toBe("auto");
  await expect(page.getByTestId("quick-action-status")).toContainText("abgeschlossen");
  await expect(mirror).toHaveValue("Kurz und einfach: Der einfache Thema ist wichtig.");
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expect(page.getByTestId("rewrite-diff-before")).toContainText(
    "Der komplizierte Sachverhalt ist relevant.",
  );
  await expect(page.getByTestId("rewrite-diff-after")).toContainText(
    "Kurz und einfach: Der einfache Thema ist wichtig.",
  );

  await page.getByTestId("rewrite-diff-undo").click();

  await expect(mirror).toHaveValue("Der komplizierte Sachverhalt ist relevant.");
  await expect(page.getByTestId("rewrite-diff-panel")).toBeHidden();
  await expect(page.getByTestId("quick-action-status")).toContainText("rückgängig");
});

test("bullet points rewrite the editor, show a diff and support full undo", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/bullet-points", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: createQuickActionBody("- Projektlage klaeren\n- Naechste Schritte festhalten"),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");

  await editor.click();
  await page.keyboard.type("Projektlage klaeren. Naechste Schritte festhalten.");
  await runQuickAction(page, "quick-action-bullet-points");

  await expect.poll(() => requestBodies.at(-1)?.text).toBe(
    "Projektlage klaeren. Naechste Schritte festhalten.",
  );
  await expect.poll(() => requestBodies.at(-1)?.language).toBe("auto");
  await expect(page.getByTestId("quick-action-status")).toContainText("Stichpunkte abgeschlossen");
  await expect(mirror).toHaveValue("- Projektlage klaeren\n- Naechste Schritte festhalten");
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expect(page.getByTestId("rewrite-diff-before")).toContainText(
    "Projektlage klaeren. Naechste Schritte festhalten.",
  );
  await expect(page.getByTestId("rewrite-diff-after")).toContainText(
    "- Projektlage klaeren",
  );
  await expect(page.getByTestId("rewrite-diff-after")).toContainText(
    "- Naechste Schritte festhalten",
  );

  await page.getByTestId("rewrite-diff-undo").click();

  await expect(mirror).toHaveValue("Projektlage klaeren. Naechste Schritte festhalten.");
  await expect(page.getByTestId("rewrite-diff-panel")).toBeHidden();
  await expect(page.getByTestId("quick-action-status")).toContainText("rückgängig");
});

test("proofread rewrites the editor, shows a diff and supports full undo", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/proofread", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: createQuickActionBody("This is the text."),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");

  await editor.click();
  await page.keyboard.type("This is teh text.");
  await runQuickAction(page, "quick-action-proofread");

  await expect.poll(() => requestBodies.at(-1)?.text).toBe("This is teh text.");
  await expect.poll(() => requestBodies.at(-1)?.language).toBe("auto");
  await expect(page.getByTestId("quick-action-status")).toContainText("Korrigieren abgeschlossen");
  await expect(mirror).toHaveValue("This is the text.");
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expect(page.getByTestId("rewrite-diff-before")).toContainText("This is teh text.");
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("This is the text.");

  await page.getByTestId("rewrite-diff-undo").click();

  await expect(mirror).toHaveValue("This is teh text.");
  await expect(page.getByTestId("rewrite-diff-panel")).toBeHidden();
  await expect(page.getByTestId("quick-action-status")).toContainText("rückgängig");
});

test("summarize with the sentence option rewrites the editor and sends the selected option", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/summarize", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: createQuickActionBody("Kurzfassung: Der Kernpunkt steht fest."),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");

  await editor.click();
  await page.keyboard.type("Der Kernpunkt steht fest. Weitere Details folgen.");
  await page.getByTestId("quick-action-summarize").click();
  await expect(page.getByTestId("quick-action-summarize-option")).toHaveValue("sentence");
  await page.getByTestId("quick-action-run").click();

  await expect.poll(() => requestBodies.at(-1)?.text).toBe(
    "Der Kernpunkt steht fest. Weitere Details folgen.",
  );
  await expect.poll(() => requestBodies.at(-1)?.language).toBe("auto");
  await expect.poll(() => requestBodies.at(-1)?.option).toBe("sentence");
  await expect(page.getByTestId("quick-action-status")).toContainText("Zusammenfassen abgeschlossen");
  await expect(mirror).toHaveValue("Kurzfassung: Der Kernpunkt steht fest.");
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expect(page.getByTestId("rewrite-diff-after")).toContainText(
    "Kurzfassung: Der Kernpunkt steht fest.",
  );
});

test("summarize with the management summary option returns the selected variant", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/summarize", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: createQuickActionBody(
        "Management Summary\n- Kernpunkt: Projekt ist freigegeben.\n- Empfehlung: Umsetzung starten.",
      ),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");

  await editor.click();
  await page.keyboard.type("Projekt ist freigegeben. Umsetzung kann starten.");
  await page.getByTestId("quick-action-summarize").click();
  await page.getByTestId("quick-action-summarize-option").selectOption("management_summary");
  await page.getByTestId("quick-action-run").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("management_summary");
  await expect(page.getByTestId("quick-action-status")).toContainText("Zusammenfassen abgeschlossen");
  await expect(mirror).toHaveValue(
    "Management Summary\n- Kernpunkt: Projekt ist freigegeben.\n- Empfehlung: Umsetzung starten.",
  );
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("Management Summary");
  await expect(page.getByTestId("rewrite-diff-after")).toContainText(
    "- Empfehlung: Umsetzung starten.",
  );
});

test("formality returns formal and informal variants with the selected option", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/formality", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);

    const responseText =
      payload.option === "informal"
        ? "Lockerer formuliert: Hallo, wir brauchen schnell deine Rueckmeldung."
        : "Formell ueberarbeitet: Guten Tag, wir benoetigen zeitnah Ihre Rueckmeldung.";

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: createQuickActionBody(responseText),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");
  const optionSelect = page.getByTestId("quick-action-formality-option");

  await editor.click();
  await page.keyboard.type("Hallo, wir brauchen schnell deine Rueckmeldung.");
  await page.getByTestId("quick-action-formality").click();
  await expect(optionSelect).toHaveValue("formal");
  await page.getByTestId("quick-action-run").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("formal");
  await expect(page.getByTestId("quick-action-status")).toContainText("Ton ändern abgeschlossen");
  await expect(mirror).toHaveValue(
    "Formell ueberarbeitet: Guten Tag, wir benoetigen zeitnah Ihre Rueckmeldung.",
  );
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();

  await page.getByTestId("rewrite-diff-undo").click();

  await expect(mirror).toHaveValue("Hallo, wir brauchen schnell deine Rueckmeldung.");
  await page.getByTestId("quick-action-formality").click();
  await optionSelect.selectOption("informal");
  await page.getByTestId("quick-action-run").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("informal");
  await expect(page.getByTestId("quick-action-status")).toContainText("Ton ändern abgeschlossen");
  await expect(mirror).toHaveValue(
    "Lockerer formuliert: Hallo, wir brauchen schnell deine Rueckmeldung.",
  );
  await expect(page.getByTestId("rewrite-diff-after")).toContainText(
    "Lockerer formuliert: Hallo, wir brauchen schnell deine Rueckmeldung.",
  );
});

test("social media returns multiple channel variants with the selected option", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/social-media", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);

    const responseText =
      payload.option === "linkedin"
        ? "LinkedIn-Post: Produktstart ist live.\n\nTakeaway: Team ist bereit."
        : "Bluesky-Post: Produktstart ist live. Fokus: Team ist bereit.";

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: createQuickActionBody(responseText),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");
  const optionSelect = page.getByTestId("quick-action-social-media-option");

  await editor.click();
  await page.keyboard.type("Produktstart ist live. Team ist bereit.");
  await page.getByTestId("quick-action-social-media").click();
  await expect(optionSelect).toHaveValue("bluesky");
  await page.getByTestId("quick-action-run").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("bluesky");
  await expect(page.getByTestId("quick-action-status")).toContainText("Social Media abgeschlossen");
  await expect(mirror).toHaveValue("Bluesky-Post: Produktstart ist live. Fokus: Team ist bereit.");
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();

  await page.getByTestId("rewrite-diff-undo").click();

  await expect(mirror).toHaveValue("Produktstart ist live. Team ist bereit.");
  await page.getByTestId("quick-action-social-media").click();
  await optionSelect.selectOption("linkedin");
  await page.getByTestId("quick-action-run").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("linkedin");
  await expect(page.getByTestId("quick-action-status")).toContainText("Social Media abgeschlossen");
  await expect(mirror).toHaveValue("LinkedIn-Post: Produktstart ist live.\n\nTakeaway: Team ist bereit.");
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("LinkedIn-Post: Produktstart ist live.");
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("Takeaway: Team ist bereit.");
});

test("medium returns multiple medium variants with the selected option", async ({ page }) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/medium", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);

    const responseText =
      payload.option === "report"
        ? "Bericht\n\nZusammenfassung: Projekt ist freigegeben.\nDetails: Team startet am Montag.\nAbschluss: Umsetzung beginnt sofort."
        : "Betreff: Projektupdate\n\nHallo Team,\n\nProjekt ist freigegeben. Team startet am Montag.\n\nViele Gruesse";

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: createQuickActionBody(responseText),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");
  const optionSelect = page.getByTestId("quick-action-medium-option");

  await editor.click();
  await page.keyboard.type("Projekt ist freigegeben. Team startet am Montag.");
  await page.getByTestId("quick-action-medium").click();
  await expect(optionSelect).toHaveValue("email");
  await page.getByTestId("quick-action-run").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("email");
  await expect(page.getByTestId("quick-action-status")).toContainText("Format anpassen abgeschlossen");
  await expect(mirror).toHaveValue(
    "Betreff: Projektupdate\n\nHallo Team,\n\nProjekt ist freigegeben. Team startet am Montag.\n\nViele Gruesse",
  );
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();

  await page.getByTestId("rewrite-diff-undo").click();

  await expect(mirror).toHaveValue("Projekt ist freigegeben. Team startet am Montag.");
  await page.getByTestId("quick-action-medium").click();
  await optionSelect.selectOption("report");
  await page.getByTestId("quick-action-run").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("report");
  await expect(page.getByTestId("quick-action-status")).toContainText("Format anpassen abgeschlossen");
  await expect(mirror).toHaveValue(
    "Bericht\n\nZusammenfassung: Projekt ist freigegeben.\nDetails: Team startet am Montag.\nAbschluss: Umsetzung beginnt sofort.",
  );
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("Bericht");
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("Abschluss: Umsetzung beginnt sofort.");
});

test("character speech returns direct and indirect variants with the selected option", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/character-speech", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);

    const responseText =
      payload.option === "indirect_speech"
        ? "Indirekte Rede\n\nDie Figur sagte, dass Projekt ist freigegeben.\nDanach erklaerte die andere Figur, dass Team startet am Montag."
        : 'Direkte Rede\n\n"Projekt ist freigegeben.", sagte die Figur.\n"Team startet am Montag.", antwortete die andere Figur.';

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: createQuickActionBody(responseText),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");
  const optionSelect = page.getByTestId("quick-action-character-speech-option");

  await editor.click();
  await page.keyboard.type("Projekt ist freigegeben. Team startet am Montag.");
  await page.getByTestId("quick-action-character-speech").click();
  await expect(optionSelect).toHaveValue("direct_speech");
  await page.getByTestId("quick-action-run").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("direct_speech");
  await expect(page.getByTestId("quick-action-status")).toContainText(
    "Rede umformen abgeschlossen",
  );
  await expect(mirror).toHaveValue(
    'Direkte Rede\n\n"Projekt ist freigegeben.", sagte die Figur.\n"Team startet am Montag.", antwortete die andere Figur.',
  );
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();

  await page.getByTestId("rewrite-diff-undo").click();

  await expect(mirror).toHaveValue("Projekt ist freigegeben. Team startet am Montag.");
  await page.getByTestId("quick-action-character-speech").click();
  await optionSelect.selectOption("indirect_speech");
  await page.getByTestId("quick-action-run").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("indirect_speech");
  await expect(page.getByTestId("quick-action-status")).toContainText(
    "Rede umformen abgeschlossen",
  );
  await expect(mirror).toHaveValue(
    "Indirekte Rede\n\nDie Figur sagte, dass Projekt ist freigegeben.\nDanach erklaerte die andere Figur, dass Team startet am Montag.",
  );
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("Indirekte Rede");
  await expect(page.getByTestId("rewrite-diff-after")).toContainText(
    "Danach erklaerte die andere Figur, dass Team startet am Montag.",
  );
});

test("custom quick action sends the custom prompt and returns the result", async ({ page }) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/custom", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);

    const responseText = `Custom Rewrite\n\nAuftrag: ${payload.prompt}\n\nErgebnis:\nProjektstart ist morgen.`;

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: createQuickActionBody(responseText),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");
  const promptInput = page.getByTestId("quick-action-custom-prompt");
  const customButton = page.getByTestId("quick-action-custom");
  const runButton = page.getByTestId("quick-action-run");

  await editor.click();
  await page.keyboard.type("Projektstart ist morgen.");
  await customButton.click();
  await expect(runButton).toBeDisabled();

  await promptInput.fill("Formuliere den Text als interne Ankuendigung.");
  await expect(runButton).toBeEnabled();
  await runButton.click();

  await expect.poll(() => requestBodies.at(-1)?.text).toBe("Projektstart ist morgen.");
  await expect.poll(() => requestBodies.at(-1)?.language).toBe("auto");
  await expect.poll(() => requestBodies.at(-1)?.prompt).toBe(
    "Formuliere den Text als interne Ankuendigung.",
  );
  await expect(page.getByTestId("quick-action-status")).toContainText("Eigener Auftrag abgeschlossen");
  await expect(mirror).toHaveValue(
    "Custom Rewrite\n\nAuftrag: Formuliere den Text als interne Ankuendigung.\n\nErgebnis:\nProjektstart ist morgen.",
  );
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expect(page.getByTestId("rewrite-diff-after")).toContainText(
    "Auftrag: Formuliere den Text als interne Ankuendigung.",
  );
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("Projektstart ist morgen.");
});

test("incomplete sentences keep word mode without sentence action", async ({ page }) => {
  await page.goto("/");

  const editor = page.getByTestId("editor-input");

  await editor.click();
  await page.keyboard.type("Alpha schnell");
  await selectPreviousCharacters(page, 7);

  await expect(page.getByTestId("rewrite-bubble")).toBeVisible();
  await expect(page.getByTestId("rewrite-primary-action")).toHaveText("Synonyme");
  await expect(page.getByTestId("rewrite-secondary-action")).toBeHidden();
});

test("language selection is sent with correction requests", async ({ page }) => {
  const requestBodies: CorrectionRequestPayload[] = [];

  await page.route("**/api/text-correction", async (route) => {
    const payload = route.request().postDataJSON() as CorrectionRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      json: createCorrectionResponse(payload.text),
    });
  });

  await page.goto("/");
  await openInspectorTab(page, "correction");

  await page.getByTestId("correction-language").selectOption("de-CH");
  await page.getByTestId("editor-input").click();
  await page.keyboard.type("Hallo teh.");

  await expect.poll(() => requestBodies.at(-1)?.language).toBe("de-CH");
  await expect(page.getByTestId("correction-status")).toContainText("1 Problem");
});

test("language selector offers the planned locale set with umlaut labels", async ({ page }) => {
  await page.goto("/");
  await openInspectorTab(page, "correction");

  const options = await page.getByTestId("correction-language").locator("option").allTextContents();

  expect(options).toEqual([
    "Automatisch",
    "Deutsch (Schweiz)",
    "Französisch",
    "Italienisch",
    "Englisch (USA)",
    "Englisch (UK)",
  ]);
});

test("language selection is sent with quick action requests", async ({ page }) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/plain-language", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: createQuickActionBody("Plain result."),
    });
  });

  await page.goto("/");

  await openInspectorTab(page, "correction");
  await page.getByTestId("correction-language").selectOption("en-GB");
  await openInspectorTab(page, "actions");
  await page.getByTestId("editor-input").click();
  await page.keyboard.type("This is a test.");
  await runQuickAction(page, "quick-action-plain-language");

  await expect.poll(() => requestBodies.at(-1)?.language).toBe("en-GB");
  await expect(page.getByTestId("quick-action-status")).toContainText("abgeschlossen");
});

test("local dictionary hides and restores known word matches", async ({ page }) => {
  await page.goto("/");
  await openInspectorTab(page, "correction");

  const editor = page.getByTestId("editor-input");
  const problemItems = page.getByTestId("correction-problem-item");
  const correctionStatus = page.getByTestId("correction-status");
  const dictionaryInput = page.getByTestId("dictionary-input");
  const dictionaryItems = page.getByTestId("dictionary-word-item");

  await editor.click();
  await page.keyboard.type("This is teh text.");

  await expect(problemItems).toHaveCount(1);

  await dictionaryInput.fill("teh");
  await page.getByTestId("dictionary-submit").click();

  await expect(dictionaryItems).toHaveCount(1);
  await expect(problemItems).toHaveCount(0);
  await expect(correctionStatus).toHaveText("Keine Probleme gefunden.");

  await page.getByTestId("dictionary-word-remove").click();

  await expect(problemItems).toHaveCount(1);
  await expect(correctionStatus).toContainText("1 Problem");
});

test("only changed segments are rechecked after the initial correction run", async ({ page }) => {
  const requestBodies: CorrectionRequestPayload[] = [];

  await page.route("**/api/text-correction", async (route) => {
    const payload = route.request().postDataJSON() as CorrectionRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      json: createCorrectionResponse(payload.text),
    });
  });

  await page.goto("/");
  await openInspectorTab(page, "correction");

  const editor = page.getByTestId("editor-input");
  const problemItems = page.getByTestId("correction-problem-item");

  await editor.click();
  await page.keyboard.type("Alpha teh.");
  await expect(problemItems).toHaveCount(1);

  await page.keyboard.press("Enter");
  await page.keyboard.type("Beta recieve");

  await expect.poll(() => requestBodies.some((payload) => payload.text === "Beta recieve")).toBe(
    true,
  );
  await expect(problemItems).toHaveCount(2);

  requestBodies.splice(0, requestBodies.length);

  await page.keyboard.type(".");

  await expect.poll(() => requestBodies.map((payload) => payload.text)).toEqual([
    "Beta recieve.",
  ]);
  await expect(problemItems).toHaveCount(2);
});
