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
  await expect(transformTab).toHaveCSS("background-color", "rgb(232, 241, 247)");
  await expect(transformTab).toHaveCSS(
    "box-shadow",
    /rgb\(53, 122, 180\).*inset/,
  );
  await expect(transformTab).toHaveCSS("color", "rgb(39, 51, 61)");
  await expect(transformTab.locator("svg")).toHaveCSS("color", "rgb(53, 122, 180)");

  await validateTab.click();
  await expect(transformTab).toHaveAttribute("aria-selected", "false");
  await expect(validateTab).toHaveAttribute("aria-selected", "true");
  await expect(transformTab).toHaveCSS("border-style", "none");
  await expect(transformTab).toHaveCSS("background-color", "rgb(237, 242, 245)");
  await expect(transformTab).toHaveCSS("box-shadow", "none");
  await expect(transformTab).toHaveCSS("color", "rgb(94, 109, 121)");
  await expect(validateTab).toHaveCSS("border-style", "none");
  await expect(validateTab).toHaveCSS("font-weight", "400");
  await expect(validateTab).toHaveCSS("background-color", "rgb(232, 241, 247)");
  await expect(validateTab).toHaveCSS("color", "rgb(39, 51, 61)");
  await expect(validateTab.locator("svg")).toHaveCSS("color", "rgb(53, 122, 180)");

  await transformTab.hover();
  await expect(transformTab).toHaveCSS("background-color", "rgb(232, 241, 247)");
  await expect(transformTab).toHaveCSS("border-style", "none");
});

test("starts with a wide empty editor and the rewrite controls", async ({ page }) => {
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
  await expect(page.getByTestId("mvp-summary-option")).toBeDisabled();
  await expect(page.getByText("STRUKTUR", { exact: true })).toHaveCount(0);
  await expect(page.getByText("STIL", { exact: true })).toHaveCount(0);
  await expect(page.getByTestId("mvp-action-plain-language")).toBeVisible();
  const editorInput = page.getByTestId("editor-input");
  await expect(editorInput).toHaveText("");
  await expect(page.getByTestId("editor-character-count")).toHaveText("0");
  await expect(page.getByTestId("editor-word-count")).toHaveText("0");
  const transformRibbon = page.locator("[data-workspace-ribbon='transform']");
  const ribbonTitles = transformRibbon.locator(".ribbon-group-title");
  await expect(ribbonTitles).toHaveText(["Umstrukturieren", "Verbessern"]);
  await expect(ribbonTitles.first()).toHaveCSS("font-size", "14px");
  await expect(ribbonTitles.first()).toHaveCSS("font-weight", "400");
  await expect(ribbonTitles.first()).toHaveCSS("text-transform", "none");
  await expect(ribbonTitles.first()).toHaveCSS("color", "rgb(39, 51, 61)");
  const firstRibbonGroupPosition = await transformRibbon
    .locator(".ribbon-group-with-title")
    .first()
    .evaluate((group) => {
      const actions = group.querySelector(".ribbon-action-row")?.getBoundingClientRect();
      const title = group.querySelector(".ribbon-group-title")?.getBoundingClientRect();
      return { actionBottom: actions?.bottom ?? 0, titleTop: title?.top ?? 0 };
    });
  expect(firstRibbonGroupPosition.titleTop).toBeGreaterThan(firstRibbonGroupPosition.actionBottom);
  await expect(page.getByTestId("advisor-toggle")).toBeHidden();
  await expect(page.getByTestId("advisor-panel")).toBeHidden();

  const widths = await page.evaluate(() => ({
    viewport: document.documentElement.clientWidth,
    editor: document.querySelector("[data-editor-shell]")?.getBoundingClientRect().width ?? 0,
  }));
  expect(widths.editor).toBeGreaterThan(widths.viewport * 0.75);
});

