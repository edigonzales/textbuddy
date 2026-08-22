const DEFAULT_OCR_LANGUAGE = "de";

export function mapTextLanguageToOcr(value: string): string {
  const normalized = value.trim().toLowerCase();

  if (normalized === "fr" || normalized === "it") {
    return normalized;
  }
  if (normalized === "en" || normalized.startsWith("en-")) {
    return "en";
  }
  if (normalized === "de" || normalized.startsWith("de-")) {
    return "de";
  }

  return DEFAULT_OCR_LANGUAGE;
}
