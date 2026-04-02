import type { Editor } from "@tiptap/core";

import { plainTextToHtml } from "./plain-text";

interface SetEditorPlainTextOptions {
  emitUpdate?: boolean;
  addToHistory?: boolean;
}

export function setEditorPlainText(
  editor: Editor,
  text: string,
  options: SetEditorPlainTextOptions = {},
): void {
  const chain = editor.chain();

  if (options.addToHistory === false) {
    chain.setMeta("addToHistory", false);
  }

  chain.setContent(plainTextToHtml(text), {
    emitUpdate: options.emitUpdate ?? true,
  }).run();
}