test("the packaged app starts with an empty editor and serves the local tools", async ({ page, request }) => {
  await page.goto("/");
  await expect(page.getByTestId("editor-mirror")).toHaveValue("");
  await expect(page.getByTestId("mvp-action-plain-language")).toBeDisabled();

  const health = await request.get("/actuator/health");
  expect(health.ok()).toBeTruthy();
  expect(await health.json()).toMatchObject({ status: "UP" });
  const correction = await request.post("/api/text-correction", {
    data: { text: "This is teh text.", language: "en-US" },
  });
  expect(correction.ok()).toBeTruthy();
  expect((await correction.json()).blocks[0]).toMatchObject({
    offset: 8, length: 3, replacements: expect.arrayContaining(["the"]),
  });
  const rewrite = await request.post("/api/quick-actions/plain-language", {
    data: { text: "Der Fall ist kompliziert.", language: "de-CH" },
  });
  expect(rewrite.ok()).toBeTruthy();
  expect(await rewrite.json()).toEqual({ text: "Kurz und einfach: Der Fall ist einfach." });
  const imported = await request.post("/api/convert/doc", {
    multipart: { file: {
      name: "antrag.txt", mimeType: "text/plain", buffer: Buffer.from("Antrag\n\nDie Gemeinde stimmt zu."),
    } },
  });
  expect(imported.ok()).toBeTruthy();
  expect(await imported.json()).toEqual({ text: "Antrag\n\nDie Gemeinde stimmt zu." });
});

test("typing updates counters and undo/redo remains one action", async ({ page }) => {
  await stubCorrection(page);
  await page.goto("/");
  await enterText(page, "Hallo Welt");

  await expect(page.getByTestId("editor-character-count")).toHaveText("10");
  await expect(page.getByTestId("editor-word-count")).toHaveText("2");
  await page.getByTestId("editor-undo").click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("");
  await page.getByTestId("editor-redo").click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Hallo Welt");
});

test("editor keeps typed markers and formatted clipboard contents as plain text", async ({ page }) => {
  await stubCorrection(page);
  await page.goto("/");
  const input = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");
  const typed = "1. Eintrag\n# Titel\n> Zitat";
  await input.pressSequentially("1. Eintrag");
  await input.press("Enter");
  await input.pressSequentially("# Titel");
  await input.press("Enter");
  await input.pressSequentially("> Zitat");
  await expect(mirror).toHaveValue(typed);
  await expect(input.locator(":scope > p")).toHaveCount(3);

  const pasted = "\n  📄 Absatz\t\u00a0mit Leerraum  \n\n\nLetzte Zeile \n";
  await input.press("ControlOrMeta+a");
  await input.evaluate((node, text) => {
    const clipboardData = new DataTransfer();
    clipboardData.setData("text/plain", text.replace(/\n/g, "\r\n"));
    clipboardData.setData("text/html", "<h1><strong>Absatz</strong></h1><ol><li><em>Letzte Zeile</em></li></ol>");
    node.dispatchEvent(new ClipboardEvent("paste", { clipboardData, bubbles: true, cancelable: true }));
  }, pasted);
  await expect(mirror).toHaveValue(pasted);
  await expect(input.locator(":scope > p")).toHaveCount(6);
  await expect(input.locator(":scope > :not(p), :scope > p > :not(br)")).toHaveCount(0);
  await page.getByTestId("editor-undo").click();
  await expect(mirror).toHaveValue(typed);
  await page.getByTestId("editor-redo").click();
  await expect(mirror).toHaveValue(pasted);
  const copied = await input.evaluate((node) => {
    const { editor } = node as HTMLElement & { editor: { commands: { selectAll: () => void } } };
    editor.commands.selectAll();
    const clipboardData = new DataTransfer();
    node.dispatchEvent(new ClipboardEvent("copy", { clipboardData, bubbles: true, cancelable: true }));
    return clipboardData.getData("text/plain");
  });
  expect(copied).toBe(pasted);
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
  await enterText(page, "Vorwort");
  const editor = page.getByTestId("editor-input");
  await editor.press("End");
  await editor.press("Shift+Enter");
  await editor.pressSequentially("Das ist teh Text.");
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Vorwort\nDas ist teh Text.");
  await expect(page.getByTestId("correction-mark")).toHaveText("teh");
  await expect(page.locator("[data-correction-index='0']")).toBeVisible();

  await page.locator("[data-correction-index='0']").click();
  await expect(page.getByTestId("workspace-mode-validate")).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await expect(page.getByRole("button", { name: /Problem 1: teh/ })).toBeFocused();
});

