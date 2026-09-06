import { expect, test, type Page } from "@playwright/test";

interface CorrectionRequestPayload {
  text: string;
  language: string;
}

interface QuickActionRequestPayload {
  text: string;
  language: string;
  option?: string;
}

function correctionResponse(text: string) {
  const blocks = [];
  const tehOffset = text.indexOf("teh");

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

  return { original: text, blocks };
}

async function stubCorrection(page: Page): Promise<void> {
  await page.route("**/api/text-correction", async (route) => {
    const payload = route.request().postDataJSON() as CorrectionRequestPayload;
    await route.fulfill({ json: correctionResponse(payload.text) });
  });
}

async function enterText(page: Page, text: string): Promise<void> {
  await page.getByTestId("editor-input").fill(text);
  await expect(page.getByTestId("editor-mirror")).toHaveValue(text);
}

async function selectTextLanguage(page: Page, language: string): Promise<void> {
  await page.getByTestId("workspace-mode-validate").click();
  await page.getByTestId("workspace-language").selectOption(language);
  await page.getByTestId("workspace-mode-transform").click();
}

test("local mode status lives beside the brand and supports hover, click, and keyboard access", async ({
  page,
}) => {
  await stubCorrection(page);
  await page.goto("/");

  const trigger = page.getByTestId("local-mode-trigger");
  const popover = page.getByTestId("local-mode-popover");

  await expect(trigger).toBeVisible();
  await expect(trigger).toHaveAttribute("aria-expanded", "false");
  await expect(trigger).toHaveAttribute("aria-label", "Hinweis zum lokalen Modus anzeigen");
  await expect(popover).toBeHidden();
  await expect(trigger).toHaveCSS("color", "rgb(102, 77, 3)");

  await trigger.hover();
  await expect(popover).toBeVisible();
  await expect(popover).toHaveCSS("background-color", "rgb(255, 243, 205)");
  await expect(popover).toHaveCSS("border-left-color", "rgb(255, 193, 7)");

  await page.mouse.move(900, 500);
  await expect(popover).toBeHidden();

  await trigger.click();
  await expect(popover).toBeVisible();
  await expect(trigger).toHaveAttribute("aria-expanded", "true");
  await expect(trigger).toHaveAttribute("aria-label", "Hinweis zum lokalen Modus schliessen");
  await page.mouse.move(900, 500);
  await expect(popover).toBeVisible();

  await trigger.click();
  await expect(popover).toBeHidden();

  await page.getByTestId("editor-input").focus();
  await trigger.focus();
  await expect(popover).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(popover).toBeHidden();
  await expect(trigger).toBeFocused();

  await trigger.click();
  await expect(popover).toBeVisible();
  await page.mouse.click(900, 500);
  await expect(popover).toBeHidden();
});

test.describe("mobile local mode status", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("opens by tap without relying on hover", async ({ page }) => {
    await stubCorrection(page);
    await page.goto("/");

    const trigger = page.getByTestId("local-mode-trigger");
    const popover = page.getByTestId("local-mode-popover");

    await expect(popover).toBeHidden();
    await trigger.click();
    await expect(popover).toBeVisible();
    await expect(trigger).toHaveAttribute("aria-expanded", "true");
  });
});

