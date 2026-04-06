const DEFAULT_LANGUAGE = "auto";

export function normalizeRequestedLanguage(
  value: string | null | undefined,
): string {
  const normalized = (value ?? "").trim();

  return normalized.length === 0 ? DEFAULT_LANGUAGE : normalized;
}
