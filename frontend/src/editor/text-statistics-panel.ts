import type { EditorTextChangedDetail } from "./types";
import { calculateTextStatistics, describeFleschScore } from "./text-statistics";

interface TextStatisticsElements {
  panel: HTMLElement;
  characters: HTMLElement;
  words: HTMLElement;
  syllables: HTMLElement;
  sentences: HTMLElement;
  averageSentenceLength: HTMLElement;
  averageSyllablesPerWord: HTMLElement;
  flesch: HTMLElement;
  fleschLabel: HTMLElement;
  fleschFill: HTMLElement;
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function formatDecimal(value: number, digits: number): string {
  return value.toFixed(digits);
}

function findElements(): TextStatisticsElements | null {
  const panel = document.querySelector<HTMLElement>("[data-text-stats-panel]");

  if (!panel) {
    return null;
  }

  const characters = panel.querySelector<HTMLElement>("[data-text-stats='characters']");
  const words = panel.querySelector<HTMLElement>("[data-text-stats='words']");
  const syllables = panel.querySelector<HTMLElement>("[data-text-stats='syllables']");
  const sentences = panel.querySelector<HTMLElement>("[data-text-stats='sentences']");
  const averageSentenceLength = panel.querySelector<HTMLElement>(
    "[data-text-stats='avg-sentence-length']",
  );
  const averageSyllablesPerWord = panel.querySelector<HTMLElement>(
    "[data-text-stats='avg-syllables-per-word']",
  );
  const flesch = panel.querySelector<HTMLElement>("[data-text-stats='flesch']");
  const fleschLabel = panel.querySelector<HTMLElement>("[data-text-stats-flesch-label]");
  const fleschFill = panel.querySelector<HTMLElement>("[data-text-stats-flesch-fill]");

  if (
    !characters ||
    !words ||
    !syllables ||
    !sentences ||
    !averageSentenceLength ||
    !averageSyllablesPerWord ||
    !flesch ||
    !fleschLabel ||
    !fleschFill
  ) {
    return null;
  }

  return {
    panel,
    characters,
    words,
    syllables,
    sentences,
    averageSentenceLength,
    averageSyllablesPerWord,
    flesch,
    fleschLabel,
    fleschFill,
  };
}

export function mountTextStatisticsPanel(): void {
  const root = document.querySelector<HTMLElement>("#editor-island-root");
  const mirror = document.querySelector<HTMLTextAreaElement>("[data-editor-mirror]");
  const elements = findElements();

  if (!root || !mirror || !elements) {
    return;
  }

  const resolvedElements = elements;

  function render(text: string): void {
    const stats = calculateTextStatistics(text);
    const normalizedFlesch = clamp((stats.fleschScore / 120) * 100, 0, 100);

    resolvedElements.characters.textContent = String(stats.characters);
    resolvedElements.words.textContent = String(stats.words);
    resolvedElements.syllables.textContent = String(stats.syllables);
    resolvedElements.sentences.textContent = String(stats.sentences);
    resolvedElements.averageSentenceLength.textContent = formatDecimal(stats.averageSentenceLength, 1);
    resolvedElements.averageSyllablesPerWord.textContent = formatDecimal(stats.averageSyllablesPerWord, 2);
    resolvedElements.flesch.textContent = formatDecimal(stats.fleschScore, 1);
    resolvedElements.fleschLabel.textContent = describeFleschScore(stats.fleschScore, stats.words > 0);
    resolvedElements.fleschFill.style.width = `${normalizedFlesch}%`;
  }

  root.addEventListener("editor:text-changed", (event) => {
    const detail = (event as CustomEvent<EditorTextChangedDetail>).detail;
    render(detail.text);
  });

  render(mirror.value);
}