test("correction marks and applies UTF-16 ranges across and between plaintext paragraphs", async ({ page }) => {
  const original = "📄 Einleitung\n\nlinks\nrechts Schluss.";
  const joined = "📄 Einleitung\n\n Schluss.";
  const offset = original.indexOf("links");
  await page.route("**/api/text-correction", async (route) => {
    const { text } = route.request().postDataJSON() as CorrectionRequestPayload;
    const change = text === original ? { length: "links\nrechts".length, replacements: [""] }
      : text === joined ? { length: 0, replacements: ["Neuer"] } : null;
    await route.fulfill({ json: { original: text, blocks: change ? [{
      offset, ...change, message: "Bitte prüfen.", shortMessage: "Änderung", ruleId: "DEMO_CHANGE",
    }] : [] } });
  });

  await page.goto("/");
  await enterText(page, original);
  await page.getByTestId("workspace-mode-validate").click();
  const marks = page.getByTestId("correction-mark");
  await expect(marks).toHaveText(["links", "rechts"]);
  await page.getByTestId("correction-suggestion").click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue(joined);
  await expect(marks).toHaveClass(/textbuddy-correction-insertion/);
  await expect(page.getByTestId("editor-input").locator("p").last().getByTestId("correction-mark")).toHaveCount(1);
  await page.getByTestId("correction-suggestion").click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("📄 Einleitung\n\nNeuer Schluss.");
});

