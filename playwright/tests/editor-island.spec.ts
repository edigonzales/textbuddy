import { expect, test } from "@playwright/test";

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
