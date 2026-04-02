import type { Editor } from "@tiptap/core";

const BLOCK_SEPARATOR = "\n";

function escapeHtml(text: string): string {
  const container = document.createElement("div");
  container.textContent = text;
  return container.innerHTML;
}

export function plainTextToHtml(text: string): string {
  if (!text) {
    return "<p></p>";
  }

  return text
    .replace(/\r\n/g, BLOCK_SEPARATOR)
    .split(BLOCK_SEPARATOR)
    .map((line) => (line ? `<p>${escapeHtml(line)}</p>` : "<p></p>"))
    .join("");
}

export function getPlainText(editor: Editor): string {
  return editor.getText({ blockSeparator: BLOCK_SEPARATOR });
}

export function countWords(text: string): number {
  const normalized = text.trim();

  if (!normalized) {
    return 0;
  }

  return normalized.split(/\s+/u).length;
}
