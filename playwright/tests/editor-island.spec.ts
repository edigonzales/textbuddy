import { expect, test } from "@playwright/test";

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

function createSseBody(events: Array<{ event: string; payload: unknown }>): string {
  return events
    .map(({ event, payload }) => `event: ${event}\ndata: ${JSON.stringify(payload)}\n\n`)
    .join("");
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

test("rewrite bubble switches between word and sentence mode based on focus", async ({ page }) => {
  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const bubble = page.getByTestId("rewrite-bubble");
  const primaryAction = page.getByTestId("rewrite-primary-action");
  const secondaryAction = page.getByTestId("rewrite-secondary-action");

  await editor.click();
  await page.keyboard.type("Alpha schnell.");

  await expect(bubble).toBeVisible();
  await expect(primaryAction).toHaveText("Satz umschreiben");
  await expect(secondaryAction).toBeHidden();

  await page.keyboard.press("ArrowLeft");

  await expect(bubble).toBeVisible();
  await expect(primaryAction).toHaveText("Wort umschreiben");
  await expect(secondaryAction).toHaveText("Satz umschreiben");
  await expect(secondaryAction).toBeVisible();

  await page.keyboard.press("ArrowRight");

  await expect(bubble).toBeVisible();
  await expect(primaryAction).toHaveText("Satz umschreiben");
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

  await expect(bubble).toBeVisible();
  await expect(page.getByTestId("rewrite-primary-action")).toHaveText("Satz umschreiben");

  await page.keyboard.press("ArrowLeft");

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
        original: payload.sentence,
        alternatives: ["Alpha Alternative."],
      },
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");

  await editor.click();
  await page.keyboard.type("Alpha schnell. Beta Satz.");
  await page.keyboard.press("ArrowUp");
  await page.keyboard.press("ArrowLeft");

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

  await expect(page.getByTestId("rewrite-bubble")).toBeVisible();
  await expect(page.getByTestId("rewrite-primary-action")).toHaveText("Satz umschreiben");
  await expect(page.getByTestId("rewrite-secondary-action")).toBeHidden();
});

test("plain language streams into the editor, shows a diff and supports full undo", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/plain-language/stream", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      status: 200,
      headers: {
        "Content-Type": "text/event-stream",
      },
      body: createSseBody([
        {
          event: "chunk",
          payload: {
            text: "Kurz und einfach: ",
          },
        },
        {
          event: "chunk",
          payload: {
            text: "Der einfache Thema ",
          },
        },
        {
          event: "chunk",
          payload: {
            text: "ist wichtig.",
          },
        },
        {
          event: "complete",
          payload: {
            text: "Kurz und einfach: Der einfache Thema ist wichtig.",
          },
        },
      ]),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");

  await expect(page.locator("[data-quick-action]")).toHaveCount(7);

  await editor.click();
  await page.keyboard.type("Der komplizierte Sachverhalt ist relevant.");
  await page.getByTestId("quick-action-plain-language").click();

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
  await expect(page.getByTestId("quick-action-status")).toContainText("rueckgaengig");
});

test("bullet points stream into the editor, show a diff and support full undo", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/bullet-points/stream", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      status: 200,
      headers: {
        "Content-Type": "text/event-stream",
      },
      body: createSseBody([
        {
          event: "chunk",
          payload: {
            text: "- Projektlage klaeren\n",
          },
        },
        {
          event: "chunk",
          payload: {
            text: "- Naechste Schritte festhalten",
          },
        },
        {
          event: "complete",
          payload: {
            text: "- Projektlage klaeren\n- Naechste Schritte festhalten",
          },
        },
      ]),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");

  await editor.click();
  await page.keyboard.type("Projektlage klaeren. Naechste Schritte festhalten.");
  await page.getByTestId("quick-action-bullet-points").click();

  await expect.poll(() => requestBodies.at(-1)?.text).toBe(
    "Projektlage klaeren. Naechste Schritte festhalten.",
  );
  await expect.poll(() => requestBodies.at(-1)?.language).toBe("auto");
  await expect(page.getByTestId("quick-action-status")).toContainText("Bullet Points abgeschlossen");
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
  await expect(page.getByTestId("quick-action-status")).toContainText("rueckgaengig");
});

