import { expect, test } from "@playwright/test";

type Rgb = readonly [number, number, number];

function parseCssColor(value: string): Rgb {
  const normalized = value.trim().toLowerCase();
  const hex = normalized.match(/^#([0-9a-f]{6})$/);
  if (hex) {
    return [
      Number.parseInt(hex[1].slice(0, 2), 16),
      Number.parseInt(hex[1].slice(2, 4), 16),
      Number.parseInt(hex[1].slice(4, 6), 16),
    ];
  }

  const rgb = normalized.match(
    /^rgba?\(\s*([\d.]+)[,\s]+([\d.]+)[,\s]+([\d.]+)(?:[,\s/]+[\d.]+)?\s*\)$/,
  );
  if (rgb) {
    return [Number(rgb[1]), Number(rgb[2]), Number(rgb[3])];
  }

  throw new Error(`Unsupported CSS color: ${value}`);
}

function relativeLuminance(color: string): number {
  const channels = parseCssColor(color).map((channel) => {
    const normalized = channel / 255;
    return normalized <= 0.04045
      ? normalized / 12.92
      : ((normalized + 0.055) / 1.055) ** 2.4;
  });

  return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
}

function contrastRatio(first: string, second: string): number {
  const lighter = Math.max(relativeLuminance(first), relativeLuminance(second));
  const darker = Math.min(relativeLuminance(first), relativeLuminance(second));
  return (lighter + 0.05) / (darker + 0.05);
}

test.beforeEach(async ({ page }) => {
  await page.goto("/");
});

test("design tokens expose the consolidated palette", async ({ page }) => {
  const tokens = await page.evaluate(() => {
    const styles = getComputedStyle(document.documentElement);
    const value = (name: string) => styles.getPropertyValue(name).trim();

    return {
      primary: value("--textbuddy-color-primary"),
      interactiveBackground: value("--textbuddy-color-interactive-bg"),
      ink: value("--textbuddy-neutral-ink"),
      muted: value("--textbuddy-neutral-muted"),
      controlBorder: value("--textbuddy-neutral-control-border"),
      border: value("--textbuddy-neutral-border"),
      surface: value("--textbuddy-neutral-surface"),
      canvas: value("--textbuddy-neutral-canvas"),
      subtle: value("--textbuddy-neutral-subtle"),
      removedTokens: [
        "--textbuddy-color-primary-hover",
        "--textbuddy-color-primary-active",
        "--textbuddy-color-active-bg",
        "--textbuddy-neutral-emphasis",
        "--textbuddy-neutral-header",
        "--textbuddy-neutral-hover",
        "--textbuddy-neutral-deleted-bg",
      ].map(value),
    };
  });

  expect(tokens).toEqual({
    primary: "#357ab4",
    interactiveBackground: "#e8f1f7",
    ink: "#27333d",
    muted: "#5e6d79",
    controlBorder: "#8496a3",
    border: "#d3dde5",
    surface: "#ffffff",
    canvas: "#f5f7f9",
    subtle: "#edf2f5",
    removedTokens: ["", "", "", "", "", "", ""],
  });
});

test("text, controls, focus and selection meet their contrast targets", async ({ page }) => {
  const colors = await page.evaluate(() => {
    const styles = getComputedStyle(document.documentElement);
    const value = (name: string) => styles.getPropertyValue(name).trim();

    return {
      primary: value("--textbuddy-color-primary"),
      interactiveBackground: value("--textbuddy-color-interactive-bg"),
      ink: value("--textbuddy-neutral-ink"),
      muted: value("--textbuddy-neutral-muted"),
      controlBorder: value("--textbuddy-neutral-control-border"),
      surface: value("--textbuddy-neutral-surface"),
      canvas: value("--textbuddy-neutral-canvas"),
      subtle: value("--textbuddy-neutral-subtle"),
    };
  });

  const nonTextPairs = [
    [colors.primary, colors.surface],
    [colors.primary, colors.canvas],
    [colors.primary, colors.interactiveBackground],
    [colors.controlBorder, colors.surface],
  ] as const;
  for (const [foreground, background] of nonTextPairs) {
    expect(contrastRatio(foreground, background)).toBeGreaterThanOrEqual(3);
  }

  const textPairs = [
    [colors.ink, colors.surface],
    [colors.muted, colors.surface],
    [colors.muted, colors.subtle],
  ] as const;
  for (const [foreground, background] of textPairs) {
    expect(contrastRatio(foreground, background)).toBeGreaterThanOrEqual(4.5);
  }
});

test("rendered controls use the accessible interaction roles", async ({ page }) => {
  const styles = await page.evaluate(() => {
    const root = getComputedStyle(document.documentElement);
    const selectedTab = getComputedStyle(
      document.querySelector<HTMLElement>('.workspace-mode-tab[aria-selected="true"]')!,
    );
    const summarySelect = getComputedStyle(document.querySelector<HTMLElement>(".ribbon-select")!);
    const quickAction = getComputedStyle(document.querySelector<HTMLElement>(".ribbon-action")!);

    return {
      primary: root.getPropertyValue("--textbuddy-color-primary").trim(),
      interactiveBackground: root.getPropertyValue("--textbuddy-color-interactive-bg").trim(),
      controlBorder: root.getPropertyValue("--textbuddy-neutral-control-border").trim(),
      selectedBackground: selectedTab.backgroundColor,
      selectedIndicator: selectedTab.boxShadow,
      selectAppearance: summarySelect.appearance,
      selectBorder: summarySelect.borderTopColor,
      selectBackground: summarySelect.backgroundColor,
      actionBorder: quickAction.borderTopColor,
    };
  });

  expect(parseCssColor(styles.selectedBackground)).toEqual(
    parseCssColor(styles.interactiveBackground),
  );
  expect(styles.selectedIndicator).toContain("rgb(53, 122, 180)");
  expect(styles.selectAppearance).toBe("auto");
  expect(parseCssColor(styles.selectBorder)).toEqual(parseCssColor(styles.controlBorder));
  expect(parseCssColor(styles.actionBorder)).toEqual(parseCssColor(styles.controlBorder));
  expect(contrastRatio(styles.selectBorder, styles.selectBackground)).toBeGreaterThanOrEqual(3);
  expect(contrastRatio(styles.primary, styles.selectedBackground)).toBeGreaterThanOrEqual(3);
});
