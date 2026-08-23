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
  await expect(page.getByTestId("inspector-panel")).toBeHidden();
  await expect(page.getByTestId("mvp-action-summarize")).toBeVisible();
  await expect(page.getByTestId("mvp-action-plain-language")).toBeVisible();
  await expect(page.getByTestId("quick-action-proofread")).toBeHidden();
  await expect(page.getByTestId("quick-action-bullet-points")).toBeHidden();
  await expect(page.getByTestId("quick-action-formality")).toBeHidden();
  await expect(page.getByTestId("advisor-panel")).toBeHidden();
  await expect(page.getByTestId("rewrite-bubble")).toBeHidden();

  const widths = await page.evaluate(() => ({
    viewport: document.documentElement.clientWidth,
    editor: document.querySelector("[data-editor-shell]")?.getBoundingClientRect().width ?? 0,
  }));
  expect(widths.editor).toBeGreaterThan(widths.viewport * 0.75);
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

test("correction stays compact in transform mode and rail opens only in validate mode", async ({
  page,
}) => {
  await stubCorrection(page);
  await page.goto("/");
  await enterText(page, "Das ist teh Text.");

  await expect(page.getByTestId("correction-mode-badge")).toHaveText("1");
  await expect(page.getByTestId("inspector-panel")).toBeHidden();

  await page.getByTestId("workspace-mode-validate").click();
  await expect(page.getByTestId("inspector-panel")).toBeVisible();
  await expect(page.getByTestId("correction-results-toggle")).toHaveAttribute(
    "aria-expanded",
    "true",
  );
  await expect(page.getByTestId("correction-retry")).toBeHidden();

  await page.getByRole("button", { name: "Korrekturergebnisse schliessen" }).click();
  await expect(page.getByTestId("inspector-panel")).toBeHidden();
  await page.getByTestId("workspace-mode-transform").click();
  await page.getByTestId("workspace-mode-validate").click();
  await expect(page.getByTestId("inspector-panel")).toBeVisible();
  await page.getByTestId("correction-results-toggle").click();
  await expect(page.getByTestId("inspector-panel")).toBeHidden();
  await page.getByTestId("correction-results-toggle").click();
  await expect(page.getByTestId("inspector-panel")).toBeVisible();

  await page.getByRole("button", { name: "the", exact: true }).click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Das ist the Text.");
  await expect(page.getByTestId("inspector-panel")).toBeHidden();
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

  await page.getByTestId("mvp-action-plain-language").click();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Das alte Haus ist klein.");
  await expect(page.getByTestId("workspace-mode-validate")).toBeDisabled();
  release?.();

  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expect(page.getByTestId("editor-input")).toBeHidden();
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Das alte Haus ist klein.");
  await expect(
    page.locator("[data-review-inline] [data-diff-decision='accepted']"),
  ).toHaveCount(2);

  await page.locator("[data-review-inline] [data-diff-decision='accepted']").first().click();
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
  await page.getByTestId("mvp-summary-option").selectOption("management_summary");
  await page.getByTestId("mvp-action-summarize").click();

  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  expect(payloads[0]?.option).toBe("management_summary");
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

  await page.getByTestId("mvp-action-plain-language").click();
  await expect(page.getByText("Keine Änderungen gefunden")).toBeVisible();
  await page.getByRole("button", { name: "Abbrechen" }).click();
  await page.getByTestId("mvp-action-plain-language").click();
  await expect(page.getByTestId("workspace-status")).toContainText(
    "Transformation fehlgeschlagen",
  );
  await expect(page.getByTestId("editor-mirror")).toHaveValue("Unverändert.");
});

test("toolbar copies, downloads DOCX and opens the live statistics popover", async ({
  page,
  context,
}) => {
  await context.grantPermissions(["clipboard-read", "clipboard-write"]);
  await stubCorrection(page);
  await page.goto("/");
  await enterText(page, "Mal Tal. Ball Fall.");

  await page.getByTestId("editor-copy").click();
  await expect.poll(() => page.evaluate(() => navigator.clipboard.readText())).toBe(
    "Mal Tal. Ball Fall.",
  );
  await page.getByTestId("editor-stats-toggle").click();
  await expect(page.getByTestId("stats-popover")).toBeVisible();
  await expect(page.getByTestId("text-stats-flesch")).toHaveText("119.5");
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
    await route.fulfill({ json: { html: "<h2>Import Titel</h2><p>Importierter Text.</p>" } });
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

  await expect(page.getByTestId("editor-input")).toContainText("Import Titel");
  await expect(page.getByTestId("editor-mirror")).toHaveValue(/Importierter Text/);
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

  test("uses a focusable correction slideover without horizontal overflow", async ({ page }) => {
    await stubCorrection(page);
    await page.goto("/");
    await enterText(page, "Das ist teh Text.");
    await page.getByTestId("workspace-mode-validate").click();

    await expect(page.getByTestId("inspector-panel")).toBeVisible();
    await expect(page.locator("body")).toHaveAttribute("data-correction-slideover-open", "true");
    await page.keyboard.press("Escape");
    await expect(page.getByTestId("inspector-panel")).toBeHidden();
    const hasOverflow = await page.evaluate(
      () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
    );
    expect(hasOverflow).toBe(false);
  });
});
