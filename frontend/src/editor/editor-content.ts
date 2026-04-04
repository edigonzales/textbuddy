import type { Editor } from "@tiptap/core";

import { plainTextToHtml } from "./plain-text";

interface SetEditorContentOptions {
  emitUpdate?: boolean;
  addToHistory?: boolean;
}

function applyEditorContent(
  editor: Editor,
  content: string,
  options: SetEditorContentOptions = {},
): void {
  const chain = editor.chain();

  if (options.addToHistory === false) {
    chain.setMeta("addToHistory", false);
  }

  chain.setContent(content, {
    emitUpdate: options.emitUpdate ?? true,
  }).run();
}

function sanitizeImportedHtml(html: string): string {
  const parsedDocument = new DOMParser().parseFromString(html, "text/html");

  parsedDocument
    .querySelectorAll("script,style,iframe,object,embed,link,meta")
    .forEach((element) => element.remove());

  parsedDocument.querySelectorAll<HTMLElement>("*").forEach((element) => {
    Array.from(element.attributes).forEach((attribute) => {
      const name = attribute.name.toLowerCase();
      const value = attribute.value.trim().toLowerCase();

      if (name.startsWith("on") || name === "srcdoc") {
        element.removeAttribute(attribute.name);
        return;
      }

      if ((name === "href" || name === "src") && value.startsWith("javascript:")) {
        element.removeAttribute(attribute.name);
      }
    });
  });

  const sanitized = parsedDocument.body.innerHTML.trim();
  return sanitized || "<p></p>";
}

export function setEditorPlainText(
  editor: Editor,
  text: string,
  options: SetEditorContentOptions = {},
): void {
  applyEditorContent(editor, plainTextToHtml(text), options);
}

export function setEditorHtml(
  editor: Editor,
  html: string,
  options: SetEditorContentOptions = {},
): void {
  applyEditorContent(editor, sanitizeImportedHtml(html), options);
}
