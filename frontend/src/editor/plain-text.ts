import type { Editor, JSONContent } from "@tiptap/core";

export function plainTextDocument(text: string): JSONContent {
  return {
    type: "doc",
    content: text.split(/\r\n?|\n/).map((line) => ({
      type: "paragraph",
      content: line ? [{ type: "text", text: line }] : [],
    })),
  };
}

export function getPlainText(editor: Editor): string {
  const doc = editor.state.doc;
  return doc.textBetween(0, doc.content.size, "\n", (leaf) =>
    leaf.type.name === "hardBreak" ? "\n" : "",
  );
}

export function countWords(text: string): number {
  const normalized = text.trim();
  return normalized ? normalized.split(/\s+/u).length : 0;
}