test("proofread streams into the editor, shows a diff and supports full undo", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/proofread/stream", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      status: 200,
      headers: {
        "Content-Type": "text/event-stream",
      },
      body: createSseBody([
        {
          event: "chunk",
          payload: {
            text: "This is ",
          },
        },
        {
          event: "chunk",
          payload: {
            text: "the text.",
          },
        },
        {
          event: "complete",
          payload: {
            text: "This is the text.",
          },
        },
      ]),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");

  await editor.click();
  await page.keyboard.type("This is teh text.");
  await page.getByTestId("quick-action-proofread").click();

  await expect.poll(() => requestBodies.at(-1)?.text).toBe("This is teh text.");
  await expect.poll(() => requestBodies.at(-1)?.language).toBe("auto");
  await expect(page.getByTestId("quick-action-status")).toContainText("Proofread abgeschlossen");
  await expect(mirror).toHaveValue("This is the text.");
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expect(page.getByTestId("rewrite-diff-before")).toContainText("This is teh text.");
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("This is the text.");

  await page.getByTestId("rewrite-diff-undo").click();

  await expect(mirror).toHaveValue("This is teh text.");
  await expect(page.getByTestId("rewrite-diff-panel")).toBeHidden();
  await expect(page.getByTestId("quick-action-status")).toContainText("rueckgaengig");
});

test("summarize with the sentence option streams into the editor and sends the selected option", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/summarize/stream", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      status: 200,
      headers: {
        "Content-Type": "text/event-stream",
      },
      body: createSseBody([
        {
          event: "chunk",
          payload: {
            text: "Kurzfassung: ",
          },
        },
        {
          event: "chunk",
          payload: {
            text: "Der Kernpunkt steht fest.",
          },
        },
        {
          event: "complete",
          payload: {
            text: "Kurzfassung: Der Kernpunkt steht fest.",
          },
        },
      ]),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");

  await editor.click();
  await page.keyboard.type("Der Kernpunkt steht fest. Weitere Details folgen.");
  await expect(page.getByTestId("quick-action-summarize-option")).toHaveValue("sentence");
  await page.getByTestId("quick-action-summarize").click();

  await expect.poll(() => requestBodies.at(-1)?.text).toBe(
    "Der Kernpunkt steht fest. Weitere Details folgen.",
  );
  await expect.poll(() => requestBodies.at(-1)?.language).toBe("auto");
  await expect.poll(() => requestBodies.at(-1)?.option).toBe("sentence");
  await expect(page.getByTestId("quick-action-status")).toContainText("Summarize abgeschlossen");
  await expect(mirror).toHaveValue("Kurzfassung: Der Kernpunkt steht fest.");
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expect(page.getByTestId("rewrite-diff-after")).toContainText(
    "Kurzfassung: Der Kernpunkt steht fest.",
  );
});

