import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";

function correctionResponse(text: string) {
  const offset = text.indexOf("teh");
  return {
    original: text,
    blocks:
      offset < 0
        ? []
        : [
            {
              offset,
              length: 3,
              message: "Possible spelling mistake found.",
              shortMessage: "Spelling",
              ruleId: "STUB_SPELLING_TEH",
              replacements: ["the"],
            },
          ],
  };
}

async function prepare(page: Page): Promise<void> {
  await page.route("**/api/text-correction", async (route) => {
    const payload = route.request().postDataJSON() as { text: string };
    await route.fulfill({ json: correctionResponse(payload.text) });
  });
  await page.goto("/");
}

async function selectTextLanguage(page: Page, language: string): Promise<void> {
  await page.getByTestId("workspace-mode-validate").click();
  await page.getByTestId("workspace-language").selectOption(language);
  await page.getByTestId("workspace-mode-transform").click();
}

async function expectNoBlockingViolations(page: Page): Promise<void> {
  const results = await new AxeBuilder({ page }).analyze();
  const blocking = results.violations.filter(
    (violation) => violation.impact === "critical" || violation.impact === "serious",
  );
  expect(
    blocking.map((violation) => ({
      id: violation.id,
      nodes: violation.nodes.map((node) => node.target.join(" ")),
    })),
  ).toEqual([]);
}

test("axe: idle workspace", async ({ page }) => {
  await prepare(page);
  await expectNoBlockingViolations(page);
});

test("axe: correction rail", async ({ page }) => {
  await prepare(page);
  await page.getByTestId("editor-input").fill("Das ist teh Text.");
  await expect(page.getByTestId("correction-mode-badge")).toHaveText("1");
  await page.getByTestId("workspace-mode-validate").click();
  await expect(page.getByTestId("correction-rail")).toBeVisible();
  await expectNoBlockingViolations(page);
});

test("axe: inline and split diff review", async ({ page }) => {
  await prepare(page);
  await page.route("**/api/quick-actions/plain-language", async (route) => {
    await route.fulfill({ json: { text: "Das neue Haus ist gross." } });
  });
  await page.getByTestId("editor-input").fill("Das alte Haus ist klein.");
  await selectTextLanguage(page, "de-CH");
  await page.getByTestId("mvp-action-plain-language").click();
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expectNoBlockingViolations(page);
  await page.getByRole("button", { name: "Zwei Spalten" }).click();
  await expect(page.getByTestId("review-split")).toBeVisible();
  await expectNoBlockingViolations(page);
});

test("axe: statistics popover", async ({ page }) => {
  await prepare(page);
  await page.getByTestId("editor-input").fill("Mal Tal. Ball Fall.");
  await page.getByTestId("workspace-mode-validate").click();
  await page.getByTestId("workspace-language").selectOption("de-CH");
  await page.getByTestId("editor-stats-toggle").click();
  await expect(page.getByTestId("stats-popover")).toBeVisible();
  await expectNoBlockingViolations(page);
});

test("axe: local mode popover", async ({ page }) => {
  await prepare(page);
  await page.getByTestId("local-mode-trigger").click();
  await expect(page.getByTestId("local-mode-popover")).toBeVisible();
  await expectNoBlockingViolations(page);
});

test.describe("mobile accessibility", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("axe: mobile ribbon and correction slideover", async ({ page }) => {
    await prepare(page);
    await page.getByTestId("editor-input").fill("Das ist teh Text.");
    await expect(page.getByTestId("correction-mode-badge")).toHaveText("1");
    await page.getByTestId("workspace-mode-validate").click();
    await expect(page.getByTestId("correction-rail")).toBeVisible();
    await expectNoBlockingViolations(page);
  });
});
