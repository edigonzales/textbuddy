import { expect, test } from "@playwright/test";

interface CorrectionRequestPayload {
  text: string;
  language: string;
}

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

test("text correction marks problems and applies a suggestion", async ({ page }) => {
  await page.goto("/");

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

  await page.getByTestId("correction-language").selectOption("de-DE");
  await page.getByTestId("editor-input").click();
  await page.keyboard.type("Hallo teh.");

  await expect.poll(() => requestBodies.at(-1)?.language).toBe("de-DE");
  await expect(page.getByTestId("correction-status")).toContainText("1 Problem");
});

test("local dictionary hides and restores known word matches", async ({ page }) => {
  await page.goto("/");

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