test("advisor uses the bundled catalog and reviews changes without another backend call", async ({ page }) => {
  await stubCorrection(page);
  const requests: string[] = [];
  page.on("request", (request) => {
    const path = new URL(request.url()).pathname;
    // The independent LanguageTool check runs after ordinary editor changes, including Undo.
    if (path.startsWith("/api/") && path !== "/api/text-correction") requests.push(`${request.method()} ${path}`);
  });

  const original = "Bitte downloaden Sie per sofort.";
  const mirror = page.getByTestId("editor-mirror");
  const findings = page.getByTestId("advisor-finding");
  const start = page.getByTestId("advisor-start");
  const status = page.getByTestId("advisor-status");
  const preview = page.getByTestId("advisor-preview");

  await page.goto("/");
  await enterText(page, original);
  await page.getByTestId("workspace-mode-validate").click();
  await page.getByTestId("advisor-toggle").click();
  const documents = page.getByTestId("advisor-document");
  await expect(documents).toHaveCount(5);
  await expect(documents.first()).toContainText("2 Regeln");
  await expect(documents.first().getByRole("link", { name: "PDF öffnen" })).toHaveAttribute("target", "_blank");
  const anglicisms = page.locator("[data-testid=advisor-document-checkbox][value=empfehlungen-anglizismen-maerz-2020]");
  const conventions = page.locator("[data-testid=advisor-document-checkbox][value=schreibweisungen]");
  await anglicisms.check();
  await conventions.check();
  await start.click();

  await expect(status).toHaveText("Prüfung abgeschlossen: 2 Befund(e).");
  await expect(findings).toHaveCount(2);
  await expect(findings.first().getByRole("link", { name: "PDF öffnen" }))
    .toHaveAttribute("href", /\/api\/advisor\/doc\/empfehlungen-anglizismen-maerz-2020#page=\d+/u);
  await page.getByTestId("advisor-finding-focus").first().click();
  await expect(page.getByTestId("correction-mark")).toHaveText("downloaden");
  await page.getByTestId("advisor-decision").nth(1).click();
  await preview.click();

  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expect(page.locator("[data-review-inline] [data-diff-decision='accepted']")).toHaveCount(1);
  await expect(mirror).toHaveValue(original);
  await page.getByRole("button", { name: "Alle ablehnen" }).click();
  await expect(mirror).toHaveValue(original);
  await expect(findings).toHaveCount(2);
  await expect(status).toHaveText("Prüfung abgeschlossen: 2 Befund(e).");

  await preview.click();
  await page.getByRole("button", { name: "Alle annehmen" }).click();
  await expect(mirror).toHaveValue("Bitte herunterladen Sie per sofort.");
  await expect(findings).toHaveCount(0);
  await page.getByTestId("editor-undo").click();
  await expect(mirror).toHaveValue(original);
  expect(requests).toEqual(["GET /api/advisor/docs", "POST /api/advisor/validate"]);

  await start.click();
  await expect(status).toHaveText("Prüfung abgeschlossen: 2 Befund(e).");
  await preview.click();
  // Apply a transaction while the preview is visible to exercise the commit-time check.
  await page.getByTestId("editor-input").evaluate((node) => {
    const { editor } = node as HTMLElement & { editor: { commands: {
      insertContentAt: (position: number, text: string) => void;
    } } };
    editor.commands.insertContentAt(1, "Neu: ");
  });
  await page.getByRole("button", { name: "Alle annehmen" }).click();
  await expect(page.locator("[data-review-progress]")).toHaveText(/Der geprüfte Text hat sich geändert/u);
  await expect(mirror).toHaveValue(`Neu: ${original}`);
  await page.getByRole("button", { name: "Alle ablehnen" }).click();
  await expect(findings).toHaveCount(0);
  await expect(preview).toBeDisabled();

  await start.click();
  await expect(status).toHaveText("Prüfung abgeschlossen: 2 Befund(e).");
  // Change the editor without an update notification so the preview must check the text itself.
  const input = page.getByTestId("editor-input");
  await input.evaluate((node) => {
    const { editor } = node as HTMLElement & { editor: { commands: {
      setContent: (text: string, options: { emitUpdate: boolean }) => void;
    } } };
    editor.commands.setContent("Geänderter Editorinhalt.", { emitUpdate: false });
  });
  await preview.click();
  await expect(page.getByTestId("rewrite-diff-panel")).toBeHidden();
  await expect(status).toHaveText(/Der geprüfte Text hat sich geändert/u);
  await expect(findings).toHaveCount(0);
  await expect(input).toHaveText("Geänderter Editorinhalt.");
  expect(requests).toEqual([
    "GET /api/advisor/docs", "POST /api/advisor/validate", "POST /api/advisor/validate", "POST /api/advisor/validate",
  ]);
});

test("advisor switches overlapping alternatives and applies only accepted UTF-16 ranges", async ({ page }) => {
  await stubCorrection(page);
  const original = "📄 A per sofort und hier.\nAbsatz: x  y. Ende.";
  const matches: Array<[string, string | null]> = [
    ["per sofort", "ab sofort"],
    ["sofort und", ""],
    ["hier", null],
    ["Absatz", "Zeile"],
    ["x  y", " \t\u00a0 "],
    ["Ende", "Anfang\n🌿 Ende"],
  ];
  await page.route("**/api/advisor/validate", async (route) => {
    const body = matches.map(([matchedText, replacement]) => {
      const start = original.indexOf(matchedText);
      return `event:validation\ndata:${JSON.stringify({
        documentTitle: "Demo", ruleTitle: matchedText, page: 1, message: "Bitte prüfen.",
        matchedText, replacement, excerpt: original, referenceUrl: "/api/advisor/doc/schreibweisungen#page=1",
        start, end: start + matchedText.length,
      })}\n\n`;
    }).join("") + 'event:complete\ndata:{"incomplete":true}\n\n';
    await route.fulfill({ contentType: "text/event-stream", body });
  });

  await page.goto("/");
  await enterText(page, original);
  await page.getByTestId("workspace-mode-validate").click();
  await page.getByTestId("advisor-toggle").click();
  await page.getByTestId("advisor-document-checkbox").first().check();
  await page.getByTestId("advisor-start").click();
  await expect(page.getByTestId("advisor-status")).toHaveText("Prüfung unvollständig (6 Befund(e)).");

  const decisions = page.getByTestId("advisor-decision");
  await expect(decisions.nth(0)).toHaveAttribute("aria-pressed", "true");
  await expect(decisions.nth(1)).toHaveText("Überlappt mit ausgewähltem Vorschlag");
  await expect(decisions.nth(1)).toBeDisabled();
  await expect(decisions.nth(2)).toHaveText("Kein Ersatzvorschlag");
  await expect(decisions.nth(2)).toBeDisabled();
  await decisions.nth(0).click();
  await decisions.nth(1).click();
  await expect(decisions.nth(0)).toBeDisabled();
  await expect(decisions.nth(1)).toHaveAttribute("aria-pressed", "true");

  await page.getByTestId("advisor-preview").click();
  const accepts = page.locator("[data-review-inline] [data-diff-decision='accepted']");
  const rejects = page.locator("[data-review-inline] [data-diff-decision='rejected']");
  await expect(accepts).toHaveCount(4);
  await rejects.nth(1).click(); // Keep “Absatz”.
  for (let i = 0; i < 3; i += 1) await accepts.first().click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("📄 A per  hier.\nAbsatz:  \t\u00a0 . Anfang\n🌿 Ende.");
  await page.getByTestId("editor-undo").click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue(original);
});

test("advisor does not report success after stream failure or cancellation", async ({ page }) => {
  await stubCorrection(page);
  const progress = 'event:progress\ndata:{"checked":1,"total":2}\n\n';
  let attempt = 0;
  let finishCancelledRequest = () => {};
  const cancelledRequest = new Promise<void>((resolve) => { finishCancelledRequest = resolve; });
  await page.route("**/api/advisor/validate", async (route) => {
    attempt += 1;
    if (attempt === 3) {
      await cancelledRequest;
      await route.fulfill({
        contentType: "text/event-stream", body: 'event:complete\ndata:{"incomplete":false}\n\n',
      });
      return;
    }
    const body = attempt === 1 ? progress : progress
      + 'event:error\ndata:{"message":"Richtlinienprüfung fehlgeschlagen."}\n\n';
    await route.fulfill({ contentType: "text/event-stream", body });
  });

  await page.goto("/");
  const original = "Original bleibt erhalten.";
  await enterText(page, original);
  await page.getByTestId("workspace-mode-validate").click();
  await page.getByTestId("advisor-toggle").click();
  await page.getByTestId("advisor-document-checkbox").first().check();

  const start = page.getByTestId("advisor-start");
  const status = page.getByTestId("advisor-status");
  await start.click();
  await expect(status).toHaveText("Richtlinienprüfung konnte nicht abgeschlossen werden.");
  await start.click();
  await expect(status).toHaveText("Richtlinienprüfung fehlgeschlagen.");
  const pending = page.waitForRequest("**/api/advisor/validate");
  await start.click();
  await pending;
  await page.getByTestId("advisor-cancel").click();
  finishCancelledRequest();
  await expect(status).toHaveText("Prüfung wurde abgebrochen.");
  await expect(page.getByTestId("advisor-finding")).toHaveCount(0);
  await expect(page.getByTestId("editor-mirror")).toHaveValue(original);
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
    "rgb(237, 242, 245)",
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
    "rgb(237, 242, 245)",
  );
  await page.getByRole("button", { name: "Inline" }).click();
  await page.locator("[data-review-inline] [data-diff-decision='rejected']").first().click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Das neue Haus ist klein.");
  await page.getByTestId("editor-undo").click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Das alte Haus ist klein.");
});