test("summarize with the management summary option streams the selected variant", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/summarize/stream", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);
    await route.fulfill({
      status: 200,
      headers: {
        "Content-Type": "text/event-stream",
      },
      body: createSseBody([
        {
          event: "chunk",
          payload: {
            text: "Management Summary\n",
          },
        },
        {
          event: "chunk",
          payload: {
            text: "- Kernpunkt: Projekt ist freigegeben.\n- Empfehlung: Umsetzung starten.",
          },
        },
        {
          event: "complete",
          payload: {
            text: "Management Summary\n- Kernpunkt: Projekt ist freigegeben.\n- Empfehlung: Umsetzung starten.",
          },
        },
      ]),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");

  await editor.click();
  await page.keyboard.type("Projekt ist freigegeben. Umsetzung kann starten.");
  await page.getByTestId("quick-action-summarize-option").selectOption("management_summary");
  await page.getByTestId("quick-action-summarize").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("management_summary");
  await expect(page.getByTestId("quick-action-status")).toContainText("Summarize abgeschlossen");
  await expect(mirror).toHaveValue(
    "Management Summary\n- Kernpunkt: Projekt ist freigegeben.\n- Empfehlung: Umsetzung starten.",
  );
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("Management Summary");
  await expect(page.getByTestId("rewrite-diff-after")).toContainText(
    "- Empfehlung: Umsetzung starten.",
  );
});

test("formality streams both formal and informal variants with the selected option", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/formality/stream", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);

    const responseText =
      payload.option === "informal"
        ? "Lockerer formuliert: Hallo, wir brauchen schnell deine Rueckmeldung."
        : "Formell ueberarbeitet: Guten Tag, wir benoetigen zeitnah Ihre Rueckmeldung.";

    await route.fulfill({
      status: 200,
      headers: {
        "Content-Type": "text/event-stream",
      },
      body: createSseBody([
        {
          event: "chunk",
          payload: {
            text: responseText.slice(0, 24),
          },
        },
        {
          event: "chunk",
          payload: {
            text: responseText.slice(24),
          },
        },
        {
          event: "complete",
          payload: {
            text: responseText,
          },
        },
      ]),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");
  const optionSelect = page.getByTestId("quick-action-formality-option");

  await editor.click();
  await page.keyboard.type("Hallo, wir brauchen schnell deine Rueckmeldung.");
  await expect(optionSelect).toHaveValue("formal");
  await page.getByTestId("quick-action-formality").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("formal");
  await expect(page.getByTestId("quick-action-status")).toContainText("Formality abgeschlossen");
  await expect(mirror).toHaveValue(
    "Formell ueberarbeitet: Guten Tag, wir benoetigen zeitnah Ihre Rueckmeldung.",
  );
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();

  await page.getByTestId("rewrite-diff-undo").click();

  await expect(mirror).toHaveValue("Hallo, wir brauchen schnell deine Rueckmeldung.");
  await optionSelect.selectOption("informal");
  await page.getByTestId("quick-action-formality").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("informal");
  await expect(page.getByTestId("quick-action-status")).toContainText("Formality abgeschlossen");
  await expect(mirror).toHaveValue(
    "Lockerer formuliert: Hallo, wir brauchen schnell deine Rueckmeldung.",
  );
  await expect(page.getByTestId("rewrite-diff-after")).toContainText(
    "Lockerer formuliert: Hallo, wir brauchen schnell deine Rueckmeldung.",
  );
});

test("social media streams multiple channel variants with the selected option", async ({
  page,
}) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/social-media/stream", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);

    const responseText =
      payload.option === "linkedin"
        ? "LinkedIn-Post: Produktstart ist live.\n\nTakeaway: Team ist bereit."
        : "Bluesky-Post: Produktstart ist live. Fokus: Team ist bereit.";

    await route.fulfill({
      status: 200,
      headers: {
        "Content-Type": "text/event-stream",
      },
      body: createSseBody([
        {
          event: "chunk",
          payload: {
            text: responseText.slice(0, 24),
          },
        },
        {
          event: "chunk",
          payload: {
            text: responseText.slice(24),
          },
        },
        {
          event: "complete",
          payload: {
            text: responseText,
          },
        },
      ]),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");
  const optionSelect = page.getByTestId("quick-action-social-media-option");

  await editor.click();
  await page.keyboard.type("Produktstart ist live. Team ist bereit.");
  await expect(optionSelect).toHaveValue("bluesky");
  await page.getByTestId("quick-action-social-media").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("bluesky");
  await expect(page.getByTestId("quick-action-status")).toContainText("Social Media abgeschlossen");
  await expect(mirror).toHaveValue("Bluesky-Post: Produktstart ist live. Fokus: Team ist bereit.");
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();

  await page.getByTestId("rewrite-diff-undo").click();

  await expect(mirror).toHaveValue("Produktstart ist live. Team ist bereit.");
  await optionSelect.selectOption("linkedin");
  await page.getByTestId("quick-action-social-media").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("linkedin");
  await expect(page.getByTestId("quick-action-status")).toContainText("Social Media abgeschlossen");
  await expect(mirror).toHaveValue("LinkedIn-Post: Produktstart ist live.\n\nTakeaway: Team ist bereit.");
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("LinkedIn-Post: Produktstart ist live.");
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("Takeaway: Team ist bereit.");
});

test("medium streams multiple medium variants with the selected option", async ({ page }) => {
  const requestBodies: QuickActionRequestPayload[] = [];

  await page.route("**/api/quick-actions/medium/stream", async (route) => {
    const payload = route.request().postDataJSON() as QuickActionRequestPayload;

    requestBodies.push(payload);

    const responseText =
      payload.option === "report"
        ? "Bericht\n\nZusammenfassung: Projekt ist freigegeben.\nDetails: Team startet am Montag.\nAbschluss: Umsetzung beginnt sofort."
        : "Betreff: Projektupdate\n\nHallo Team,\n\nProjekt ist freigegeben. Team startet am Montag.\n\nViele Gruesse";

    await route.fulfill({
      status: 200,
      headers: {
        "Content-Type": "text/event-stream",
      },
      body: createSseBody([
        {
          event: "chunk",
          payload: {
            text: responseText.slice(0, 24),
          },
        },
        {
          event: "chunk",
          payload: {
            text: responseText.slice(24),
          },
        },
        {
          event: "complete",
          payload: {
            text: responseText,
          },
        },
      ]),
    });
  });

  await page.goto("/");

  const editor = page.getByTestId("editor-input");
  const mirror = page.getByTestId("editor-mirror");
  const optionSelect = page.getByTestId("quick-action-medium-option");

  await editor.click();
  await page.keyboard.type("Projekt ist freigegeben. Team startet am Montag.");
  await expect(optionSelect).toHaveValue("email");
  await page.getByTestId("quick-action-medium").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("email");
  await expect(page.getByTestId("quick-action-status")).toContainText("Medium abgeschlossen");
  await expect(mirror).toHaveValue(
    "Betreff: Projektupdate\n\nHallo Team,\n\nProjekt ist freigegeben. Team startet am Montag.\n\nViele Gruesse",
  );
  await expect(page.getByTestId("rewrite-diff-panel")).toBeVisible();

  await page.getByTestId("rewrite-diff-undo").click();

  await expect(mirror).toHaveValue("Projekt ist freigegeben. Team startet am Montag.");
  await optionSelect.selectOption("report");
  await page.getByTestId("quick-action-medium").click();

  await expect.poll(() => requestBodies.at(-1)?.option).toBe("report");
  await expect(page.getByTestId("quick-action-status")).toContainText("Medium abgeschlossen");
  await expect(mirror).toHaveValue(
    "Bericht\n\nZusammenfassung: Projekt ist freigegeben.\nDetails: Team startet am Montag.\nAbschluss: Umsetzung beginnt sofort.",
  );
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("Bericht");
  await expect(page.getByTestId("rewrite-diff-after")).toContainText("Abschluss: Umsetzung beginnt sofort.");
});

test("incomplete sentences keep word mode without sentence action", async ({ page }) => {
  await page.goto("/");

  const editor = page.getByTestId("editor-input");

  await editor.click();
  await page.keyboard.type("Alpha schnell");
  await page.keyboard.press("ArrowLeft");

  await expect(page.getByTestId("rewrite-bubble")).toBeVisible();
  await expect(page.getByTestId("rewrite-primary-action")).toHaveText("Wort umschreiben");
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
