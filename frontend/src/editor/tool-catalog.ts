export type TextbuddyToolKey =
  | "correction"
  | "plain-language"
  | "bullet-points"
  | "proofread"
  | "summarize"
  | "formality"
  | "social-media"
  | "medium"
  | "character-speech"
  | "custom"
  | "advisor"
  | "word-synonym"
  | "sentence-rewrite";

export interface TextbuddyToolDefinition {
  key: TextbuddyToolKey;
  mvpVisible: boolean;
}

export const TEXTBUDDY_TOOL_CATALOG: readonly TextbuddyToolDefinition[] = [
  { key: "correction", mvpVisible: true },
  { key: "plain-language", mvpVisible: true },
  { key: "summarize", mvpVisible: true },
  { key: "bullet-points", mvpVisible: false },
  { key: "proofread", mvpVisible: false },
  { key: "formality", mvpVisible: false },
  { key: "social-media", mvpVisible: false },
  { key: "medium", mvpVisible: false },
  { key: "character-speech", mvpVisible: false },
  { key: "custom", mvpVisible: false },
  { key: "advisor", mvpVisible: false },
  { key: "word-synonym", mvpVisible: false },
  { key: "sentence-rewrite", mvpVisible: false },
];

export function isMvpToolVisible(key: TextbuddyToolKey): boolean {
  return TEXTBUDDY_TOOL_CATALOG.some((tool) => tool.key === key && tool.mvpVisible);
}