test("workspace mode tabs use icons and the intended visual states", async ({ page }) => {
  await stubCorrection(page);
  await page.goto("/");

  const transformTab = page.getByTestId("workspace-mode-transform");
  const validateTab = page.getByTestId("workspace-mode-validate");

  await expect(transformTab.locator("svg.workspace-mode-tab-icon")).toHaveCount(1);
  await expect(validateTab.locator("svg.workspace-mode-tab-icon")).toHaveCount(1);
  await expect(transformTab.locator("svg")).toHaveAttribute("aria-hidden", "true");
  await expect(transformTab.locator("svg")).toHaveAttribute("focusable", "false");
  await expect(validateTab.locator("svg")).toHaveAttribute("aria-hidden", "true");
  await expect(validateTab.locator("svg")).toHaveAttribute("focusable", "false");

  await expect(transformTab).toHaveCSS("border-style", "none");
  await expect(transformTab).toHaveCSS("font-weight", "400");
  await expect(transformTab).toHaveCSS("background-color", "rgb(236, 245, 252)");
  await expect(transformTab).toHaveCSS("color", "rgb(39, 51, 61)");
  await expect(transformTab.locator("svg")).toHaveCSS("color", "rgb(66, 153, 225)");

  await validateTab.click();
  await expect(transformTab).toHaveAttribute("aria-selected", "false");
  await expect(validateTab).toHaveAttribute("aria-selected", "true");
  await expect(transformTab).toHaveCSS("border-style", "none");
  await expect(transformTab).toHaveCSS("background-color", "rgb(237, 242, 245)");
  await expect(transformTab).toHaveCSS("color", "rgb(94, 109, 121)");
  await expect(validateTab).toHaveCSS("border-style", "none");
  await expect(validateTab).toHaveCSS("font-weight", "400");
  await expect(validateTab).toHaveCSS("background-color", "rgb(236, 245, 252)");
  await expect(validateTab).toHaveCSS("color", "rgb(39, 51, 61)");
  await expect(validateTab.locator("svg")).toHaveCSS("color", "rgb(66, 153, 225)");

  await transformTab.hover();
  await expect(transformTab).toHaveCSS("background-color", "rgb(232, 241, 247)");
  await expect(transformTab).toHaveCSS("border-style", "none");
});

test("starts with a wide editor and only the three MVP tools", async ({ page }) => {
  await stubCorrection(page);
  await page.goto("/");

  await expect(page.getByTestId("workspace-mode-transform")).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await expect(page.getByTestId("editor-shell")).toBeVisible();
  await expect(page.getByTestId("correction-rail")).toBeHidden();
  await expect(page.getByTestId("mvp-summary-option")).toBeVisible();
  await expect(page.getByTestId("mvp-action-summarize")).toHaveCount(0);
  await expect(page.getByTestId("mvp-summary-option")).toHaveValue("");
  await expect(page.getByTestId("mvp-summary-option")).toBeEnabled();
  await expect(page.getByText("STRUKTUR", { exact: true })).toHaveCount(0);
  await expect(page.getByText("STIL", { exact: true })).toHaveCount(0);
  await expect(page.getByTestId("mvp-action-plain-language")).toBeVisible();
  const editorInput = page.getByTestId("editor-input");
  await expect(editorInput.locator("p")).toHaveCount(3);
  await expect(editorInput).toContainText(
    "Um die steuerliche Wettbewerbsfähigkeit des Kantons zu verbessern",
  );
  await expect(editorInput).toContainText("Vor diesem Hintergrund sieht die Steuerstrategie");
  await expect(editorInput).toContainText("Die zur Umsetzung empfohlenen Massnahmen");
  await expect(page.getByTestId("editor-character-count")).not.toHaveText("0");
  await expect(page.getByTestId("editor-word-count")).not.toHaveText("0");
  const transformRibbon = page.locator("[data-workspace-ribbon='transform']");
  const ribbonTitles = transformRibbon.locator(".ribbon-group-title");
  await expect(ribbonTitles).toHaveText(["Umstrukturieren", "Verbessern"]);
  await expect(ribbonTitles.first()).toHaveCSS("font-size", "14px");
  await expect(ribbonTitles.first()).toHaveCSS("font-weight", "400");
  await expect(ribbonTitles.first()).toHaveCSS("text-transform", "none");
  await expect(ribbonTitles.first()).toHaveCSS("color", "rgb(63, 75, 85)");
  const firstRibbonGroupPosition = await transformRibbon
    .locator(".ribbon-group-with-title")
    .first()
    .evaluate((group) => {
      const actions = group.querySelector(".ribbon-action-row")?.getBoundingClientRect();
      const title = group.querySelector(".ribbon-group-title")?.getBoundingClientRect();
      return { actionBottom: actions?.bottom ?? 0, titleTop: title?.top ?? 0 };
    });
  expect(firstRibbonGroupPosition.titleTop).toBeGreaterThan(firstRibbonGroupPosition.actionBottom);
  await expect(page.getByTestId("quick-action-proofread")).toHaveCount(0);
  await expect(page.getByTestId("quick-action-bullet-points")).toHaveCount(0);
  await expect(page.getByTestId("quick-action-formality")).toHaveCount(0);
  await expect(page.getByTestId("advisor-toggle")).toBeHidden();
  await expect(page.getByTestId("advisor-panel")).toBeHidden();
  await expect(page.getByTestId("rewrite-bubble")).toHaveCount(0);

  const widths = await page.evaluate(() => ({
    viewport: document.documentElement.clientWidth,
    editor: document.querySelector("[data-editor-shell]")?.getBoundingClientRect().width ?? 0,
  }));
  expect(widths.editor).toBeGreaterThan(widths.viewport * 0.75);
});

