type TemplateValue = string | number;

interface UiI18nState {
  locale: string;
  messages: Record<string, string>;
}

const DEFAULT_STATE: UiI18nState = {
  locale: "de",
  messages: {},
};

let state: UiI18nState = DEFAULT_STATE;
let initialized = false;

function resolveRoot(): HTMLElement | null {
  return document.querySelector<HTMLElement>("#editor-island-root");
}

function parseMessages(raw: string): Record<string, string> {
  if (!raw || raw.trim().length === 0) {
    return {};
  }

  try {
    const parsed = JSON.parse(raw) as unknown;

    if (!parsed || typeof parsed !== "object") {
      return {};
    }

    return Object.entries(parsed).reduce<Record<string, string>>((accumulator, [key, value]) => {
      accumulator[key] = typeof value === "string" ? value : String(value);
      return accumulator;
    }, {});
  } catch {
    return {};
  }
}

export function initializeUiI18n(): void {
  const root = resolveRoot();

  if (!root) {
    state = DEFAULT_STATE;
    initialized = true;
    return;
  }

  state = {
    locale: (root.dataset.uiLocale ?? "de").trim() || "de",
    messages: parseMessages(root.dataset.uiMessages ?? ""),
  };
  initialized = true;
}

function ensureInitialized(): void {
  if (!initialized) {
    initializeUiI18n();
  }
}

function interpolate(template: string, variables: Record<string, TemplateValue>): string {
  return Object.entries(variables).reduce((result, [key, value]) => {
    return result.replaceAll(`{${key}}`, String(value));
  }, template);
}

export function t(key: string, variables?: Record<string, TemplateValue>): string {
  ensureInitialized();
  const template = state.messages[key] ?? key;

  if (!variables || Object.keys(variables).length === 0) {
    return template;
  }

  return interpolate(template, variables);
}

export function localeLanguage(): string {
  ensureInitialized();
  return state.locale;
}