test("the review shows a non-breaking space and keeps undo exact", async ({ page }) => {
  await stubCorrection(page);
  await page.route("**/api/quick-actions/plain-language", (route) =>
    route.fulfill({ json: { text: "Der Betrag beträgt CHF\u00a020." } }));
  await page.goto("/");
  await enterText(page, "Der Betrag beträgt CHF 20.");
  await page.getByTestId("mvp-action-plain-language").click();

  const diff = page.locator("[data-review-inline]");
  await expect(diff.locator(".diff-removed")).toHaveText("·");
  await expect(diff.locator(".diff-added")).toHaveText("⍽");
  await page.getByRole("button", { name: "Alle annehmen" }).click();
  expect(await page.getByTestId("editor-mirror").inputValue()).toBe("Der Betrag beträgt CHF\u00a020.");
  await page.getByTestId("editor-undo").click();
  expect(await page.getByTestId("editor-mirror").inputValue()).toBe("Der Betrag beträgt CHF 20.");
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
  const summary = page.getByTestId("mvp-summary-option");
  expect(await summary.locator("option").evaluateAll(options =>
    options.map(option => (option as HTMLOptionElement).value),
  )).toEqual(["", "sentence", "three_sentence", "paragraph", "page", "management_summary"]);
  await summary.selectOption("management_summary");

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
        text: "# Import Titel\n* <script>Importierter Text.</script>",
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
    "# Import Titel\n* <script>Importierter Text.</script>",
  );
  await expect(page.getByTestId("editor-input").locator("script")).toHaveCount(0);
  expect(requestUrl).toContain("ocrLanguage=fr");
});

test("the full editor accepts document drag and drop", async ({ page }) => {
  await stubCorrection(page);
  await page.route("**/api/convert/doc**", async (route) => {
    await route.fulfill({ json: { text: "Per Drop importiert." } });
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