test("temporary start text can be used by a quick action", async ({ page }) => {
  await stubCorrection(page);
  let requestText = "";
  await page.route("**/api/quick-actions/plain-language", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;
    requestText = payload.text;
    await route.fulfill({ json: { text: `${payload.text} Zusatz.` } });
  });
  await page.goto("/");

  await page.getByTestId("mvp-action-plain-language").click();
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  expect(requestText).toContain(
    "Um die steuerliche Wettbewerbsfähigkeit des Kantons zu verbessern",
  );
});

test("typing updates counters and undo/redo remains one action", async ({ page }) => {
  await stubCorrection(page);
  await page.goto("/");
  await enterText(page, "Hallo Welt");

  await expect(page.getByTestId("editor-character-count")).toHaveText("10");
  await expect(page.getByTestId("editor-word-count")).toHaveText("2");
  await page.getByTestId("editor-undo").click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue(
    /Um die steuerliche Wettbewerbsfähigkeit des Kantons zu verbessern/,
  );
  await page.getByTestId("editor-redo").click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Hallo Welt");
});

test("correction stays compact in transform mode and rail opens only in validate mode", async ({
  page,
}) => {
  await stubCorrection(page);
  await page.goto("/");
  await enterText(page, "Das ist teh Text.");

  await expect(page.getByTestId("correction-mode-badge")).toHaveText("1");
  await expect(page.getByTestId("correction-rail")).toBeHidden();

  await page.getByTestId("workspace-mode-validate").click();
  await expect(page.getByTestId("correction-rail")).toBeVisible();
  await expect(page.getByTestId("correction-results-toggle")).toHaveAttribute(
    "aria-expanded",
    "true",
  );
  await expect(page.getByTestId("correction-retry")).toBeHidden();

  await page.getByRole("button", { name: "Korrekturergebnisse schliessen" }).click();
  await expect(page.getByTestId("correction-rail")).toBeHidden();
  await page.getByTestId("workspace-mode-transform").click();
  await page.getByTestId("workspace-mode-validate").click();
  await expect(page.getByTestId("correction-rail")).toBeVisible();
  await page.getByTestId("correction-results-toggle").click();
  await expect(page.getByTestId("correction-rail")).toBeHidden();
  await page.getByTestId("correction-results-toggle").click();
  await expect(page.getByTestId("correction-rail")).toBeVisible();

  await page.getByRole("button", { name: "the", exact: true }).click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Das ist the Text.");
  await expect(page.getByTestId("correction-rail")).toBeHidden();
  await expect(page.getByTestId("correction-results-toggle")).toBeDisabled();
});

test("clicking a correction mark switches mode and focuses its finding", async ({ page }) => {
  await stubCorrection(page);
  await page.goto("/");
  await enterText(page, "Das ist teh Text.");
  await expect(page.locator("[data-correction-index='0']")).toBeVisible();

  await page.locator("[data-correction-index='0']").click();
  await expect(page.getByTestId("workspace-mode-validate")).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await expect(page.getByRole("button", { name: /Problem 1: teh/ })).toBeFocused();
});

