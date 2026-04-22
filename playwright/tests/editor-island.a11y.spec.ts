import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";

interface QuickActionRequestPayload {
  text: string;
  language: string;
  option?: string;
}

function createSseBody(events: Array<{ event: string; payload: unknown }>): string {
  return events
    .map(({ event, payload }) => `event: ${event}\ndata: ${JSON.stringify(payload)}\n\n`)
    .join("");
}

async function expectNoCriticalOrSeriousViolations(page: Page): Promise<void> {
  const results = await new AxeBuilder({
    page,
  })
    .exclude("[data-advisor-pdf-frame]")
    .analyze();

  const blockingViolations = results.violations.filter(
    (violation) => violation.impact === "critical" || violation.impact === "serious",
  );

  expect(
    blockingViolations.map((violation) => ({
      id: violation.id,
      impact: violation.impact,
      nodes: violation.nodes.length,
      description: violation.description,
      targets: violation.nodes.map((node) => node.target.join(" ")),
      snippets: violation.nodes.map((node) => node.html),
    })),
  ).toEqual([]);
}

test("axe: idle state has no critical or serious violations", async ({ page }) => {
  await page.goto("/");
  await expectNoCriticalOrSeriousViolations(page);
});

test("axe: quick action configuration state has no critical or serious violations", async ({
  page,
}) => {
  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  await editor.click();
  await page.keyboard.type("Das ist ein kurzer Testtext.");
  await page.getByTestId("quick-action-summarize").click();
  await expect(page.getByTestId("quick-action-config-summarize")).toBeVisible();

  await expectNoCriticalOrSeriousViolations(page);
});

test("axe: streaming plus diff state has no critical or serious violations", async ({
  page,
}) => {
  await page.route("**/api/quick-actions/summarize/stream", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    await route.fulfill({
      status: 200,
      headers: {
        "Content-Type": "text/event-stream",
      },
      body: createSseBody([
        {
          event: "chunk",
          payload: {
            text: `Kurzfassung (${payload.option ?? "sentence"}): `,
          },
        },
        {
          event: "complete",
          payload: {
            text: "Kurzfassung (sentence): Das ist ein kurzer Testtext.",
          },
        },
      ]),
    });
  });

  await page.goto("/");
  const editor = page.getByTestId("editor-input");
  await editor.click();
  await page.keyboard.type("Das ist ein kurzer Testtext.");
  await page.getByTestId("quick-action-summarize").click();
  await page.getByTestId("quick-action-run").click();
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();

  await expectNoCriticalOrSeriousViolations(page);
});

test("axe: advisor results plus viewer state has no critical or serious violations", async ({
  page,
}) => {
  await page.route("**/api/advisor/validate", async (route) => {
    await route.fulfill({
      status: 200,
      headers: {
        "Content-Type": "text/event-stream",
      },
      body: createSseBody([
        {
          event: "validation",
          payload: {
            stableKey: "schreibweisungen::per-sofort-vermeiden::per-sofort",
            documentName: "schreibweisungen",
            documentTitle: "Schreibweisungen",
            ruleId: "per-sofort-vermeiden",
            ruleTitle: "Per sofort vermeiden",
            page: 1,
            pageLabel: "Seite 1",
            message: "Nutze stattdessen 'ab sofort'.",
            matchedText: "per sofort",
            excerpt: "Bitte downloaden Sie das Formular per sofort.",
            suggestion: "Bitte laden Sie das Formular ab sofort herunter.",
            referenceUrl: "/api/advisor/doc/schreibweisungen#page=1",
          },
        },
      ]),
    });
  });

  await page.goto("/");
  await page.getByTestId("editor-input").click();
  await page.keyboard.type("Bitte downloaden Sie das Formular per sofort.");
  await page.getByTestId("advisor-doc-checkbox").first().check();
  await page.getByTestId("advisor-validate").click();
  await expect(page.getByTestId("advisor-result-item")).toBeVisible();
  await page.getByTestId("advisor-result-detail-open").click();
  await expect(page.getByTestId("advisor-pdf-viewer")).toBeVisible();

  await expectNoCriticalOrSeriousViolations(page);
});

test.describe("mobile accessibility", () => {
  test.use({
    viewport: {
      width: 390,
      height: 844,
    },
  });

  test("axe: mobile stacked layout has no critical or serious violations", async ({
    page,
  }) => {
    await page.goto("/");
    await page.getByTestId("quick-action-custom").click();
    await expect(page.getByTestId("quick-action-config-custom")).toBeVisible();
    await expectNoCriticalOrSeriousViolations(page);
  });
});

test("keyboard: upload button supports Enter and keeps OCR select interaction isolated", async ({
  page,
}) => {
  await page.goto("/");

  const uploadButton = page.getByTestId("document-import-button");
  await uploadButton.focus();

  await Promise.all([
    page.waitForEvent("filechooser"),
    page.keyboard.press("Enter"),
  ]);

  let chooserCount = 0;
  page.on("filechooser", () => {
    chooserCount += 1;
  });

  await page.getByTestId("document-import-ocr-language").focus();
  await page.keyboard.press("Space");
  await page.waitForTimeout(150);

  expect(chooserCount).toBe(0);
});

test("keyboard: rewrite bubble closes with Escape", async ({
  page,
}) => {
  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  await editor.click();
  await page.keyboard.type("Alpha schnell.");
  await page.keyboard.press("ArrowLeft");
  await expect(page.getByTestId("rewrite-bubble")).toBeVisible();

  await page.keyboard.press("Escape");

  await expect(page.getByTestId("rewrite-bubble")).toBeHidden();
});

test("keyboard: advisor viewer closes with Escape and restores focus to opener", async ({
  page,
}) => {
  await page.goto("/");

  const openButton = page.getByTestId("advisor-doc-open").first();
  await openButton.click();
  await expect(page.getByTestId("advisor-pdf-viewer")).toBeVisible();
  await expect(page.getByTestId("advisor-pdf-close")).toBeFocused();

  await page.keyboard.press("Escape");

  await expect(page.getByTestId("advisor-pdf-viewer")).toBeHidden();
  await expect(openButton).toBeFocused();
});