test("advisor validates bundled guidelines, reviews selected fixes and preserves findings on rejection", async ({
  page,
}) => {
  await stubCorrection(page);
  const documents = [
    {
      name: "empfehlungen-anglizismen-maerz-2020",
      title: "Empfehlungen zu Anglizismen",
      summary: "Demo für verständliche deutsche Alternativen.",
      source: "Textbuddy Referenzblatt (projektintern)",
      documentUrl: "/api/advisor/doc/empfehlungen-anglizismen-maerz-2020",
      ruleCount: 2,
    },
    {
      name: "schreibweisungen",
      title: "Schreibweisungen",
      summary: "Demo für interne Schreibstandards.",
      source: "Textbuddy Referenzblatt (projektintern)",
      documentUrl: "/api/advisor/doc/schreibweisungen",
      ruleCount: 2,
    },
  ];
  let validateBody: unknown;
  let fixBody: { text: string; findings: Array<{ ruleId: string }> } | undefined;
  await page.route("**/api/advisor/docs", async (route) => route.fulfill({ json: documents }));
  await page.route("**/api/advisor/validate", async (route) => {
    validateBody = route.request().postDataJSON();
    const validation = (stableKey: string, documentName: string, documentTitle: string,
      ruleId: string, ruleTitle: string, matchedText: string, suggestion: string,
      start: number, end: number) => `event:validation\ndata:${JSON.stringify({
        stableKey, documentName, documentTitle, ruleId, ruleTitle, page: 1, pageLabel: "Seite 1",
        message: "Diese Formulierung entspricht nicht der Demo-Regel.", matchedText,
        excerpt: `…${matchedText}…`, suggestion,
        referenceUrl: `/api/advisor/doc/${documentName}#page=1`, start, end,
      })}\n\n`;
    await route.fulfill({
      contentType: "text/event-stream",
      body: validation("ang::download::6:16", documents[0]!.name, documents[0]!.title,
        "downloaden-statt-herunterladen", "Deutsche Alternative", "downloaden", "herunterladen", 6, 16)
        + 'event:progress\ndata:{"checked":2,"total":4}\n\n'
        + validation("schreib::sofort::21:31", documents[1]!.name, documents[1]!.title,
          "per-sofort-vermeiden", "Per sofort ersetzen", "per sofort", "ab sofort", 21, 31)
        + 'event:progress\ndata:{"checked":4,"total":4}\n\n',
    });
  });
  await page.route("**/api/advisor/fix", async (route) => {
    fixBody = route.request().postDataJSON() as typeof fixBody;
    await route.fulfill({ json: { text: "Bitte herunterladen Sie per sofort." } });
  });
  await page.goto("/");
  await enterText(page, "Bitte downloaden Sie per sofort.");

  await expect(page.getByTestId("advisor-toggle")).toBeHidden();
  await page.getByTestId("workspace-mode-validate").click();
  await page.getByTestId("advisor-toggle").click();
  await expect(page.getByTestId("advisor-panel")).toBeVisible();
  await expect(page.getByTestId("advisor-document")).toHaveCount(2);
  await expect(page.getByTestId("advisor-document").first()).toContainText("2 Regeln");
  await expect(page.getByTestId("advisor-document").first().getByRole("link", { name: "PDF öffnen" }))
    .toHaveAttribute("target", "_blank");
  await page.getByTestId("advisor-document-checkbox").first().check();
  await page.getByTestId("advisor-document-checkbox").nth(1).check();
  await page.getByTestId("advisor-start").click();

  await expect(page.getByTestId("advisor-status")).toContainText("Prüfung abgeschlossen: 2 Befund(e)");
  await expect(page.getByTestId("advisor-finding")).toHaveCount(2);
  await expect(page.getByTestId("advisor-finding-excerpt").first()).toHaveText("…downloaden…");
  expect(validateBody).toEqual({
    text: "Bitte downloaden Sie per sofort.",
    docs: [documents[0]!.name, documents[1]!.name],
  });
  await page.getByTestId("advisor-finding-focus").first().click();
  await expect(page.locator("[data-testid='correction-mark']")).toHaveText("downloaden");
  await page.getByTestId("advisor-decision").nth(1).click();
  await expect(page.getByTestId("advisor-decision").nth(1)).toHaveText("Überspringen");
  await page.getByTestId("advisor-fix").click();

  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  expect(fixBody?.findings).toHaveLength(1);
  expect(fixBody?.findings[0]?.ruleId).toBe("downloaden-statt-herunterladen");
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Bitte downloaden Sie per sofort.");
  await page.getByRole("button", { name: "Alle ablehnen" }).click();
  await expect(page.getByTestId("advisor-panel")).toBeVisible();
  await expect(page.getByTestId("advisor-finding")).toHaveCount(2);
  await expect(page.getByTestId("advisor-status")).toContainText("Prüfung abgeschlossen: 2 Befund(e)");

  await page.getByTestId("advisor-fix").click();
  await page.getByRole("button", { name: "Alle annehmen" }).click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Bitte herunterladen Sie per sofort.");
  await expect(page.getByTestId("advisor-finding")).toHaveCount(0);
  await page.getByTestId("editor-undo").click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Bitte downloaden Sie per sofort.");
});

test("correction sends one request for a 50000-character document", async ({ page }) => {
  const payloads: CorrectionRequestPayload[] = [];
  await page.route("**/api/text-correction", async (route) => {
    const payload = route.request().postDataJSON() as CorrectionRequestPayload;
    payloads.push(payload);
    await route.fulfill({ json: correctionResponse(payload.text) });
  });
  await page.goto("/");

  const text = `${"a".repeat(49_999)}.`;
  await enterText(page, text);
  await expect.poll(() => payloads.length).toBe(1);
  await page.waitForTimeout(450);

  expect(payloads).toHaveLength(1);
  expect(payloads[0]?.text).toBe(text);
});

test("correction aborts an older request and ignores its late response", async ({ page }) => {
  let calls = 0;
  let releaseFirst: (() => void) | undefined;
  await page.route("**/api/text-correction", async (route) => {
    calls += 1;
    const payload = route.request().postDataJSON() as CorrectionRequestPayload;
    if (calls === 1) {
      await new Promise<void>((resolve) => {
        releaseFirst = resolve;
      });
      try {
        await route.fulfill({ json: correctionResponse(payload.text) });
      } catch {
        // The browser already cancelled the obsolete request.
      }
      return;
    }
    await route.fulfill({ json: correctionResponse(payload.text) });
  });
  await page.goto("/");

  await enterText(page, "Das ist teh alt.");
  await expect.poll(() => calls).toBe(1);
  await enterText(page, "Das ist sauber neu.");
  await expect.poll(() => calls).toBe(2);
  await expect(page.getByTestId("correction-results-toggle")).toBeDisabled();

  releaseFirst?.();
  await expect(page.getByTestId("correction-results-toggle")).toBeDisabled();
  await expect(page.locator("[data-correction-index]")).toHaveCount(0);
});

test("plain-language review preserves the original and commits as one undoable transaction", async ({
  page,
}) => {
  await stubCorrection(page);
  let release: (() => void) | undefined;
  await page.route("**/api/quick-actions/plain-language", async (route) => {
    await new Promise<void>((resolve) => {
      release = resolve;
    });
    await route.fulfill({ json: { text: "Das neue Haus ist gross." } });
  });
  await page.goto("/");
  await enterText(page, "Das alte Haus ist klein.");
  await selectTextLanguage(page, "de-CH");

  await page.getByTestId("mvp-action-plain-language").click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Das alte Haus ist klein.");
  await expect(page.getByTestId("workspace-mode-validate")).toBeDisabled();
  release?.();

  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expect(page.getByTestId("review-readability")).toBeVisible();
  await expect(page.getByTestId("review-readability")).toContainText(
    "Flesch-Lesbarkeit (Deutsch)",
  );
  await expect(page.getByTestId("review-readability-before")).toHaveText("104.8");
  await expect(page.getByTestId("review-readability-after")).toHaveText("116.5");
  await expect(page.getByTestId("review-readability-difference")).toHaveText("(+11.7)");
  await expect(page.getByTestId("editor-input")).toBeHidden();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Das alte Haus ist klein.");
  await expect(page.getByRole("button", { name: "Abbrechen" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Inline" })).toHaveAttribute(
    "title",
    "Inline",
  );
  await expect(page.getByRole("button", { name: "Inline" }).locator("svg")).toHaveCount(1);
  await expect(page.getByRole("button", { name: "Zwei Spalten" }).locator("svg")).toHaveCount(1);
  await expect(page.getByRole("button", { name: "Erneut ausführen" }).locator("svg")).toHaveCount(1);
  await expect(page.locator("[data-review-inline] .diff-removed").first()).toHaveCSS(
    "background-color",
    "rgb(232, 237, 240)",
  );
  await expect(page.locator("[data-review-inline] .diff-removed").first()).toHaveCSS(
    "color",
    "rgb(94, 109, 121)",
  );
  await expect(page.locator("[data-review-inline] .diff-removed").first()).toHaveCSS(
    "text-decoration-line",
    "line-through",
  );
  await expect(
    page.locator("[data-review-inline] [data-diff-decision='accepted']"),
  ).toHaveCount(2);
  await expect(
    page.locator("[data-review-inline] [data-diff-decision='accepted']").first(),
  ).toHaveCSS("background-color", "rgb(25, 135, 84)");
  await expect(
    page.locator("[data-review-inline] [data-diff-decision='accepted']").first(),
  ).toHaveCSS("border-top-width", "0px");
  await expect(
    page.locator("[data-review-inline] [data-diff-decision='rejected']").first(),
  ).toHaveCSS("background-color", "rgb(237, 242, 245)");

  await page.locator("[data-review-inline] [data-diff-decision='accepted']").first().click();
  await expect(page.getByTestId("review-readability-before")).toHaveText("104.8");
  await expect(page.getByTestId("review-readability-after")).toHaveText("116.5");
  await expect(page.getByTestId("review-readability-difference")).toHaveText("(+11.7)");
  await page.getByRole("button", { name: "Zwei Spalten" }).click();
  await expect(page.getByTestId("review-readability-difference")).toHaveText("(+11.7)");
  await expect(page.locator("[data-review-split-before] .diff-rejected").first()).toHaveCSS(
    "background-color",
    "rgb(232, 237, 240)",
  );
  await page.getByRole("button", { name: "Inline" }).click();
  await page.locator("[data-review-inline] [data-diff-decision='rejected']").first().click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Das neue Haus ist klein.");
  await page.getByTestId("editor-undo").click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Das alte Haus ist klein.");
});

test("summary keeps option values, supports split review, reject-all and retry", async ({ page }) => {
  await stubCorrection(page);
  const payloads: QuickActionRequestPayload[] = [];
  await page.route("**/api/quick-actions/summarize", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;
    payloads.push(payload);
    await route.fulfill({ json: { text: `Kurz: ${payload.text}` } });
  });
  await page.goto("/");
  await enterText(page, "Ein langer Ausgangstext.");
  await selectTextLanguage(page, "de-CH");
  await page.getByTestId("mvp-summary-option").selectOption("management_summary");

  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expect(page.getByTestId("review-readability")).toBeHidden();
  expect(payloads[0]?.option).toBe("management_summary");
  await expect(page.getByTestId("mvp-summary-option")).toHaveValue("");
  await expect(page.getByRole("button", { name: "Alle ablehnen" })).toHaveCSS(
    "background-color",
    "rgb(237, 242, 245)",
  );
  await expect(page.getByRole("button", { name: "Alle ablehnen" }).locator("svg")).toHaveCount(1);
  await expect(page.getByRole("button", { name: "Alle annehmen" })).toHaveCSS(
    "background-color",
    "rgb(209, 231, 221)",
  );
  await expect(page.getByRole("button", { name: "Alle annehmen" })).toHaveCSS(
    "color",
    "rgb(10, 54, 34)",
  );
  await expect(page.getByRole("button", { name: "Alle annehmen" }).locator("svg")).toHaveCount(1);
  await page.getByRole("button", { name: "Zwei Spalten" }).click();
  await expect(page.getByTestId("review-split")).toBeVisible();
  await page.getByRole("button", { name: "Erneut ausführen" }).click();
  await expect.poll(() => payloads.length).toBe(2);
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await page.getByRole("button", { name: "Alle ablehnen" }).click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Ein langer Ausgangstext.");
});

test("unchanged and failed transformations never alter the editor", async ({ page }) => {
  await stubCorrection(page);
  let calls = 0;
  await page.route("**/api/quick-actions/plain-language", async (route) => {
    calls += 1;
    if (calls === 1) {
      await route.fulfill({ json: { text: "Unverändert." } });
    } else {
      await route.fulfill({ status: 500, json: { message: "Transformation fehlgeschlagen" } });
    }
  });
  await page.goto("/");
  await enterText(page, "Unverändert.");
  await selectTextLanguage(page, "de-CH");

  await page.getByTestId("mvp-action-plain-language").click();
  await expect(page.getByText("Keine Änderungen gefunden")).toBeVisible();
  await expect(page.getByTestId("review-readability-before")).toHaveText("-55.0");
  await expect(page.getByTestId("review-readability-after")).toHaveText("-55.0");
  await expect(page.getByTestId("review-readability-difference")).toHaveText("(±0.0)");
  await page.getByRole("button", { name: "Alle ablehnen" }).click();
  await page.getByTestId("mvp-action-plain-language").click();
  await expect(page.getByTestId("workspace-status")).toContainText(
    "Transformation fehlgeschlagen",
  );
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Unverändert.");
});

test("plain-language retry recalculates readability from the new result", async ({ page }) => {
  await stubCorrection(page);
  let calls = 0;
  await page.route("**/api/quick-actions/plain-language", async (route) => {
    calls += 1;
    await route.fulfill({
      json: { text: calls === 1 ? "Das neue Haus ist gross." : "Das Haus ist gross." },
    });
  });
  await page.goto("/");
  await enterText(page, "Das alte Haus ist klein.");
  await selectTextLanguage(page, "de-CH");

  await page.getByTestId("mvp-action-plain-language").click();
  await expect(page.getByTestId("review-readability-after")).toHaveText("116.5");
  await page.getByRole("button", { name: "Erneut ausführen" }).click();

  await expect.poll(() => calls).toBe(2);
  await expect(page.getByTestId("review-readability-before")).toHaveText("104.8");
  await expect(page.getByTestId("review-readability-after")).toHaveText("117.5");
  await expect(page.getByTestId("review-readability-difference")).toHaveText("(+12.7)");
});

test("plain-language readability stays hidden for automatic and non-German languages", async ({
  page,
}) => {
  await stubCorrection(page);
  await page.route("**/api/quick-actions/plain-language", async (route) => {
    await route.fulfill({ json: { text: "Das neue Haus ist gross." } });
  });
  await page.goto("/");
  await enterText(page, "Das alte Haus ist klein.");

  await page.getByTestId("mvp-action-plain-language").click();
  await expect(page.getByTestId("review-readability")).toBeHidden();
  await page.getByRole("button", { name: "Alle ablehnen" }).click();

  await selectTextLanguage(page, "fr");
  await page.getByTestId("mvp-action-plain-language").click();
  await expect(page.getByTestId("review-readability")).toBeHidden();
});

test("toolbar copies, downloads DOCX and opens the live statistics popover", async ({
  page,
  context,
}) => {
  await context.grantPermissions(["clipboard-read", "clipboard-write"]);
  await stubCorrection(page);
  await page.goto("/");
  await enterText(page, "Mal Tal. Ball Fall.");

  await page.getByTestId("workspace-mode-validate").click();
  await page.getByTestId("editor-copy").click();
  await expect.poll(() => page.evaluate(() => navigator.clipboard.readText())).toBe(
    "Mal Tal. Ball Fall.",
  );
  const fleschSection = page.locator("[data-text-stats-flesch]");
  await page.getByTestId("editor-stats-toggle").click();
  await expect(page.getByTestId("stats-popover")).toBeVisible();
  await expect(fleschSection).toHaveAttribute("hidden", "");
  await page.getByTestId("workspace-language").selectOption("de-CH");
  await expect(fleschSection).not.toHaveAttribute("hidden", "");
  await expect(page.getByTestId("text-stats-flesch")).toBeVisible();
  await expect(page.getByTestId("text-stats-flesch")).toHaveText("119.5");
  await expect(page.getByTestId("text-stats-flesch-label")).toHaveText("Sehr leicht lesbar");
  await page.getByTestId("workspace-language").selectOption("fr");
  await expect(fleschSection).toHaveAttribute("hidden", "");
  await page.keyboard.press("Escape");
  await expect(page.getByTestId("stats-popover")).toBeHidden();

  const downloadPromise = page.waitForEvent("download");
  await page.getByTestId("editor-download").click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toMatch(/^textbuddy-\d{4}-\d{2}-\d{2}\.docx$/);
});

test("upload uses the shared text language for OCR and imports into the full editor", async ({
  page,
}) => {
  await stubCorrection(page);
  let requestUrl = "";
  await page.route("**/api/convert/doc**", async (route) => {
    requestUrl = route.request().url();
    await route.fulfill({
      json: {
        html: "<h2># Import <em>Titel</em></h2><script>nicht übernehmen</script><p>* Importierter Text.</p>",
      },
    });
  });
  await page.goto("/");
  await page.getByTestId("workspace-mode-validate").click();
  await page.getByTestId("workspace-language").selectOption("fr");
  await page.getByTestId("workspace-mode-transform").click();

  await page.getByTestId("document-import-input").setInputFiles({
    name: "import.docx",
    mimeType: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    buffer: Buffer.from("dummy"),
  });

  await expect(page.getByTestId("editor-mirror")).toHaveValue(
    "# Import Titel\n* Importierter Text.",
  );
  expect(requestUrl).toContain("ocrLanguage=fr");
});

test("the full editor accepts document drag and drop", async ({ page }) => {
  await stubCorrection(page);
  await page.route("**/api/convert/doc**", async (route) => {
    await route.fulfill({ json: { html: "<p>Per Drop importiert.</p>" } });
  });
  await page.goto("/");

  const dataTransfer = await page.evaluateHandle(() => {
    const transfer = new DataTransfer();
    transfer.items.add(new File(["Drop-Inhalt"], "drop.txt", { type: "text/plain" }));
    return transfer;
  });
  await page
    .locator("[data-editor-import-drop-target]")
    .dispatchEvent("drop", { dataTransfer });

  await expect(page.getByTestId("editor-mirror")).toHaveValue("Per Drop importiert.");
});

test.describe("mobile workspace", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("keeps transform ribbon titles centered", async ({ page }) => {
    await stubCorrection(page);
    await page.goto("/");

    const transformRibbon = page.locator("[data-workspace-ribbon='transform']");
    const firstGroup = transformRibbon.locator(".ribbon-group-with-title").first();
    const title = firstGroup.locator(".ribbon-group-title");
    const groupBox = await firstGroup.boundingBox();
    const titleBox = await title.boundingBox();

    expect(groupBox).not.toBeNull();
    expect(titleBox).not.toBeNull();
    const titleCenter = (titleBox?.x ?? 0) + (titleBox?.width ?? 0) / 2;
    const groupCenter = (groupBox?.x ?? 0) + (groupBox?.width ?? 0) / 2;
    expect(Math.abs(titleCenter - groupCenter)).toBeLessThanOrEqual(1);
  });

  test("uses a focusable correction slideover without horizontal overflow", async ({ page }) => {
    await stubCorrection(page);
    await page.goto("/");
    await enterText(page, "Das ist teh Text.");
    await page.getByTestId("workspace-mode-validate").click();

    await expect(page.getByTestId("correction-rail")).toBeVisible();
    await expect(page.locator("body")).toHaveAttribute("data-correction-slideover-open", "true");
    await page.keyboard.press("Escape");
    await expect(page.getByTestId("correction-rail")).toBeHidden();
    const hasOverflow = await page.evaluate(
      () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
    );
    expect(hasOverflow).toBe(false);
  });
});
